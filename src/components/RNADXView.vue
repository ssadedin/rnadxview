<template>
  <v-container>
      <v-card v-if='panelApp'>
        <v-card-title>{{currentPanel}} 
            <span class='subtitle ml-4' >({{panelApp.chosenPanelInfo.disease_group}}, {{panelApp.chosenPanelInfo.stats.number_of_genes}} Genes)</span>
        </v-card-title>
      </v-card>
      <v-layout class='mt-5'>
        <v-row>
          <v-col>
              <v-text-field
                    v-model="search"
                    append-icon="mdi-magnify"
                    label="Search"
                    single-line
                    hide-details
                  ></v-text-field>
          </v-col>
          <v-spacer></v-spacer>
          <v-col>
             <v-switch
              v-model="taggedOnly"
              label="Tagged Only"
              title='Show only results that have been tagged'
            ></v-switch>
          </v-col>
          <v-col>
             <v-switch
              v-model="strengthFilter"
              :label="`Strong Results Only`"
              title='Only show results where: PSI changes by at least 0.3 OR both splicing and DE present OR significant DE with substantial log fold change'
            ></v-switch>
          </v-col>
          <v-col cols=2 class='pt-6'>
              <v-btn id='addGeneListButton' @click='addGeneList'>PanelApp Gene List</v-btn>
          </v-col>
          <v-col cols=1 class='pt-6'>
              <v-btn id='clearGeneListButton' class='ml-1' @click='panelApp=null'>Clear Genes</v-btn>
          </v-col>
        </v-row>
      </v-layout>
      <v-data-table
        :headers='columns'
        :items='results'
        :search='search'
        :single-expand="singleExpand"
        :expanded.sync="expanded"
        item-key='gene'
        show-expand
      >
      <template v-slot:item.gene="{ item }">
      <GeneCardsLink :gene='item.gene'></GeneCardsLink>
     </template>
     <template v-slot:item.fraser.length="{ item }">
         <span :class='fraserClass(item)'>{{item.fraser ? item.fraser.length : ''}}</span>
     </template>
     <template v-slot:item.deseq.length="{ item }">
     <td>
        <span v-if='deDirection(item.deseq)=="down"' :class='["deArrow ",deDirection(item.deseq), deSig(item.deseq)]'>
            <span v-for='i in deSize(item.deseq)' v-bind:key='i' class='pr-0 mr-0 pl-0 ml-0'>↓</span>
        </span>
        <span v-if='deDirection(item.deseq)=="up"' :class='["deArrow ",deDirection(item.deseq), deSig(item.deseq)]' >
            <span v-for='i in deSize(item.deseq)' v-bind:key='i' class='pr-0 mr-0 pl-0 ml-0'>↑</span>
        </span>
        <!-- ⇯ ⤊ -->
     </td>
     </template>
     <template v-slot:item.variants.length="{ item }">
         <span v-if='item.variants && item.variants.length>0' class='variantSymbol' >{{variantSymbol(maxVariant(item.variants))}}</span>
     </template>

     <template v-slot:item.rating="{ item }">
     <td>
         <v-avatar
          class='panelAppAvatar'
          :color='geneConfidenceColor(item)'
          rounded
          size="20"
          @click='openPanelApp(item.gene)'
          v-if='panelApp && geneConfidence(item)>0 && panelApp.genes[item.gene]'>{{panelApp.genes[item.gene].inheritance}}</v-avatar>
     </td>
        
     </template>
     
     <template v-slot:item.tags="{ item}">
        <td @mouseover='hover(item, true)' @mouseleave='hover(item, false)'>
             <template v-for='tag of userAnnotations[item.gene]'>
                 <span v-if='tag.tag' 
                       v-bind:key='tag.tag' 
                       :title='tag.notes'
                       @click.alt.exact='removeTag(item,tag)' 
                       @click.exact='showTag(item,tag)' 
                       class='tag'
                       :style='`background-color: ${tag.color};`'
                 >{{tag.tag}}</span>
             </template>
             &nbsp;
             <v-icon :color='hovers[item.gene] ? "red" : "white"' @click='addTag(item)'>mdi-tag-multiple</v-icon>
         </td>
     </template>
     
     <template v-slot:expanded-item="{ headers, item }">
      <td :colspan="headers.length" class='pl-1 pr-1 pt-1 pb-1' >
          <v-container>
            <v-layout>
                <v-row>
                <v-col cols=6 class='pa-0 ma-0'>
                    <v-card>
                        <v-card-title class='pl-3 pb-0 mb-0'>Fraser</v-card-title>
                        <v-card-text class='pl-1'>
                            <table class='fraser'>
                            <tr><th>Location</th><th>PSI Value</th><th>Delta PSI</th><th>pValue</th><th>Case</th><th>Controls</th><th>Novel</th></tr>
                            <tr v-for='f in fraser[item.gene]' v-bind:key='f.span'>
                                <td><a href='#' class='tableLink' @click.stop.prevent='loadIGV(f)'>{{f.position}}-{{f.end}}</a></td>
                                <td>{{f.psiValue}}</td><td>{{f.deltaPsi}}</td><td>{{f.pValue.toPrecision(3)}}</td>
                                <td>{{f.counts}}/{{f.totalCounts}}</td>
                                <td>{{Math.round(f.meanCounts)}}/{{Math.round(f.meanTotalCounts)}}</td>
                                <td style='text-align: center; color: orange'>{{f.known=="TRUE"?"" : "★"}}</td>
                            </tr>
                            </table>
                        </v-card-text>
                    </v-card>
                </v-col>
                
                <v-col class='pa-0 ma-0 ml-1'>
                    <v-card class='pt-0 mt-0'>
                        <v-card-title class='pl-3 pb-0 mb-0 mt-0'>DESeq</v-card-title>
                        <v-card-text class='pl-1'>
                            <table class='fraser'>
                            <tr><th>Reads (/kb)</th><th>LFC</th><th>SE</th><th>pValue</th></tr>
                            <tr v-for='d in deseq[item.gene]' v-bind:key='d.geneid'>
                                <td>{{d.baseMean.toPrecision(4)}} ({{(1000*d.baseMean/d.geneSize).toPrecision(2)}})</td><td>{{d.log2FoldChange.toPrecision(3)}}</td><td>{{d.lfcSE.toPrecision(2)}}</td><td>{{d.pvalue.toPrecision(3)}} ({{d.padj.toPrecision(3)}})</td>
                            </tr>
                            </table>
                        </v-card-text>
                    </v-card>
                </v-col>
                
                <v-col cols=3 class='pa-0 ma-0 ml-1' v-if='item.variants && item.variants.length>0'>
                    <v-card class='pa-0'>
                        <v-card-title class='pl-3 pb-0 mb-0'>Variants</v-card-title>
                        <v-card-text class='pl-1'>
                            <table class='fraser'>
                            <tr><th>Position</th><th>Type</th><th>VAF</th><th>Depth</th><th>Inh</th></tr>
                            <tr v-for='v,i of item.variants' v-bind:key='i'>
                                <td><a class='variantLink' href='#' :title='v.dosage==1?"De novo" : "Recessive"' @click.prevent='loadIGVVariant(v)'>{{v.chr}}:{{v.pos}}</a></td>
                                <td>{{v.type}}</td>
                                <td>{{v.vaf.toPrecision(2)}}</td>
                                <td>{{v.dp}}</td>
                                <td>
                                   <span>{{(v.dosage == 1) ? 'dnv' : 'rec'}}</span>
                                </td>
                            </tr>
                            </table>
                        </v-card-text>
                    </v-card>
                </v-col>
                 
              </v-row>
            </v-layout>
          </v-container>
      </td>
    </template>
    </v-data-table>

    <v-dialog v-model="showPanelDialog" scrollable max-width="500px">
      <PanelApp ref='panelAppDlg' v-on:panelappevent="updatePanelData" />
    </v-dialog>
    <iframe ref=tableIGVIframe name=igv src='about:blank' style='display:none'></iframe>
    
    <TagDialog ref='tagDialog' @saveTag='saveTag'></TagDialog>

  </v-container>
