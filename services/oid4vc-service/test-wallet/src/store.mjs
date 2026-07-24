// Minimal in-memory credential store. In the browser build this is swapped for
// a localStorage-backed version (see web/app.js) but the shape is identical.
export class CredentialStore {
  constructor() {
    this.items = [];
  }

  add(item) {
    // item: { format, raw, configId, issuer, docType?, claims?, label }
    this.items.push({ id: this.items.length + 1, ...item });
    return this.items[this.items.length - 1];
  }

  all() {
    return this.items;
  }

  // Find a stored credential that can satisfy a single DCQL credential query.
  findForQuery(cq) {
    return this.items.find((it) => {
      if (cq.format && cq.format !== it.format) return false;
      const meta = cq.meta || {};
      if (meta.doctype_value && it.docType !== meta.doctype_value) return false;
      if (Array.isArray(meta.vct_values) && !meta.vct_values.includes(it.vct)) return false;
      return true;
    });
  }
}
