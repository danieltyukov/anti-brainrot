'use strict';
const test = require('node:test');
const assert = require('node:assert/strict');

require('../extension/lib/countdown.js');
const { countdown } = globalThis.AntiBrainrot;

test('start creates a counting state with the full time remaining', () => {
  const s = countdown.start(5000, 1000);
  assert.deepEqual(s, { status: 'counting', totalMs: 5000, startedAt: 1000, remainingMs: 5000 });
});

test('tick reduces remaining time and finishes at zero', () => {
  const s = countdown.start(5000, 1000);
  const mid = countdown.tick(s, 3500);
  assert.equal(mid.status, 'counting');
  assert.equal(mid.remainingMs, 2500);
  assert.equal(s.remainingMs, 5000, 'tick does not mutate');
  const done = countdown.tick(s, 6000);
  assert.equal(done.status, 'done');
  assert.equal(done.remainingMs, 0);
  const late = countdown.tick(s, 9000);
  assert.equal(late.remainingMs, 0);
});

test('tick on a finished or idle state is a no-op', () => {
  const idle = { status: 'idle' };
  assert.deepEqual(countdown.tick(idle, 100), idle);
});

test('zero length countdown is done immediately', () => {
  assert.equal(countdown.tick(countdown.start(0, 10), 10).status, 'done');
});

test('format renders m:ss and h:mm:ss', () => {
  assert.equal(countdown.format(299000), '4:59');
  assert.equal(countdown.format(7000), '0:07');
  assert.equal(countdown.format(0), '0:00');
  assert.equal(countdown.format(60000), '1:00');
  assert.equal(countdown.format(3600000), '1:00:00');
  assert.equal(countdown.format(3661000), '1:01:01');
  assert.equal(countdown.format(999), '0:01', 'rounds up so the display never shows 0:00 while counting');
});

test('delayLabel names every delay choice', () => {
  assert.equal(countdown.delayLabel(0), 'Instant');
  assert.equal(countdown.delayLabel(30), '30 seconds');
  assert.equal(countdown.delayLabel(60), '1 minute');
  assert.equal(countdown.delayLabel(300), '5 minutes');
  assert.equal(countdown.delayLabel(600), '10 minutes');
  assert.equal(countdown.delayLabel(1800), '30 minutes');
  assert.equal(countdown.delayLabel(3600), '1 hour');
  assert.equal(countdown.delayLabel(7200), '2 hours');
  assert.equal(countdown.delayLabel(90), '90 seconds');
});