</template>

<style>
span.deArrow {
    font-size: 120%;
}

span.deArrow.na {
    filter: opacity(0%)
}

span.deArrow.non {
    filter: opacity(20%)
}

span.deArrow.weak {
    filter: opacity(60%)
}
span.deArrow.sig {
    filter: opacity(90%)
}
span.deArrow.strong {
    filter: opacity(100%)
}

span.deArrow.down {
    color: blue;
}
span.deArrow.up {
    color: red;
}
table.fraser td,  table.fraser th {
    font-size: 12px;
    text-align: left;
    padding: 0px 5px;
}

table.fraser td {
    text-align: left;
    font-weight: normal;
}
.subtitle {
    font-weight: normal;
    color: #333;
    font-size: 90%;
}
a.tableLink {
    text-decoration: none;
}
a.tableLink:hover, a.tableLink:active {
    text-decoration: underline;
}
.gene-confidence {
    font-size: 30px;
}
.gene-confidence-1 {
    color : red;
}
.gene-confidence-2 {
    color : orange;
}
.gene-confidence-3 {
    color : green;
}
.panelAppAvatar {
    color: white; 
    font-weight: bold; 
    font-size: 10px;
    cursor: pointer;
}
.tag {
    background-color: navy;
    color: white;
    border-radius: 3px;
    font-size: 9px;
    padding: 3px;
    margin: 2px;
    cursor: pointer;
}

