<template>
    <a class='geneCardsLink' title='Open gene in GeneCards' :href='`http://www.genecards.org/cgi-bin/carddisp.pl?gene=${encode(gene)}&search=${encode(gene)}#${section}`' target='genecards'><slot>{{gene}}</slot></a> 
</template>

<style>
a.geneCardsLink {
    text-decoration: none;
}

a.geneCardsLink:hover, a.geneCardsLink:active {
    text-decoration: underline;
}
</style>

<script>
    export default {
        
        name: 'GeneCardsLink',
        
        methods: {
            encode(arg) {
                return encodeURIComponent(arg);
            }
        },
        
        props: {
            gene: String,
            section: { type: String, default: 'diseases'}
        }

    }
</script>