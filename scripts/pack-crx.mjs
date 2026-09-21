// Packs a zip of the extension into a CRX3 file signed with the pinned key.
//
//   node scripts/pack-crx.mjs <in.zip> <out.crx> [key.pem]
//
// The key defaults to $ABR_CRX_KEY, then ~/.config/anti-brainrot/key.pem.
// No dependencies: the CRX3 header is a small protobuf message written by
// hand, and Node's crypto does the RSA signature.
//
// CRX3 layout: "Cr24", uint32 version (3), uint32 header length, header,
// zip. The header is CrxFileHeader { repeated AsymmetricKeyProof
// sha256_with_rsa = 2 { bytes public_key = 1; bytes signature = 2 };
// bytes signed_header_data = 10000 } where signed_header_data is
// SignedData { bytes crx_id = 1 }, the first 16 bytes of the SHA-256 of the
// public key. The signature covers "CRX3 SignedData\0", the little endian
// length of signed_header_data, signed_header_data, and the zip.
import { readFileSync, writeFileSync } from 'node:fs';
import { createPrivateKey, createPublicKey, createHash, sign } from 'node:crypto';
import { homedir } from 'node:os';
import { join } from 'node:path';

export function varint(n) {
  const out = [];
  while (n >= 0x80) {
    out.push((n & 0x7f) | 0x80);
    n = Math.floor(n / 128);
  }
  out.push(n);
  return Buffer.from(out);
}

export function field(number, bytes) {
  return Buffer.concat([varint((number << 3) | 2), varint(bytes.length), bytes]);
}

export function extensionIdOf(publicKeyDer) {
  return createHash('sha256')
    .update(publicKeyDer)
    .digest('hex')
    .slice(0, 32)
    .replace(/[0-9a-f]/g, (c) => String.fromCharCode('a'.charCodeAt(0) + parseInt(c, 16)));
}

export function packCrx(zip, pem) {
  const privateKey = createPrivateKey(pem);
  const publicKey = createPublicKey(privateKey).export({ type: 'spki', format: 'der' });
  const crxId = createHash('sha256').update(publicKey).digest().subarray(0, 16);
  const signedHeaderData = field(1, crxId);
  const lengthLe = Buffer.alloc(4);
  lengthLe.writeUInt32LE(signedHeaderData.length);
  const signature = sign('sha256', Buffer.concat([Buffer.from('CRX3 SignedData\0', 'latin1'), lengthLe, signedHeaderData, zip]), privateKey);
  const proof = field(2, Buffer.concat([field(1, publicKey), field(2, signature)]));
  const header = Buffer.concat([proof, field(10000, signedHeaderData)]);
  const prefix = Buffer.alloc(12);
  prefix.write('Cr24', 0, 'latin1');
  prefix.writeUInt32LE(3, 4);
  prefix.writeUInt32LE(header.length, 8);
  return { crx: Buffer.concat([prefix, header, zip]), id: extensionIdOf(publicKey) };
}

// Reads the pieces back out of a CRX3 file. Used by the tests and by the
// build to confirm the id before the file goes out.
export function readCrx(crx) {
  if (crx.subarray(0, 4).toString('latin1') !== 'Cr24') throw new Error('not a CRX file');
  if (crx.readUInt32LE(4) !== 3) throw new Error('not CRX3');
  const headerLength = crx.readUInt32LE(8);
  const header = crx.subarray(12, 12 + headerLength);
  const zip = crx.subarray(12 + headerLength);
  const fields = parseFields(header);
  const proof = parseFields(fields.get(2)[0]);
  const signedHeaderData = fields.get(10000)[0];
  const crxId = parseFields(signedHeaderData).get(1)[0];
  return { publicKey: proof.get(1)[0], signature: proof.get(2)[0], signedHeaderData, crxId, zip };
}

function readVarint(buf, at) {
  let n = 0;
  let shift = 0;
  let i = at;
  for (;;) {
    const b = buf[i++];
    n += (b & 0x7f) * 2 ** shift;
    if (b < 0x80) break;
    shift += 7;
  }
  return [n, i];
}

function parseFields(buf) {
  const out = new Map();
  let i = 0;
  while (i < buf.length) {
    let tag;
    [tag, i] = readVarint(buf, i);
    if ((tag & 7) !== 2) throw new Error('unexpected wire type');
    let len;
    [len, i] = readVarint(buf, i);
    const number = Math.floor(tag / 8);
    if (!out.has(number)) out.set(number, []);
    out.get(number).push(buf.subarray(i, i + len));
    i += len;
  }
  return out;
}

export function defaultKeyPath() {
  return process.env.ABR_CRX_KEY || join(homedir(), '.config', 'anti-brainrot', 'key.pem');
}

if (import.meta.url === `file://${process.argv[1]}`) {
  const [input, output, keyArg] = process.argv.slice(2);
  if (!input || !output) {
    console.error('usage: node scripts/pack-crx.mjs <in.zip> <out.crx> [key.pem]');
    process.exit(2);
  }
  const keyPath = keyArg || defaultKeyPath();
  const { crx, id } = packCrx(readFileSync(input), readFileSync(keyPath, 'utf8'));
  writeFileSync(output, crx);
  console.log(`wrote ${output} (id ${id})`);
}
