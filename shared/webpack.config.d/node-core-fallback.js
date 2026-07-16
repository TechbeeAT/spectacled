// Silences the "Module not found: Error: Can't resolve 'os' / 'path'" webpack messages emitted when
// bundling the shared module's browser test bundle (spectacled-shared-test). A transitive
// dependency (the ktor JS client) references those Node core modules in code paths the browser
// never runs; webpack 5 no longer auto-polyfills them. Mapping them to `false` resolves them to an
// empty module, which is correct for the browser and removes the noise.
//
// Merged into the existing resolve config (rather than replacing it) so Kotlin's own aliases and
// extension settings are preserved.
config.resolve = config.resolve || {};
config.resolve.fallback = Object.assign({}, config.resolve.fallback, {
    os: false,
    path: false,
});
