// composeApp/webpack.config.d/sqljs.js

console.log("[SQL.js config] ✅ Custom Webpack config loaded!");

// {project}/webpack.config.d/sqljs.js
config.resolve = {
    fallback: {
        fs: false,
        path: false,
        crypto: false,
    }
};

const path = require('path');
const CopyWebpackPlugin = require('copy-webpack-plugin');
config.plugins.push(
    new CopyWebpackPlugin({
        patterns: [
            { from: '../../node_modules/sql.js/dist/sql-wasm.wasm', to: 'sql-wasm.wasm' },
            { from: '../../node_modules/sql.js/dist/sql-wasm.js', to: 'sql-wasm.js' },
            // __dirname always points at this webpack.config.d directory, unlike the
            // relative patterns above which resolve against webpack's own build context -
            // that context isn't guaranteed to be 2 levels above the repo root, it just
            // happens to coincide with node_modules being hoisted/symlinked nearby.
            { from: path.resolve(__dirname, '../../webWorker/spectacledSqlWorker.js'), to: 'spectacledSqlWorker.js' }
        ]
    })
);

console.log("[SQL.js config] ✅ sql-wasm.wasm copy plugin configured!");
