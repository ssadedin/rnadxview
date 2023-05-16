<template>
  <v-card>
    <v-card-title>Select Panel from PanelApp</v-card-title>
    <v-card-text>
        <v-autocomplete
          v-model="chosenPanelInfoName"
          :items="panelInfoNames"
          dense
          chips
          small-chips
          label="Choose a Panel"
          solo
          id="panelAppGenelistSelector"
        ></v-autocomplete>
    </v-card-text>
    <v-card-actions>
      <v-btn
        text
        @click="closePanelAppDialog({ showPanelDialog: false })"
      >
        Close
      </v-btn>
      <v-btn
        text
        @click="getGenesAndEmit({ showPanelDialog: false })"
      >
        Import
      </v-btn>
    </v-card-actions>
  </v-card>
</template>

<script>
import PanelAppApi from "@/panelappapi.js";

export default {
  async mounted() {
    this.panelappapi = new PanelAppApi();
    this.panelInfos.push(...(await this.panelappapi.get_panel_infos()));
    this.panelInfos.forEach(obj => {
      this.panelInfoNames.push(obj.name);
    })
  },

  data: () => {
    return {
      panelapp_data: {
        showPanelDialog: true,
        chosenPanelName: null,
        chosenPanelGenes: null,
        chosenPanelInfo: null,
        chosenPanelUri: null,
      },
      chosenPanelInfoName: null,
      chosenPanelInfoObj: null,
      panelInfos: [],
      panelInfoNames: [],
      panelappapi: null,
    };
  },
  methods: {
    closePanelAppDialog(fieldToUpdate) {
      this.panelapp_data = { ...this.panelapp_data, ...fieldToUpdate };
      this.$emit("panelappevent", this.panelapp_data);
    },
    async getGenesAndEmit(fieldToUpdate) {
      // chosenPanelInfoObj should be updated
      this.chosenPanelInfoObj = this.panelInfos[this.panelInfoNames.indexOf(this.chosenPanelInfoName)]
      this.panelapp_data = { ...this.panelapp_data, ...fieldToUpdate };
      let [gene_infos, uri] = await this.panelappapi.get_panel_genes(
        this.chosenPanelInfoObj.id
      );
      this.panelapp_data.chosenPanelGenes = gene_infos
      this.panelapp_data.chosenPanelName = this.chosenPanelInfoObj.name;
      this.panelapp_data.chosenPanelInfo = this.chosenPanelInfoObj;
      this.panelapp_data.chosenPanelUri = uri;
      this.$emit("panelappevent", this.panelapp_data);
    },
  },
};
</script>

<style scoped></style>
