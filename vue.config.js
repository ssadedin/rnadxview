module.exports = {
  transpileDependencies: [
    'vuetify'
  ],

  devServer: {
     proxy: {
               "^/db": {
                   target: 'http://localhost:5984',
                   pathRewrite: { '^/db': '' },
                   changeOrigin: true
             }
      }
  }
}
