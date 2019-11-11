import { Injectable } from '@angular/core';
import {ServerResponse} from '../interfaces/serverResponse'
import {HttpOptions} from '../interfaces/httpOptions'
import {HttpClient} from '@angular/common/http'
import { mergeMap } from 'rxjs/operators';
import { of as observableOf, throwError as observableThrowError, Observable } from 'rxjs';
import urlConfig from '../urlConfig.json';




@Injectable({
  providedIn: 'root'
})
export class DataService {

   http: HttpClient;
   baseUrl: string;

  constructor(http: HttpClient) {
    this.http = http;
    this.baseUrl = urlConfig.URLS.BASE_URL;
  }

  post(requestParam: any): Observable<ServerResponse> {
    console.log(requestParam)
    const httpOptions: HttpOptions = {
      headers: requestParam.header ? this.getHeader(requestParam.header) : this.getHeader(),
      params: requestParam.param,
      observe: 'response'

    };
    return this.http.post(this.baseUrl+requestParam.url, requestParam.data, httpOptions).pipe(
      mergeMap(({body, headers}: any) => {
        // replace ts time with header date , this value is used in telemetry
        body.ts =  this.getDateDiff((headers.get('Date')));
        if (body.responseCode !== 'OK') {
          return observableThrowError(body);
        }
        return observableOf(body);
      }));
  }

  private getHeader(headers?: HttpOptions['headers']): HttpOptions['headers'] {
    const default_headers = {
      'Accept': 'application/json',
      'Content-Type':'application/json'
    };

    return { ...default_headers };

}


private getDateDiff (serverdate): number {
  const currentdate: any = new Date();
  const serverDate: any = new Date(serverdate);
  if (serverdate) {
    return ( serverDate - currentdate ) / 1000;
  } else {
    return 0;
  }
}
}


