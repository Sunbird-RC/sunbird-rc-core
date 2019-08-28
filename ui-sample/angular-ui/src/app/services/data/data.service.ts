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
      params: requestParam.param
    };
    return this.http.post(this.baseUrl+requestParam.url, requestParam.data, httpOptions).pipe(
      mergeMap((data: ServerResponse) => {
        if (data.responseCode !== 'OK') {
          return observableThrowError(data);
        }
        return observableOf(data);
      }));
  }

  private getHeader(headers?: HttpOptions['headers']): HttpOptions['headers'] {
    const default_headers = {
      'Accept': 'application/json',
    };

    return { ...default_headers };

}
}


