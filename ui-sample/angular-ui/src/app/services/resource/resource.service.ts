
import {of as observableOf, throwError as observableThrowError,  Observable, BehaviorSubject, from } from 'rxjs';

import { Injectable, EventEmitter } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import * as _ from 'lodash-es';



@Injectable({
  providedIn: 'root'
})
export class ResourceService {
  static singletonInstance: ResourceService;

  messages: any = {};

  frmelmnts: any = {};

  public baseUrl: string;
  public http: HttpClient;

  public resourceBundles

  private _languageSelected = new BehaviorSubject<any>({});

  languageSelected$ = this._languageSelected.asObservable();

  
  constructor(httpClient: HttpClient) {
        this.http = httpClient
        this.getResource();
   }



   public initialize() {
    const range  = {value: 'en', label: 'English', dir: 'ltr'};
    // this.getResource('en', range);
  }
  /**
   * method to fetch resource bundle
  */
  public getResource(language = 'en', range?: any): void {
    const resourcebundles: any = null;
    if (resourcebundles) {
      this.messages = resourcebundles.messages;
      this.frmelmnts = resourcebundles.frmelmnts;
      this.getLanguageChange(range);
    } else {
      const option = {
        url: "../../../assets/resourceBundles/jsons" + '/' + language + ".json"
      };
      this.http.get(option.url).subscribe(
        (data: any) => {
          this.frmelmnts = _.merge({}, data.data.frmelmnts);
          this.getLanguageChange(range);
        },
        (err: any) => {
        }
      );
    }
  }

  getLanguageChange(language) {
    this._languageSelected.next(language);
  }
}
