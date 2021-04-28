import { Injectable } from '@angular/core';
import * as json2csv from 'json2csv';
import { saveAs } from 'file-saver';

@Injectable({
  providedIn: 'root'
})
export class DownloadCSVFileService {


  Json2csvParser = json2csv.Parser;
  constructor() {

  }
  public downloadCsvFile(data: any, filename?: string) {
    let csvData = this.convertJsonToCSV(data);
    let file = new Blob([csvData], { type: 'text/csv;charset=utf-8' });
    saveAs(file, "Employees.csv");
  }



  public convertJsonToCSV(objArray: any, fields?) {
    let json2csvParser = new this.Json2csvParser({ opts: fields });
    let csv = json2csvParser.parse(objArray);
    return csv;
  }

}
