/*
 A general dialog with automatically handled OK and Cancel buttons, 
 escape key handling and a title and body section.
*/
<template>
    <v-dialog v-model='show' v-if='show' max-width=600>
        <v-card>
            <v-card-title>
            <v-row class='mt-1 mb-1 ml-0'>
            {{item.gene}} <v-spacer></v-spacer>
            <v-avatar size=30 class='primary white--text' v-if='user'>{{user[0].toUpperCase()}}</v-avatar>
            </v-row>
            </v-card-title>
            <v-card-text>
                <v-text-field ref='tagField' autofocus label='Tag' hint='Enter the tag for this gene' v-model='name'></v-text-field>
                <v-textarea label='Notes' hint='Enter notes about why you are adding this tag' v-model='notes'></v-textarea>
            </v-card-text>
           
            <v-color-picker
                  dot-size="14"
                  hide-canvas
                  hide-inputs
                  hide-sliders
                  mode="rgba"
                  show-swatches
                  swatches-max-height="107"
                  :swatches=swatches
                  v-model='color'
                ></v-color-picker>
            <v-card-actions>
                <v-btn @click='$emit("saveTag", item, name, notes, color); show=false'>OK</v-btn>
                <v-btn @click='show=false'>Cancel</v-btn>
            </v-card-actions>
        </v-card>
    </v-dialog>
</template>

<style>
</style>

<script>

export default {
  
  name: 'tag-dialog',
  
  components: {
  },
  
  props: {
  },
  
  mounted: function() {
  },
  
  computed: {
  },
  
  watch: {
    show() {
    }
  },
  
  methods: {
  },
  
  data: () => { return {
      show: false,
      item : Object,
      name: null,
      notes: null,
      user: null,
      color: null,
      swatches: [
                ['#FF0000', '#E01BE0'],
                ['#FFFF00', '#FF971B'],
                ['#00CC00', '#707070'],
                ['#6134D6', '#00AAAA'],
                ['#0000FF', '#80492C'],
              ]      
  }}

}
</script>
