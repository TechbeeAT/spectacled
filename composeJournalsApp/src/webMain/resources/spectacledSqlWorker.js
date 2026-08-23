// Drop-in replacement for @cashapp/sqldelight-sqljs-worker's sqljs.worker.js.
//
// The stock worker keeps the sql.js database purely in memory, so the whole
// database is lost on every page refresh (DAT-6). This version adds the one
// thing it's missing: on startup it restores the last snapshot from
// IndexedDB, and after every write it exports the database and saves a new
// snapshot back to IndexedDB (debounced so a burst of writes only triggers
// one export).
//
// The message protocol (exec / begin_transaction / end_transaction /
// rollback_transaction, id / results / error fields) matches
// app.cash.sqldelight's WebWorkerDriver exactly, so the Kotlin side needs no
// changes. Loaded as a classic (non-module) worker via importScripts so it
// doesn't depend on webpack's npm-package bundling.
//
// Forked from sqljs.worker.js as shipped in @cashapp/sqldelight-sqljs-worker
// version 2.3.2 (matches the `sqldelight` version pinned in
// gradle/libs.versions.toml), source:
// https://github.com/cashapp/sqldelight/blob/2.3.2/drivers/web-worker-driver/sqljs/sqljs.worker.js
// The exec/begin_transaction/end_transaction/rollback_transaction cases below
// are byte-for-byte identical to that version - only the imports, the
// createDatabase()/loadSnapshot() init, and the schedulePersist() calls are
// new. If `sqldelight` is ever bumped, re-diff this file against the new
// version's sqljs.worker.js and re-apply these changes on top - the upstream
// protocol could change without notice.

importScripts('sql-wasm.js');

const DB_NAME = 'spectacled-sqlite';
const STORE_NAME = 'snapshots';
const SNAPSHOT_KEY = 'database';
const PERSIST_DEBOUNCE_MS = 300;

let db = null;
let persistTimer = null;

function openMetaDb() {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(DB_NAME, 1);
    request.onupgradeneeded = () => {
      request.result.createObjectStore(STORE_NAME);
    };
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
}

async function loadSnapshot() {
  try {
    const metaDb = await openMetaDb();
    return await new Promise((resolve, reject) => {
      const tx = metaDb.transaction(STORE_NAME, 'readonly');
      const req = tx.objectStore(STORE_NAME).get(SNAPSHOT_KEY);
      req.onsuccess = () => resolve(req.result ?? null);
      req.onerror = () => reject(req.error);
    });
  } catch (e) {
    console.error('[spectacledSqlWorker] Failed to load snapshot, starting with a fresh database', e);
    return null;
  }
}

async function saveSnapshot(bytes) {
  try {
    const metaDb = await openMetaDb();
    await new Promise((resolve, reject) => {
      const tx = metaDb.transaction(STORE_NAME, 'readwrite');
      tx.objectStore(STORE_NAME).put(bytes, SNAPSHOT_KEY);
      tx.oncomplete = () => resolve();
      tx.onerror = () => reject(tx.error);
    });
  } catch (e) {
    console.error('[spectacledSqlWorker] Failed to persist database snapshot', e);
  }
}

function schedulePersist() {
  if (persistTimer !== null) {
    clearTimeout(persistTimer);
  }
  persistTimer = setTimeout(() => {
    persistTimer = null;
    saveSnapshot(db.export());
  }, PERSIST_DEBOUNCE_MS);
}

async function createDatabase() {
  const SQL = await initSqlJs({ locateFile: () => 'sql-wasm.wasm' });
  const snapshot = await loadSnapshot();
  db = snapshot ? new SQL.Database(snapshot) : new SQL.Database();
}

function onModuleReady() {
  const data = this.data;

  switch (data && data.action) {
    case "exec":
      if (!data["sql"]) {
        throw new Error("exec: Missing query string");
      }
      if (!/^\s*SELECT/i.test(data.sql)) {
        schedulePersist();
      }
      return postMessage({
        id: data.id,
        results: db.exec(data.sql, data.params)[0] ?? { values: [] }
      });
    case "begin_transaction":
      return postMessage({
        id: data.id,
        results: db.exec("BEGIN TRANSACTION;")
      })
    case "end_transaction":
      schedulePersist();
      return postMessage({
        id: data.id,
        results: db.exec("END TRANSACTION;")
      })
    case "rollback_transaction":
      return postMessage({
        id: data.id,
        results: db.exec("ROLLBACK TRANSACTION;")
      })
    default:
      throw new Error(`Unsupported action: ${data && data.action}`);
  }
}

function onError(err) {
  return postMessage({
    id: this.data.id,
    error: err
  });
}

if (typeof importScripts === "function") {
  db = null;
  const sqlModuleReady = createDatabase()
  self.onmessage = (event) => {
    return sqlModuleReady
      .then(onModuleReady.bind(event))
      .catch(onError.bind(event));
  }
}
