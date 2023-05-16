/*
 A general dialog with automatically handled OK and Cancel buttons, 
 escape key handling and a title and body section.
*/
<template>
    <v-dialog v-model='show' persistent :max-width="width">
      <v-card>
        <v-card-title class="headline">{{title}}</v-card-title>
        <v-card-text>
          <form :id='id'>
            <slot></slot>
          </form>
        </v-card-text>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn id='cancelButton' color="gray darken-1" text @click.native="show=false;$emit('cancel')">Cancel</v-btn>
          <v-btn id='okButton' color="green darken-1" text @click.native="show=false;$emit('ok',item)">OK</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
</template>

<style>
    div.history {
        max-height: 100px;
        overflow: auto;
    }
    table.historyTable {
        min-width: 260px;
    }
    table.historyTable td, table.historyTable th  {
        padding: 0 5px !important;
        margin: 0 !important;
        height: auto !important;
    }
    span.infoHd {
        color: #444;
        font-weight: 500;
    }
    span.infoHd h3 {
        display: inline;
        color: green;
    }
</style>

<script>

export default {
  
  name: 'simple-dialog',
  
  components: {
  },
  
  props: {
      id: String,
      title: String,
      width: { type: Number, default: 290 },
  },
  
  mounted: function() {
      this.$emit('mounted')
      this.$emit('setFocus')
  },
  
  computed: {
  },
  
  watch: {
      show: function() {
            if(this.show) {
                 this.$nextTick(() => this.$emit('setFocus'))
            }
            else {
                // we can find that an element in the dialog retains focus, causing the 
                // later keystrokes to get ignored on page because it thinks an element is focused
                if(document.activeElement)
                    document.activeElement.blur()
            }
      }
  },
  
  methods: {
  },
  
  data: () => { return {
      show: false,
      item: null
  }}
}
</script>
