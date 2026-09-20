package io.github.danieltyukov.antibrainrot.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import io.github.danieltyukov.antibrainrot.App
import io.github.danieltyukov.antibrainrot.MainActivity
import io.github.danieltyukov.antibrainrot.R
import io.github.danieltyukov.antibrainrot.core.DomainPolicy
import io.github.danieltyukov.antibrainrot.core.Keys
import io.github.danieltyukov.antibrainrot.core.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

// A DNS-only VPN. Only the two fake DNS addresses are routed into the tunnel,
// so every other packet takes its normal path. Queries for blocked names get
// NXDOMAIN; the rest are forwarded to the network's own resolver. No traffic
// leaves the device through this app.
class DnsVpnService : VpnService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var tun: ParcelFileDescriptor? = null
    private var worker: Thread? = null
    private val stopping = AtomicBoolean(false)
    @Volatile private var policy: DomainPolicy = DomainPolicy(emptySet(), false, emptyList(), emptyList())
    private val forwardPool = Executors.newCachedThreadPool()
    private val writeLock = Any()
    private var adultList: Set<String> = emptySet()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopFilter()
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                startForeground(NOTIFICATION_ID, buildNotification())
                if (adultList.isEmpty()) adultList = loadAdultList()
                scope.launch { App.instance.settings.flow.collect { applySettings(it) } }
                if (worker == null) startFilter()
                return START_STICKY
            }
        }
    }

    override fun onDestroy() {
        stopFilter()
        scope.cancel()
        forwardPool.shutdownNow()
        super.onDestroy()
    }

    override fun onRevoke() {
        stopFilter()
        stopSelf()
        super.onRevoke()
    }

    private fun applySettings(s: Settings) {
        // Blocked sites are also answered NXDOMAIN; timed sites need to load
        // during a session, so only the address bar watches them.
        policy = DomainPolicy(adultList, s.sites.adult, Keys.blockedHosts(s), s.sites.allowed)
        if (!Enforcer.siteFilterWanted(s)) {
            stopFilter()
            stopSelf()
        }
    }

    private fun loadAdultList(): Set<String> = try {
        assets.open("adult-domains.txt").bufferedReader().useLines { lines -> lines.map { it.trim() }.filter { it.isNotEmpty() }.toHashSet() }
    } catch (e: Exception) {
        Log.w(TAG, "adult list missing: ${e.message}")
        emptySet()
    }

    private fun startFilter() {
        val builder = Builder()
            .setSession(getString(R.string.app_name))
            .addAddress(TUN_ADDRESS4, 24)
            .addDnsServer(DNS_ADDRESS4)
            .addRoute(DNS_ADDRESS4, 32)
            .setBlocking(true)
            .setMtu(1500)
        try {
            builder.addAddress(TUN_ADDRESS6, 64).addDnsServer(DNS_ADDRESS6).addRoute(DNS_ADDRESS6, 128)
        } catch (e: Exception) {
            Log.w(TAG, "IPv6 not available: ${e.message}")
        }
        try {
            builder.addDisallowedApplication(packageName)
        } catch (e: Exception) {
            Log.w(TAG, "could not exclude own package: ${e.message}")
        }
        if (Build.VERSION.SDK_INT >= 29) builder.setMetered(false)
        val pfd = builder.establish() ?: run {
            Log.w(TAG, "establish() returned null")
            stopSelf()
            return
        }
        tun = pfd
        stopping.set(false)
        running = true
        worker = Thread({ loop(pfd) }, "abr-dns").also { it.start() }
    }

    private fun stopFilter() {
        stopping.set(true)
        running = false
        try {
            tun?.close()
        } catch (e: Exception) {
            // already closed
        }
        tun = null
        worker = null
    }

    private fun upstreamResolvers(): List<InetAddress> {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val fallback = listOf(InetAddress.getByName("1.1.1.1"), InetAddress.getByName("9.9.9.9"))
        return try {
            val network = cm.activeNetwork ?: return fallback
            val props = cm.getLinkProperties(network) ?: return fallback
            val servers = props.dnsServers.filter { !it.isLoopbackAddress && it.hostAddress != DNS_ADDRESS4 && it.hostAddress != DNS_ADDRESS6 }
            if (servers.isEmpty()) fallback else servers
        } catch (e: Exception) {
            fallback
        }
    }

    private fun loop(pfd: ParcelFileDescriptor) {
        val input = FileInputStream(pfd.fileDescriptor)
        val output = FileOutputStream(pfd.fileDescriptor)
        val buffer = ByteArray(32767)
        while (!stopping.get()) {
            val length = try {
                input.read(buffer)
            } catch (e: Exception) {
                break
            }
            if (length <= 0) continue
            val packet = buffer.copyOf(length)
            handlePacket(packet, output)
        }
    }

    private fun handlePacket(packet: ByteArray, output: FileOutputStream) {
        val parsed = Dns.parse(packet) ?: return
        val name = parsed.question ?: return
        if (policy.isBlocked(name)) {
            val reply = Dns.buildReply(parsed, Dns.nxdomain(parsed.payload))
            synchronized(writeLock) { output.write(reply) }
            return
        }
        forwardPool.execute {
            try {
                val socket = DatagramSocket()
                protect(socket)
                socket.soTimeout = 5000
                val resolvers = upstreamResolvers()
                var answer: ByteArray? = null
                for (resolver in resolvers) {
                    try {
                        socket.send(DatagramPacket(parsed.payload, parsed.payload.size, InetSocketAddress(resolver, 53)))
                        val response = DatagramPacket(ByteArray(4096), 4096)
                        socket.receive(response)
                        answer = response.data.copyOf(response.length)
                        break
                    } catch (e: Exception) {
                        // try the next resolver
                    }
                }
                socket.close()
                val data = answer ?: return@execute
                val reply = Dns.buildReply(parsed, data)
                synchronized(writeLock) { output.write(reply) }
            } catch (e: Exception) {
                Log.w(TAG, "forward failed: ${e.message}")
            }
        }
    }

    private fun buildNotification(): Notification {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, getString(R.string.vpn_channel), NotificationManager.IMPORTANCE_MIN))
        }
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val builder = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, CHANNEL_ID) else @Suppress("DEPRECATION") Notification.Builder(this)
        return builder
            .setContentTitle(getString(R.string.vpn_running))
            .setContentText(getString(R.string.vpn_running_text))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(open)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val ACTION_START = "io.github.danieltyukov.antibrainrot.START"
        const val ACTION_STOP = "io.github.danieltyukov.antibrainrot.STOP"
        const val CHANNEL_ID = "site-filter"
        const val NOTIFICATION_ID = 1
        const val TUN_ADDRESS4 = "10.111.222.1"
        const val DNS_ADDRESS4 = "10.111.222.2"
        const val TUN_ADDRESS6 = "fd00:abbe:abbe::1"
        const val DNS_ADDRESS6 = "fd00:abbe:abbe::2"
        private const val TAG = "abr-dns"
        @Volatile var running = false
    }
}

