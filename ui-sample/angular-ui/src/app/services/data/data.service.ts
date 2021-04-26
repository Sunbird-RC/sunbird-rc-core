import { Injectable } from '@angular/core';
import { ServerResponse } from '../interfaces/serverResponse'
import { HttpOptions } from '../interfaces/httpOptions'
import { HttpClient, HttpHeaders } from '@angular/common/http'
import { mergeMap } from 'rxjs/operators';
import { of as observableOf, throwError as observableThrowError, Observable } from 'rxjs';
import appConfig from '../app.config.json';
import { map } from 'rxjs/operators';



@Injectable({
  providedIn: 'root'
})
export class DataService {

  http: HttpClient;
  baseUrl: string;

  constructor(http: HttpClient) {
    this.http = http;
    this.baseUrl = appConfig.URLS.UTIl_SERVICE_BASE_URL;
  }

  post(requestParam: any): Observable<ServerResponse> {
    const httpOptions: HttpOptions = {
      headers: requestParam.header ? this.getHeader(requestParam.header) : this.getHeader(),
      params: requestParam.param,
      observe: 'response'

    };
    return this.http.post(this.baseUrl + requestParam.url, requestParam.data, httpOptions).pipe(
      mergeMap(({ body, headers }: any) => {
        // replace ts time with header date , this value is used in telemetry
        body.ts = this.getDateDiff((headers.get('Date')));
        if (body.responseCode !== 'OK') {
          return observableThrowError(body);
        }
        return observableOf(body);
      }));
  }

/**
   * for making get api calls
   *
   * @param requestParam interface
   */
  get(requestParam: any): Observable<ServerResponse> {
    const httpOptions: HttpOptions = {
      headers: requestParam.header ?  this.getHeader(requestParam.header) : this.getHeader(),
      params: requestParam.param
    };
    return this.http.get(this.baseUrl + requestParam.url, httpOptions).pipe(
      mergeMap((data: ServerResponse) => {
        if (data.responseCode !== 'OK') {
          return observableThrowError(data);
        }
        return observableOf(data);
      }));
  }
  getImg(requestParam: any): Observable<any> {
    const headers = new HttpHeaders().set('Content-Type', 'text/plain; charset=utf-8');
  
    return this.http.post(this.baseUrl + requestParam.url, 
      { qrCode: requestParam.qrCode }, 
      { headers, responseType: 'text', params:requestParam.param}
    ).pipe();
  }
  private getHeader(headers?: any) {
    const default_headers = {
      'Accept': 'application/json',
      'Content-Type': 'application/json',
      'Access-Control-Allow-Origin': '*',
    };
    if(headers) {
      if (headers.Authorization) {
        default_headers['Authorization'] = 'Bearer ' + headers.Authorization;
      }
      if(headers.role) {
        default_headers['role'] = headers.role
      }
    }
   
    return { ...default_headers };
  }


  getImage(imageUrl: string) {
    return this.http.get(imageUrl, {observe: 'response', responseType: 'blob'})
      .pipe(map((res) => {
        return new Blob([res.body], {type: res.headers.get('Content-Type')});
      }))
  }

  private getDateDiff(serverdate): number {
    const currentdate: any = new Date();
    const serverDate: any = new Date(serverdate);
    if (serverdate) {
      return (serverDate - currentdate) / 1000;
    } else {
      return 0;
    }
  }
}


