import axios from "axios";

export default class PanelAppApi {
  constructor(api_url = "https://panelapp.agha.umccr.org/api/v1/") {
    this.api_url = new URL(api_url);
    this.panel_infos = [];
  }

  concatted_url(suffix) {
    return new URL(suffix, this.api_url.href).href;
  }

  async get_all_page_items(uri) {
    let data = [];
    // Need to keep iterating the pages until we get a 404
    let currPage = 1;
    while (true) {
      try {
        let response = await axios.get(uri, {
          params: {
            page: currPage,
          },
        });
        data.push(...response.data.results);
      } catch (err) {
        // Exit once we hit the page that can't give us data
        break;
      }
      currPage += 1;
    }
    return data;
  }

  async get_panel_infos() {
    if (this.panel_infos.length > 0) {
      return this.panel_infos;
    }
    let uri = this.concatted_url("panels/");
    this.panel_infos = await this.get_all_page_items(uri);
    return this.panel_infos;
  }
  
  get_inheritance(moi) {
    if(moi.includes('MONOALLELIC')) {
        return 'AD'
    }
    else
    if(moi.includes('BIALLELIC')) {
        return 'AR'
    }
    else
    if(moi.includes('X-LINKED')) {
        return 'XL'
    }
  }

  async get_panel_genes(panel_pk) {
    let uri = this.concatted_url(`panels/${panel_pk}/genes/`);
    let genes = await this.get_all_page_items(uri);
    let gene_names = [];
    for (let gene_obj of genes) {
      // Extract just the essential info we need
      gene_names.push(
        {
            gene_symbol : gene_obj.gene_data.gene_symbol,
            confidence_level : gene_obj.confidence_level,
            inheritance: this.get_inheritance(gene_obj.mode_of_inheritance)
        });
    }
    return [gene_names, uri];
  }
}
