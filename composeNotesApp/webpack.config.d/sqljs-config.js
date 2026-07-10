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

const CopyWebpackPlugin = require('copy-webpack-plugin');
config.plugins.push(
    new CopyWebpackPlugin({
        patterns: [
            { from: '../../node_modules/sql.js/dist/sql-wasm.wasm', to: 'sql-wasm.wasm' },
            { from: '../../node_modules/sql.js/dist/sql-wasm.js', to: 'sql-wasm.js' }
        ]
    })
);

console.log("[SQL.js config] ✅ sql-wasm.wasm copy plugin configured!");
