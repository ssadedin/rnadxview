import static groovy.json.JsonOutput.*

import gngs.RefGenes
import gngs.ToolBase
import gngs.VCF
import gngs.Variant
import gngs.RepeatMotif
import gngs.FASTA
import gngs.Region
import graxxia.TSV
import groovy.json.JsonOutput
import groovy.util.logging.Log
import htsjdk.samtools.util.IOUtil

@Log
class CreateReportAssets extends ToolBase {
    
    String sampleId
    
    RefGenes refgene

    @Override
    public void run() {
        
        this.sampleId = opts.sample
        
        log.info "Reading refgene database ..."
        this.refgene = new RefGenes(opts.refgene)
        
        // Copy the distribution assets
        log.info "Copying rnadxview distribution assets to $opts.dir ..."
        IOUtil.copyDirectoryTree(opts.dist, new File(opts.dir))
        
        def sessionFilePath = opts.session.absolutePath
        if(opts.map) {
           opts.map*.tokenize('=').each {
               sessionFilePath = sessionFilePath.replaceAll(it[0], it[1])
           } 
        }
        
        def sampleInfo = JsonOutput.toJson([
            identifier: sampleId,
            session_file: sessionFilePath
        ])
        
        new File(opts.dir, 'sampleInfo.json').text = sampleInfo       
        
        new File(opts.dir, sampleId + '.igv.session.xml').text = opts.session.text

        log.info "Reading $opts.fraser for sample $opts.sample"
        List<Map<String,?>> fraser = new TSV(opts.fraser)
            .toListMap()
            .grep {
                it.sampleID == sampleId
            }

        // It's a lot more work but it is much nicer to have line oriented JSON
        def fraserJSONOutput = tableToJson(fraser, 'fraser.json')

        log.info "Wrote FRASER output to $fraserJSONOutput "
        
        List<Map<String,?>> deseq = new TSV(opts.deseq).toListMap()
        addRPKM(deseq)
        def deJSONOutput = tableToJson(deseq, 'deseq.json')
        log.info "Wrote DESeq output to $deJSONOutput "
        
        def variantsJSONOutput
        if(opts.vcf) {
            log.info "Finding rare variants ...."
            List<Map> variants = findRareVariants(new VCF(opts.vcf))
            
            variantsJSONOutput = tableToJson(variants, 'variants.json')
            log.info "Wrote ${variants.size()} rare variants to $variantsJSONOutput ..."
        }

        log.info "Templating index.html with JSON and asset paths ..."
        new File(opts.dir, "index.html").text = 
            new File(opts.dist, "index.html")
            .text
            .replaceAll('"/css/','"css/')
            .replaceAll('"/js/','"js/')
            .replaceAll('<div id="datahere"></div>', 
            """
                <script>window.deseq = ${deJSONOutput.text}</script>
                <script>window.fraser = ${fraserJSONOutput.text}</script>
                <script>window.variants = ${variantsJSONOutput?.text}</script>
                <script>window.sampleInfo = ${sampleInfo}</script>
            """)
            
     }
     
    List<Map> findRareVariants(VCF vcf) {

        FASTA ref = new FASTA(opts.ref)
        
        if(!vcf.samples.size()==3) {
            log.warning "VCF is not a trio VCF: ignoring variants for this sample ..."
            return []
        }
        
        // A bit VCGS specific ...
        String probandPrefix = opts.sample + '-'
        
        int probandIndex = vcf.samples.findIndexOf { it.startsWith(probandPrefix) }
        List allIndexes = [0,1,2]
        List<Integer> otherIndexes = allIndexes.grep { it != probandIndex }
        
        return vcf.grep  { Variant v ->

            List dosages = v.dosages

               
            if(v.totalDepth<7)
                return false
                
            if(v.vaf <= 0.3)
                return false

            if(v.type == 'DEL' || v.type == 'INS') {
                RepeatMotif rep = ref.repeatAt(v.chr, v.pos)
                if(rep != null) {
                    return false
                }
            }

            if(dosages[0] == 2 && dosages[otherIndexes[0]] == 1 && dosages[otherIndexes[1]] == 1)
                return true

            if(dosages[0] == 1 && dosages[otherIndexes[0]] == 0 && dosages[otherIndexes[1]] == 0)
                return true

             return false
        }.collect { Variant v ->
            
            def genes = refgene.getGenes(new Region(v).widen(500))
            [
                chr : v.chr,
                pos: v.pos,
                ref : v.ref,
                alt : v.alt,
                type: v.type,
                dosage: v.getDosages()[0],
                genes: genes,
                vaf: v.vaf,
                dp: v.totalDepth
            ]
        }
        .grep { 
            it.genes
        }
    }
    
    private void addRPKM(List<Map> deseqResults) {
        for(Map<String,Object> deseq in deseqResults) {
            String gene = deseq.geneid
            def exons = this.refgene.getExons(gene, false)
            deseq.geneSize = exons.size()
        }
    }
     
    private File tableToJson(Iterable<Map> tableData, String name) {
        def tableJSONOutput = new File(opts.dir, name)
        tableJSONOutput.withWriter { w ->
            w.write('{')
            w.write('"columns": ' + toJson(tableData[0]*.key) + ',\n')
            w.write('"data":[\n')
            tableData.eachWithIndex { Map<String,?> row, int index ->
                if(index > 0)
                    w.write(',\n')
                w.write(toJson(row*.value))
            }
            w.write('\n]\n}')
        }
        return tableJSONOutput
    }
    
    static void main(String [] args) {
        cli('CreateReportAssets -sample <sample>', args) {
            session 'IGV session file to link to in the report', required: true, type: File
            map 'Mapping of file prefix to other file prefix', args: '*', required: false
            fraser 'Raw FRASER report', args:1, required: true, type: File
            deseq 'Raw results from DESeq analysis', args:1, required: true, type: File
            sample 'Sample to create reports assets for', args:1, required: true
            dist 'Location of the rnadxview distribution folder', args:1, required: true, type: File
            dir 'Output directory to write report assets to', args:1
            refgene 'RefSeq database to use for gene annotations', args:1, required: true
            ref 'Reference fasta for determining repeat sequences', args:1, required: true
            vcf 'VCF file containing rare variants to link in report', args:1, required: false
        }
    }
}
