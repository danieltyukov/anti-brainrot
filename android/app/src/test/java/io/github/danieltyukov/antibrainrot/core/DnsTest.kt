package io.github.danieltyukov.antibrainrot.core

import io.github.danieltyukov.antibrainrot.service.Dns
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.nio.ByteBuffer

class DnsTest {
    // A minimal query: id 0x1234, RD set, one question.
    private fun query(name: String, type: Int): ByteArray {
        val labels = name.split('.')
        val out = ByteBuffer.allocate(12 + labels.sumOf { it.length + 1 } + 5)
        out.putShort(0x1234).putShort(0x0100).putShort(1).putShort(0).putShort(0).putShort(0)
        for (label in labels) out.put(label.length.toByte()).put(label.toByteArray(Charsets.US_ASCII))
        out.put(0).putShort(type.toShort()).putShort(1)
        return out.array()
    }

    @Test fun parsesTheQuestion() {
        val q = Dns.question(query("www.google.com", 1))!!
        assertEquals("www.google.com", q.name)
        assertEquals(1, q.type)
        assertEquals(12 + 16 + 4, q.end)
        assertEquals(28, Dns.question(query("x.y", 28))!!.type)
        assertNull(Dns.question(ByteArray(5)))
    }

    @Test fun buildsAnAnswerWithTheGivenAddresses() {
        val payload = query("www.google.com", 1)
        val q = Dns.question(payload)!!
        val ip = byteArrayOf(216.toByte(), 239.toByte(), 38, 120)
        val a = Dns.answer(payload, q, listOf(ip), ttl = 300)
        assertEquals(q.end + 16, a.size)
        assertEquals(0x12, a[0].toInt()); assertEquals(0x34, a[1].toInt())
        assertEquals(0x81, a[2].toInt() and 0xFF) // QR, RD
        assertEquals(0x80, a[3].toInt() and 0xFF) // RA, NOERROR
        assertEquals(1, a[5].toInt()) // QDCOUNT
        assertEquals(1, a[7].toInt()) // ANCOUNT
        assertEquals(0, a[9].toInt()); assertEquals(0, a[11].toInt())
        val rr = q.end
        assertEquals(0xC0, a[rr].toInt() and 0xFF); assertEquals(0x0C, a[rr + 1].toInt())
        assertEquals(1, a[rr + 3].toInt()) // type A
        assertEquals(1, a[rr + 5].toInt()) // class IN
        assertEquals(300, ByteBuffer.wrap(a, rr + 6, 4).int)
        assertEquals(4, a[rr + 11].toInt())
        assertEquals(ip.toList(), a.copyOfRange(rr + 12, rr + 16).toList())
        // No addresses: a NODATA answer, same header, nothing after the question.
        val none = Dns.answer(payload, q, emptyList(), ttl = 300)
        assertEquals(q.end, none.size)
        assertEquals(0, none[7].toInt())
    }
}