// Minimal IP/UDP/DNS packet handling for the filter: enough to read a query
// name, forward the DNS payload, and write a well formed reply back.
object Dns {
    class Parsed(
        val ipVersion: Int,
        val header: ByteArray, // IP header bytes
        val srcAddr: ByteArray,
        val dstAddr: ByteArray,
        val srcPort: Int,
        val dstPort: Int,
        val payload: ByteArray,
        val question: String?,
    )

    fun parse(packet: ByteArray): Parsed? {
        if (packet.isEmpty()) return null
        val version = (packet[0].toInt() shr 4) and 0xF
        return when (version) {
            4 -> parse4(packet)
            6 -> parse6(packet)
            else -> null
        }
    }

    private fun parse4(p: ByteArray): Parsed? {
        if (p.size < 28) return null
        val ihl = (p[0].toInt() and 0xF) * 4
        if (p[9].toInt() and 0xFF != 17) return null // UDP only
        val udp = ihl
        val srcPort = ((p[udp].toInt() and 0xFF) shl 8) or (p[udp + 1].toInt() and 0xFF)
        val dstPort = ((p[udp + 2].toInt() and 0xFF) shl 8) or (p[udp + 3].toInt() and 0xFF)
        if (dstPort != 53) return null
        val payload = p.copyOfRange(udp + 8, p.size)
        return Parsed(4, p.copyOfRange(0, ihl), p.copyOfRange(12, 16), p.copyOfRange(16, 20), srcPort, dstPort, payload, questionName(payload))
    }

