//CSVTOJSON

// @ts-nocheck

var request = require("request")
var async = require("async")
var fs = require("fs");
var path = require("path");
var csvjson = require('csvjson');
console.log("Data",data);

var options = {
  delimiter : ',', // optional
  quote     : '"' // optional
};

var csvToJson = function (csvFileName) {
  var data = fs.readFileSync(path.join(__dirname, 'Data-Student-Parent-Teacher.csv'), { encoding : 'utf8'});
  const jsonObject = csvjson.toObject(data, options);
  console.log("JSON Object",jsonObject);
  return jsonObject;
}
