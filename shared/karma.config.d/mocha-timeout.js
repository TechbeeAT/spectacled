// Raises the Mocha per-test timeout for the Kotlin/JS browser test run (jsBrowserTest).
//
// The first test to execute in the browser suite pays one-time costs that Mocha counts against that
// single test's timeout: evaluating the whole test bundle, loading the skiko wasm runtime, and the
// first TimeZone.of(...) call, which parses the full packed IANA tz database shipped by
// @js-joda/timezone. On slower CI runners that first test can exceed Mocha's 2000ms default and
// fail with a misleading "Timeout of 2000ms exceeded" even though its assertion is trivial (this is
// why IcsDateTimeTest.effectiveZone_dateOnlyAlwaysWinsAsUtc, the first ics test, was the one
// reported as failing). 20s gives ample headroom for the cold-start cost without masking a
// genuinely hung async test.
//
// Merged into the existing client config (rather than replacing it) so Kotlin's own Karma settings
// are preserved, mirroring the merge style of webpack.config.d/node-core-fallback.js.
config.client = config.client || {};
config.client.mocha = Object.assign({}, config.client.mocha, {
    timeout: 20000,
});
