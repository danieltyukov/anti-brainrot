'use strict';

// Minimal in-memory stand-in for chrome.storage.sync and chrome.storage.onChanged.
function installChromeMock() {
  const store = {};
  const listeners = [];
  globalThis.chrome = {
    storage: {
      sync: {
        async get(key) {
          if (key in store) return { [key]: structuredClone(store[key]) };
          return {};
        },
        async set(obj) {
          const changes = {};
          for (const [k, v] of Object.entries(obj)) {
            changes[k] = { oldValue: store[k], newValue: structuredClone(v) };
            store[k] = structuredClone(v);
          }
          for (const l of listeners) l(changes, 'sync');
        },
      },
      onChanged: {
        addListener(l) {
          listeners.push(l);
        },
      },
    },
  };
  return { store, listeners };
}

function removeChromeMock() {
  delete globalThis.chrome;
}

module.exports = { installChromeMock, removeChromeMock };
