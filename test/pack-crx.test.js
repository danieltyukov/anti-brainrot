'use strict';
const test = require('node:test');
const assert = require('node:assert/strict');
const { generateKeyPairSync, verify, createPublicKey } = require('node:crypto');
const { readFileSync } = require('node:fs');
const path = require('node:path');

const manifest = JSON.parse(readFileSync(path.join(__dirname, '..', 'extension', 'manifest.json'), 'utf8'));

async function load() {
  return import('../scripts/pack-crx.mjs');
}

test('varint and field encode the protobuf wire format', async () => {
  const { varint, field } = await load();
  assert.deepEqual([...varint(0)], [0]);
  assert.deepEqual([...varint(127)], [127]);
  assert.deepEqual([...varint(128)], [0x80, 0x01]);
  assert.deepEqual([...varint(300)], [0xac, 0x02]);
  assert.deepEqual([...field(1, Buffer.from('ab'))], [0x0a, 2, 0x61, 0x62]);
  // field 10000, wire type 2: tag 80002 = 0x82 0xf1 0x04
  assert.deepEqual([...field(10000, Buffer.alloc(0))], [0x82, 0xf1, 0x04, 0]);
});

test('the manifest key yields the pinned id', async () => {
  const { extensionIdOf } = await load();
  assert.equal(extensionIdOf(Buffer.from(manifest.key, 'base64')), 'ibcicobbbpfmonjbhpmllnjgdkedneop');
});

test('packCrx writes a CRX3 whose signature verifies and whose id comes from the key', async () => {
  const { packCrx, readCrx, extensionIdOf } = await load();
  const { privateKey } = generateKeyPairSync('rsa', { modulusLength: 2048 });
  const pem = privateKey.export({ type: 'pkcs8', format: 'pem' });
  const zip = Buffer.from('PK\x03\x04 not really a zip but bytes are bytes');
  const { crx, id } = packCrx(zip, pem);

  assert.equal(crx.subarray(0, 4).toString('latin1'), 'Cr24');
  assert.equal(crx.readUInt32LE(4), 3);
  const parts = readCrx(crx);
  assert.deepEqual(parts.zip, zip, 'the zip is appended untouched');
  assert.equal(parts.crxId.length, 16);
  assert.equal(extensionIdOf(parts.publicKey), id);
  assert.match(id, /^[a-p]{32}$/);
  const publicKey = createPublicKey(privateKey).export({ type: 'spki', format: 'der' });
  assert.deepEqual(parts.publicKey, publicKey);

  const lengthLe = Buffer.alloc(4);
  lengthLe.writeUInt32LE(parts.signedHeaderData.length);
  const signed = Buffer.concat([Buffer.from('CRX3 SignedData\0', 'latin1'), lengthLe, parts.signedHeaderData, zip]);
  assert.equal(verify('sha256', signed, createPublicKey(privateKey), parts.signature), true);
  const tampered = Buffer.concat([Buffer.from('CRX3 SignedData\0', 'latin1'), lengthLe, parts.signedHeaderData, Buffer.from('x')]);
  assert.equal(verify('sha256', tampered, createPublicKey(privateKey), parts.signature), false);
});

test('readCrx rejects other files', async () => {
  const { readCrx } = await load();
  assert.throws(() => readCrx(Buffer.from('PK\x03\x04')), /not a CRX/);
});
