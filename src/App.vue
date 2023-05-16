<template>
  <v-app>
    <v-app-bar
      app
      color="primary"
      dark
    >
      <div class=" align-center">
          <h2>RNADX Results for {{sampleInfo && sampleInfo.identifier}}</h2>
          <div v-if='$refs.rnadxView && $refs.rnadxView.sampleState' class='reviewState'>
               {{sampleState.review_state}}
               <span v-if='sampleState.result'>({{sampleState.result}})</span>
                &nbsp;<a href='#' @click='$refs.reviewStateDlg.show=true'>edit</a>
          </div>
          <div v-else>
              <div v-if='$refs.rnadxView' class='reviewState'>
                <a href='#' @click.prevent='initReviewState'>set review state</a>
              </div>
          </div>
      </div>

      <v-spacer></v-spacer>
      
      <span v-if='$refs.rnadxView'>
          <v-btn rounded  v-for='panel of $refs.rnadxView.usedPanels'
            class='mr-1 elevation-0' x-small 
            :color='(panel.name==currentPanel) ? "green" : "blue"' 
            @click.exact='selectPanel(panel)' 
            @click.shift.exact='removePanel(panel)'
            title='Use shift+click to remove this panel'
            v-bind:key='panel.name'>{{panel.name}}</v-btn>
      </span>
      
      <a href='#' @click='loadIGVSession()' class='ml-2' style='color: white; text-underline-offset: 2px;'>IGV Session</a>

      <v-icon class='ml-4' v-if='connected' @click='showConnectServerDialog' white>mdi-power-plug</v-icon>
      <v-icon class='ml-4' v-else @click='showConnectServerDialog'>mdi-power-plug-off</v-icon>

    </v-app-bar>
    
    <v-snackbar :timeout="2000" top v-model="snackbar" ref='snackbar'>                                                                                                                                                                                                      
          {{snackbarMessage}}
          <v-btn text color="blue" @click.native="snackbar = false">Close</v-btn>
    </v-snackbar>    

    <v-main>
      <RNADXView ref='rnadxView' :username='username' :sample-info='sampleInfo'/>
    </v-main>
    
    <SimpleDialog v-model='showConnectDialog' :width=500 ref='connectDialog' title='Connect to Shared Tags' @ok='connectPouchDB'>
        <v-text-field 
            label='Your Name' 
            hint='This is how you will be identified to other users who see your annotations'
            id='username'
            v-model='username'
        ></v-text-field>
        <v-text-field type='password' label='Password' hint='The password for the server' v-model='password' password></v-text-field>
    </SimpleDialog>
    
    <SimpleDialog v-if='$refs.rnadxView && $refs.rnadxView.sampleState' v-model='showReviewStateDialog' :width=500 ref='reviewStateDlg' title='Review State' @ok='updateReviewState'>
        <v-select :items='reviewStates' label="Set review state" v-model="sampleState.review_state" ></v-select>
        <v-select :items='reviewResults' label="Set review result" v-model="sampleState.result" ></v-select>
        <v-textarea v-model='sampleState.notes'></v-textarea>
    </SimpleDialog>
    
  </v-app>
</template>

<style>
div.reviewState {
    font-size: 85%;
}

.reviewState a {
    color: white !important;
    
    text-underline-offset: 2px;
}

</style>

<script>
import axios from 'axios'

import RNADXView from './components/RNADXView';
import SimpleDialog from './components/SimpleDialog';

import PouchDB from 'pouchdb';

let pouchBaseURL = window.location.origin

export default {
  name: 'App',

  components: {
    RNADXView,
    SimpleDialog
  },
  
  mounted: async function() {

    this.sampleInfo = await this.loadSampleInfo()
    
    let dbName = 'rnadx1-' + this.sampleInfo.identifier.toLowerCase()
    
    window.pouch = new PouchDB(dbName)
    
    this.remoteDBName = pouchBaseURL + '/db/' + dbName

    window.document.title = this.sampleId + ' RNADX' 
  },
  
  computed: {
    sampleId() { 
        return this.sampleInfo ? this.sampleInfo.identifier : '' 
    },
    
    currentPanel() {
        return this.$refs.rnadxView.currentPanel
    },
    
    sampleState() {
        return this.$refs.rnadxView.sampleState
    }
  },
  
  methods: {

    async loadSampleInfo() {
      return window.sampleInfo ? window.sampleInfo : (await axios.get('sampleInfo.json')).data
    },
   
    loadIGVSession() {
        
        let assetPath = this.sampleInfo.session_file
        
        if(this.isWindows()) {
               assetPath = assetPath.replace('/misc/vcgs','//data.mcri.edu.au/vcgs')
        }
        else
        if(this.isMac()) {
               assetPath = assetPath.replace('/misc/vcgs','/Volumes/vcgs')
        }        
        
        let igvLink = `http://localhost:60151/load?file=${assetPath}`
        
        this.$refs.rnadxView.$refs.tableIGVIframe.src=igvLink
    },
    
    isWindows() {
        return navigator.platform.indexOf('Win') > -1
    },

    isMac() {
        return navigator.platform == 'MacIntel'
    },
    
    showConnectServerDialog() {
        if(window.localStorage['rnadx.username'])
            this.username = window.localStorage['rnadx.username']

        if(window.localStorage['rnadx.password'])
            this.password = window.localStorage['rnadx.password']

        this.$refs.connectDialog.show = true
    },
    
    connectPouchDB() {
        
        console.log("Connecting to server")
        
        window.localStorage['rnadx.username'] = this.username;
        window.localStorage['rnadx.password'] = this.password;
        
        window.remoteDB = new PouchDB(this.remoteDBName, { auth: { "username":  this.username, "password": this.password} })
        
        window.pouch.replicate.from(window.remoteDB).on('complete', () => {

            console.log("Couchdb replicate complete")
            
            this.$refs.rnadxView.loadState()

            this.toast('Connected to ' + this.remoteDBName)
            this.connected = true
            
            let sync = PouchDB.sync(window.pouch, window.remoteDB, {live: true, retry: true})
            sync.on('change', () => {
                console.log("Couchdb sync complete")
                this.$refs.rnadxView.loadState()
            })
        })
    },
    
    selectPanel(panel) {
        this.$refs.rnadxView.selectPanel(panel)
    },
    
    removePanel(panel) {
        this.$refs.rnadxView.removePanel(panel)
    },
    
    async updateReviewState() {

        console.log("Updating sample state to: ", this.sampleState)
        
        this.$refs.rnadxView.saveMetaData()
    },
    
    initReviewState() {
        this.$refs.rnadxView.sampleState = {
            review_state : this.reviewStates[0],
            result : null,
            notes: null,
            updated_by : null
        }
        
        this.$nextTick(() => 
            this.$refs.reviewStateDlg.show = true)
    }
  },

  data: () => ({
    pouchURL : null,
    sampleInfo: null,
    showConnectDialog: false,
    showReviewStateDialog: false,
    remoteDBName: null,
    username: null,
    password: null,
    snackbar : false,
    snackbarMessage : null,
    connected: false,
    reviewStates: ["Not Reviewed","First Pass","Second Pass","Reviewed","Closed"],
    reviewResults: ["", "Undiagnosed", "Followup Required", "Potential Diagnosis", "Diagnosed"],
  }),
};
</script>
