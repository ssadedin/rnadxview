import Vue from 'vue'
import App from './App.vue'
import vuetify from './plugins/vuetify'

Vue.config.productionTip = false

Vue.mixin({
  methods: {
    toast(msg) {
        window.vue.$children[0].snackbar = true
        window.vue.$children[0].snackbarMessage = msg
    }
  }

})

console.log("Mixed in utils class")

window.vue = new Vue({
  vuetify,
  render: h => h(App)
}).$mount('#app')