    private fun parse6(p: ByteArray): Parsed? {
        if (p.size < 48) return null
        if (p[6].toInt() and 0xFF != 17) return null
        val udp = 40
        val srcPort = ((p[udp].toInt() and 0xFF) shl 8) or (p[udp + 1].toInt() and 0xFF)
        val dstPort = ((p[udp + 2].toInt() and 0xFF) shl 8) or (p[udp + 3].toInt() and 0xFF)
        if (dstPort != 53) return null
        val payload = p.copyOfRange(udp + 8, p.size)
        return Parsed(6, p.copyOfRange(0, 40), p.copyOfRange(8, 24), p.copyOfRange(24, 40), srcPort, dstPort, payload, questionName(payload))
    }

    fun questionName(dns: ByteArray): String? {
        if (dns.size < 13) return null
        val qdcount = ((dns[4].toInt() and 0xFF) shl 8) or (dns[5].toInt() and 0xFF)
        if (qdcount < 1) return null
        val sb = StringBuilder()
        var i = 12
        while (i < dns.size) {
            val len = dns[i].toInt() and 0xFF
            if (len == 0) break
            if (len and 0xC0 == 0xC0) return null
            if (i + 1 + len > dns.size) return null
            if (sb.isNotEmpty()) sb.append('.')
            sb.append(String(dns, i + 1, len, Charsets.US_ASCII))
            i += 1 + len
        }
        return if (sb.isEmpty()) null else sb.toString().lowercase()
    }

    // Copies the query and flips it into an NXDOMAIN response.
    fun nxdomain(query: ByteArray): ByteArray {
        val r = query.copyOf()
        r[2] = (0x81).toByte() // QR=1, RD=1
        r[3] = (0x83).toByte() // RA=1, RCODE=3 NXDOMAIN
        r[6] = 0; r[7] = 0 // ANCOUNT
        r[8] = 0; r[9] = 0 // NSCOUNT
        r[10] = 0; r[11] = 0 // ARCOUNT
        return r
    }

    fun buildReply(q: Parsed, dnsPayload: ByteArray): ByteArray {
        val udpLength = 8 + dnsPayload.size
        return if (q.ipVersion == 4) {
            val total = 20 + udpLength
            val out = ByteBuffer.allocate(total)
            out.put((0x45).toByte()).put(0.toByte()).putShort(total.toShort())
            out.putShort(0).putShort(0x4000.toShort()) // id, DF
            out.put(64.toByte()).put(17.toByte()).putShort(0) // ttl, udp, checksum placeholder
            out.put(q.dstAddr).put(q.srcAddr)
            out.putShort(q.dstPort.toShort()).putShort(q.srcPort.toShort()).putShort(udpLength.toShort()).putShort(0)
            out.put(dnsPayload)
            val bytes = out.array()
            val checksum = checksum(bytes, 0, 20, 0)
            bytes[10] = (checksum shr 8).toByte(); bytes[11] = checksum.toByte()
            bytes
        } else {
            val out = ByteBuffer.allocate(40 + udpLength)
            out.putInt(0x60000000).putShort(udpLength.toShort()).put(17.toByte()).put(64.toByte())
            out.put(q.dstAddr).put(q.srcAddr)
            out.putShort(q.dstPort.toShort()).putShort(q.srcPort.toShort()).putShort(udpLength.toShort()).putShort(0)
            out.put(dnsPayload)
            val bytes = out.array()
            // UDP checksum is mandatory on IPv6: pseudo header + udp segment
            val pseudo = ByteBuffer.allocate(40)
            pseudo.put(q.dstAddr).put(q.srcAddr).putInt(udpLength).putInt(17)
            var sum = partialSum(pseudo.array(), 0, 40, 0)
            sum = partialSum(bytes, 40, udpLength, sum)
            var cs = fold(sum)
            if (cs == 0) cs = 0xFFFF
            bytes[46] = (cs shr 8).toByte(); bytes[47] = cs.toByte()
            bytes
        }
    }

    private fun partialSum(b: ByteArray, offset: Int, length: Int, start: Long): Long {
        var sum = start
        var i = offset
        val end = offset + length
        while (i + 1 < end) {
            sum += (((b[i].toInt() and 0xFF) shl 8) or (b[i + 1].toInt() and 0xFF)).toLong()
            i += 2
        }
        if (i < end) sum += ((b[i].toInt() and 0xFF) shl 8).toLong()
        return sum
    }

    private fun fold(sumIn: Long): Int {
        var sum = sumIn
        while (sum shr 16 != 0L) sum = (sum and 0xFFFF) + (sum shr 16)
        return (sum.inv() and 0xFFFF).toInt()
    }

    fun checksum(b: ByteArray, offset: Int, length: Int, start: Long): Int = fold(partialSum(b, offset, length, start))
}
