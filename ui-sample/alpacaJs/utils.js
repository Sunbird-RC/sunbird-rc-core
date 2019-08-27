function dataLocalizeRefresh(lang, fullLangText) {
  console.log('hey')
  var opts = { language: lang, pathPrefix: "./lang" };
  $("[data-localize]").localize("stringData", opts);
  console.log("dataLocalizeRefreshEnd " + lang);
  document.getElementById("langSelected").innerHTML = fullLangText
}

function dataLocalizeRefreshDefault() {
  var langSelected = document.getElementById("langSelected").innerText
  console.log(langSelected)
  var lang = "en"
  if (langSelected != null) {
    lang = langSelected.innerText
    console.log('lo', langSelected, lang)
  }
  var opts = { language: lang, pathPrefix: "./lang" };
  $("[data-localize]").localize("stringData", opts);
  console.log("dataLocalizeRefreshEnd " + lang);

}
