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
// answer exactly like that version - only the imports, the
// createDatabase()/loadSnapshot() init, the transactionOpen tracking and the
// schedulePersist() calls are new. If `sqldelight` is ever bumped, re-diff
// this file against the new version's sqljs.worker.js and re-apply these
// changes on top - the upstream protocol could change without notice.
//
// Taking a snapshot is not free of side effects: sql.js' Database.export()
// closes and re-opens the database. That rolls back a transaction that is
// still open and resets connection pragmas like foreign_keys to their
// defaults. So no snapshot is taken while a transaction is open (the end of
// the transaction schedules it instead), and foreign_keys is restored after
// every export.

importScripts('sql-wasm.js');

const DB_NAME = 'spectacled-sqlite';
const STORE_NAME = 'snapshots';
const SNAPSHOT_KEY = 'database';
const PERSIST_DEBOUNCE_MS = 300;

let db = null;
let persistTimer = null;
// Transactions arrive either as begin_/end_/rollback_transaction actions or - from
// TransactionSerializingSqlDriver - as plain BEGIN/END/ROLLBACK statements via exec.
let transactionOpen = false;

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
    if (transactionOpen) {
      return;  // the end of the transaction schedules the snapshot again
    }
    saveSnapshot(exportDatabase());
  }, PERSIST_DEBOUNCE_MS);
}

function exportDatabase() {
  const foreignKeys = db.exec('PRAGMA foreign_keys;')[0].values[0][0];
  const bytes = db.export();
  db.exec(`PRAGMA foreign_keys=${foreignKeys ? 'ON' : 'OFF'};`);
  return bytes;
}

// Runs a statement that begins or ends a transaction and keeps transactionOpen in step with it.
// Not updated when the statement throws (e.g. a failed COMMIT leaves the transaction open).
function execTransactionStatement(sql, opensTransaction) {
  const results = db.exec(sql);
  transactionOpen = opensTransaction;
  if (!opensTransaction) {
    schedulePersist();
  }
  return results;
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
      if (/^\s*BEGIN\b/i.test(data.sql)) {
        return postMessage({
          id: data.id,
          results: execTransactionStatement(data.sql, true)[0] ?? { values: [] }
        });
      }
      if (/^\s*(END|COMMIT|ROLLBACK)\b/i.test(data.sql)) {
        return postMessage({
          id: data.id,
          results: execTransactionStatement(data.sql, false)[0] ?? { values: [] }
        });
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
        results: execTransactionStatement("BEGIN TRANSACTION;", true)
      })
    case "end_transaction":
      return postMessage({
        id: data.id,
        results: execTransactionStatement("END TRANSACTION;", false)
      })
    case "rollback_transaction":
      return postMessage({
        id: data.id,
        results: execTransactionStatement("ROLLBACK TRANSACTION;", false)
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