.variantSymbol, .variantSymbol a {
    color: darkorange;
    font-size: 9px;
    margin: 0px 3px;
}


.variantSymbol a, a.variantLink {
    text-decoration: none;
}
.variantSymbol a:active, .variantSymbol a:hover,  {
    text-decoration: underline;
}

.linked {
    font-weight: bold;
}

.super {
    color: red;
}

</style>

<script>

  import Vue from 'vue'
  import axios from 'axios'
  import _ from 'underscore'
  import PanelApp from './PanelApp.vue'
  import GeneCardsLink from './GeneCardsLink.vue'
  import TagDialog from './TagDialog.vue'
  import PanelAppApi from "@/panelappapi.js";

  const metaDataDocumentId = 'rnadx_metadata__'

  export default {
    name: 'RNADXView',
    
    props: [
       'sampleInfo',
       'username'
    ],
    
    components: {
        PanelApp,
        GeneCardsLink,
        TagDialog
    },
    
    mounted: async function() {
        window._ = _;
        console.log('I was mounted')
        let rawFraser = await this.loadFraser()
        this.fraser = _.groupBy(rawFraser, fraserResult => fraserResult.gene_name)
        console.log('Loaded fraser: ', this.fraser)

        let rawDESeq = await this.loadDESeq()
        this.deseq = _.groupBy(rawDESeq, deseqResult => deseqResult.geneid)
        console.log('Loaded deseq: ', this.deseq)

        this.variants = {}
        let rawVariants = await this.loadVariants()
        for(var variant of rawVariants) {
            for(var gene of variant.genes) {
                if(this.variants[gene]) {
                    this.variants[gene].push(variant)
                }
                else {
                    Vue.set(this.variants, gene, [variant])
                }
            }
        }
        Vue.set(this, 'variants', this.variants)

        console.log(`Indexed ${rawVariants.length} variants`)
        
        try {
            this.loadState()
        }
        catch(e) {
            console.log('Error loading state from pouch: ', e)
        }
    },
    
    methods: {
        
      addGeneList() {
        this.showPanelDialog = true
      },
      
      openPanelApp(gene) {
        window.open(`https://panelapp.agha.umccr.org/panels/entities/${gene}`, 'panelApp')
      },
      
      async removePanel(panel) {
        this.usedPanels = this.usedPanels.filter(p => p.name != panel.name)
        this.saveMetaData()
      },

      async selectPanel(panel) {
        
        let panelappapi = new PanelAppApi();
        
        let [gene_infos, uri] = await panelappapi.get_panel_genes(panel.id); 
        
        console.log(`Received ${gene_infos.length} genes from  ${uri}`)
        
        this.updatePanelData({
            chosenPanelInfo: { 
                name: panel.name, 
                id: panel.id,
                stats: { 
                    number_of_genes : gene_infos.length
                }
             },
            chosenPanelGenes: gene_infos
        })
      },

      updatePanelData(data) {
        console.log("Updating panel data", data)
        data.genes = _.indexBy(data.chosenPanelGenes, g => g.gene_symbol)
        this.panelApp = data
        let panelName = this.panelApp.chosenPanelInfo.name

        if(!_.find(this.usedPanels, p => p.name == panelName)) {
            this.usedPanels.push({ name: this.panelApp.chosenPanelInfo.name, id: this.panelApp.chosenPanelInfo.id})
            this.saveMetaData()
        }

        this.showPanelDialog = false
      },

      async loadFraser() {
        console.log("Loading fraser window fraser: ", window.fraser)
        let result = await this.loadTable('fraser')
        result.forEach(row => {
            row.position = row.seqnames + ':' + row.start
            row.span = row.position + '-' + row.end
        })
        return result
      },
      
      async loadVariants() {
        console.log("Loading variants : ", window.variants)
        let result = await this.loadTable('variants')
        console.log("loaded ${result.length} variants")
        return result
      },
 
      async loadDESeq() {
        return this.loadTable('deseq')
      },

      async loadTable(tableName) {
        console.log(`Loading ${tableName} results`)

        let raw = window[tableName] ? window[tableName] : (await axios.get(tableName + '.json')).data
        
        console.log(`Got ${tableName} results: `, raw)

        return raw.data.map( row => {
            let data = {}
            raw.columns.forEach((col, i) => {
                data[col] = row[i]
            })
            return data
        })
      },
      
      deSig(deseqRows) {
        if(!deseqRows)
            return 'na'
        if(deseqRows.length == 0)
            return 'na'

        let de = deseqRows[0]
        if(!de.pvalue)
            return 'na'
        
        let sig = 'non'
        if(de.pvalue<0.0001)
            sig = 'strong'
        else
        if(de.pvalue<0.001)
            sig = 'sig'
        else
        if(de.pvalue<0.05)
            sig = 'weak'
        else
            sig = 'non'

         return sig
      },
      
      deDirection(deseqRows) {
        if(!deseqRows || deseqRows.length == 0)
            return 'na'

        let de = deseqRows[0]
        if(!de.pvalue)
            return 'na'

        if(de.log2FoldChange<0)
            return 'down'
        else
            return 'up'
      },
      
      deSize(deseqRows) {
        
        if(deseqRows.length == 0)
            return 'na'
        
        let log2fc = Math.abs(deseqRows[0].log2FoldChange)
        if(log2fc<0.3)
            return 1
        else
        if(log2fc<1)
            return 2
        else
        if(log2fc<3)
            return 3
        return 4
      },
      
      loadIGV(fraser) {
          let igvLink = 
            `http://localhost:60151/goto?locus=${fraser.seqnames}:${fraser.start-50}-${fraser.end+50}`
          this.$refs.tableIGVIframe.src=igvLink
          return
      },
      
      loadIGVVariant(variant) {
          let igvLink = 
            `http://localhost:60151/goto?locus=${variant.chr}:${variant.pos-50}-${variant.pos+50}`
          this.$refs.tableIGVIframe.src=igvLink
          return
      },

      geneConfidence(item) {
        if(this.panelApp && this.panelApp.genes) 
            return this.panelApp.genes[item.gene].confidence_level 
        else
            return 0
      },

      geneConfidenceColor(item) {
        if(this.panelApp && this.panelApp.genes) 
            return ['', 'red','orange','green'][this.panelApp.genes[item.gene].confidence_level]
        else
            return null
      },
      
      geneConfidenceClass(item) {
        if(this.panelApp && this.panelApp.genes) 
            return 'gene-confidence-' + this.panelApp.genes[item.gene].confidence_level 
        else
            return null
      },
      
      addTag(item) {
        this.$refs.tagDialog.item = item
        this.$refs.tagDialog.show = true
      },
      
      saveTag(tagItem, newTagName, newTagNotes, color) {
        if(!tagItem.tags) {
            tagItem.tags = []
        }
        if(!tagItem.tags.find(t => t == newTagName)) {
            tagItem.tags.push(newTagName)
        }
        
        if(typeof(this.userAnnotations[tagItem.gene]) == 'undefined')
            Vue.set(this.userAnnotations, tagItem.gene, {})

        Vue.set(this.userAnnotations[tagItem.gene], newTagName, {
            tag: newTagName,
            notes: newTagNotes,
            user: this.username,
            color: color.hex || color
        })
        this.saveGene(tagItem.gene)
      },
      
      async saveGene(gene) {
        
        let doc = null
        try {
            doc = await window.pouch.get(gene)
            
            let meta = {
                _id : doc._id,
                _rev : doc._rev
            }
            
            doc = {
                ...this.userAnnotations[gene],
                ...meta
            }
           
            console.log('Updating existing doc', doc)
        }
        catch(e) {
            console.log(e)
            doc = {
                _id : gene,
                ...this.userAnnotations[gene]
            }
        }
        console.log("Sending doc to pouch", doc)
        window.pouch.put(doc)
      },
      
      async saveMetaData() {
        let doc = null
        try {
            doc = await window.pouch.get(metaDataDocumentId)
            
            let meta = {
                _id : doc._id,
                _rev : doc._rev
            }
            
            doc = {
                usedPanels: this.usedPanels,
                sampleState: this.sampleState,
                ...meta
            }
           
            console.log('Updating existing doc', doc)
        }
        catch(e) {
            console.log(e)
            doc = {
                _id : metaDataDocumentId,
                usedPanels: this.usedPanels,
                sampleState: this.sampleState
            }
        }
        console.log("Sending doc to pouch", doc)
        window.pouch.put(doc)
      },
      
      removeTag(item,tag) {
        if(!confirm("Delete tag " + tag.tag + " with notes:\n\n" + tag.notes))
            return
        Vue.delete(this.userAnnotations[item.gene], tag.tag)
        if(Object.keys(this.userAnnotations[item.gene]).length==0)
            Vue.delete(this.userAnnotations, item.gene)

        this.saveGene(item.gene)
      },
      
      showTag(item, tag) {
        this.$refs.tagDialog.item = item
        this.$refs.tagDialog.name = tag.tag
        this.$refs.tagDialog.notes = tag.notes
        this.$refs.tagDialog.user = tag.user
        this.$refs.tagDialog.color = tag.color
        this.$refs.tagDialog.show = true
      },

      hover(item, state) {
        Vue.set(this.hovers, item.gene, state)
      },
      
      saveState() {
        window.localStorage[this.storageKey] = JSON.stringify(this.userAnnotations)
      },
      
      async loadState() {
       
        let allDocs = await window.pouch.allDocs({
            include_docs:true
        })

        console.log('Got docs: ', allDocs)

        let geneRows = 
            allDocs
                .rows
                .filter(doc => doc._id != metaDataDocumentId )
        
        let mapped_annotations = _.indexBy(geneRows.map( ga =>  {
            return ga.doc
        }), ga => ga._id)
        
        console.log('Mapped annotations: ', mapped_annotations)
        
        allDocs.rows
               .filter(row => row.id == metaDataDocumentId)
               .forEach(row =>  { 
                    console.log("Setting panels from db")
                    if(row.doc.usedPanels) Vue.set(this, 'usedPanels', row.doc.usedPanels) 
                    if(row.doc.sampleState) Vue.set(this, 'sampleState', row.doc.sampleState) 
                })
        
        this.userAnnotations = mapped_annotations
      },
      
      maxVariant(variants) {
        for(var v of variants) {
            if(v.dosage == 1)
              return v
        }
        return variants[0]
      },
      
      variantSymbol(v) {
        if(!v)
            return ''
        // De novo => star
        if(v.dosage == 1)
            return "★"
        else
            return "⚭"
      },
      
      isStrongCombined(result) {
            // No fraser no deal
            if(!result.fraser || !result.deseq)
                return false

            // We are only interested if de is down at least enough that it could be NMD or similar
            if(this.deDirection(result.deseq) != "down" || this.deSize(result.deseq) < 1)
                return false

            // There is a fraser result and DE, but don't include this when the fraser result is 
            // trivial : novel, unlinked and small
            if(_.every(result.fraser, fraser => (fraser.deltaPsi < 0.1) && (fraser.psiValue < 0.05)))
                return false

            return true;
        },
        
        fraserClass(result) {
            let classes = []
            if(result.linked)
                classes.push('linked')
            if(result.superFraser)
                classes.push('super')
            return classes
        }
    },
    
    computed: {
        
        currentPanel() {
            if(this.panelApp && this.panelApp.chosenPanelInfo)
                return this.panelApp.chosenPanelInfo.name
            else
                return null
        },
        
        storageKey() {
            return window.location.href + '-' + this.sampleInfo.identifier;
        },
        
        allGenes() {
            return _.unique([].concat(Object.keys(this.fraser), Object.keys(this.deseq))).filter(g => g)
        },
        
        filteredGenes() {
            let genes = this.allGenes
            if(this.panelApp) {
                genes = genes.filter(gene => this.panelApp.genes[gene])
            }
            return genes
        },
        
        results() {
            let results = this.filteredGenes.map(gene => {
                let result = {
                    gene : gene,
                    fraser: this.fraser[gene],
                    deseq: this.deseq[gene],
                    variants: this.variants[gene]
                }
                
                result.superFraser = _.any(result.fraser, fraser => {
                    return Math.abs(fraser.deltaPsi) > 0.4  &&
                        (fraser.psiValue > 0.93  || fraser.psiValue < 0.1 || (fraser.psiValue-fraser.deltaPsi>0.9))
                        && fraser.known == "TRUE"
                })
        
                let starts = _.groupBy(result.fraser, x => x.start)
                let ends = _.groupBy(result.fraser, x => x.end)
                
                result.linked = 
                    (_.countBy(Object.values(starts), x => x.length>1)[true] || 0)+ 
                    (_.countBy(Object.values(ends), x => x.length>1)[true] || 0) + 
                    (_.countBy(Object.keys(starts), x => ends[x])[true] || 0)

                return result
            })

            if(this.strengthFilter) {
                results = results.filter( result => {
                    
                    // Not interested if the total mean counts in fraser are < 10 - this means the controls
                    // are all low coverage, we won't really conclude anything in this scenario
                    let usableFraser = result.fraser && !_.every(result.fraser, f => f.meanTotalCounts < 10)

                    if(this.isStrongCombined(result))
                        return usableFraser

                    if(this.deSig(result.deseq) == "strong" && this.deSize(result.deseq) > 2)
                        return true
                       
                    if(usableFraser && result.fraser.filter(x => Math.abs(x.deltaPsi) > 0.3 && x.pValue < 0.05).length>0)  {
                        return true
                    }
                    return false
                })
            }
            
            if(this.taggedOnly) {
               results = results.filter(result => this.userAnnotations[result.gene] ) 
            }
            
            return results
        },
        
    },

    data: () => ({
        search: null,
        fraser: { },
        deseq: { },
        hovers: { },
        singleExpand: true,
        expanded: [],
        panelApp: null,
        usedPanels: [],
        strengthFilter: false,
        taggedOnly: false,
        userAnnotations: { },
        variants : { },
        sampleState: null,
        columns: [
            { text: 'Gene', value: 'gene'},
            { text: 'Fraser', value: 'fraser.length'},
            { text: 'DESeq', value: 'deseq.length'},
            { text: 'Variant', value: 'variants.length'},
            { text: 'PanelApp', value: 'rating'},
            { text: 'Tags', value: 'tags'}
        ],
        showPanelDialog: false
    }),
  }
</script>
