const { merge } = require("webpack-merge");
const common = require("./webpack.common");

module.exports = merge(common, {
  mode: "development",
  devtool: "inline-source-map",
  devServer: {
    open: true,
    hot: true,
    compress: true,
    port: 8081,
    historyApiFallback: true,
    liveReload: true,
  },
  output: {
    filename: "[name].[contenthash].js",
    publicPath: "/",
  },
  module: {
    rules: [
      {
        test: /\.(sa|sc|c)ss$/i,
        use: ["style-loader", "css-loader", "sass-loader"],
      },
    ],
  },
});
