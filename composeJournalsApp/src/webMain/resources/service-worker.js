// Spectacled offline service worker.
//
// Strategy: network-first for same-origin GET requests to static app-shell assets, falling back
// to the cache only when the network fails (i.e. offline). Network-first is deliberate:
//   - It never serves a stale bundle or a stale sql.js worker while online, so app updates and
//     the DAT-6 IndexedDB persistence mechanism always load the freshest files. The cache is
//     purely an offline fallback.
//   - Dynamic requests are never cached: non-GET methods (CalDAV PROPFIND/REPORT/PUT/POST), any
//     cross-origin request (the CalDAV server / proxy), and same-origin requests that aren't a
//     navigation or a known static asset extension all pass straight through, untouched.
//
// Bump CACHE_VERSION whenever this file changes to drop the previous cache on activation.

const CACHE_VERSION = 'spectacled-shell-v1';

// Only these same-origin responses are treated as cacheable app-shell assets.
const SHELL_ASSET = /\.(?:js|mjs|css|wasm|html|ico|png|svg|webmanifest|json|woff2?)$/i;

self.addEventListener('install', () => {
  // Take over as soon as installed instead of waiting for existing tabs to close.
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  event.waitUntil((async () => {
    const keys = await caches.keys();
    await Promise.all(keys.filter((k) => k !== CACHE_VERSION).map((k) => caches.delete(k)));
    await self.clients.claim();
  })());
});

self.addEventListener('fetch', (event) => {
  const request = event.request;
  const url = new URL(request.url);

  // Leave everything that isn't a same-origin GET for a static shell asset (or a navigation) to
  // the browser's default handling — crucially, all CalDAV/proxy traffic.
  if (request.method !== 'GET' || url.origin !== self.location.origin) return;
  const isNavigation = request.mode === 'navigate';
  if (!isNavigation && !SHELL_ASSET.test(url.pathname)) return;

  event.respondWith((async () => {
    const cache = await caches.open(CACHE_VERSION);
    try {
      const response = await fetch(request);
      // Cache a copy of successful same-origin responses for offline use.
      if (response && response.ok && response.type === 'basic') {
        cache.put(request, response.clone());
      }
      return response;
    } catch (error) {
      // Offline: serve the cached copy if we have one.
      const cached = await cache.match(request);
      if (cached) return cached;
      // For a page load with no cached match, fall back to the cached app shell.
      if (isNavigation) {
        const shell = (await cache.match('index.html')) || (await cache.match('./'));
        if (shell) return shell;
      }
      throw error;
    }
  })());
});
