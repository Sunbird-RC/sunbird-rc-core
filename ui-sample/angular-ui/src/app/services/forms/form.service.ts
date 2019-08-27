import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import signUp  from "../jsons/signUp.json";
import person from "../jsons/person.json";

@Injectable({
  providedIn: 'root'
})
export class FormService {

  constructor(private http: HttpClient) { }


  getFormConfig(formName) {
    return signUp.data;
  }
  
  getPersonForm() {
    return person.data;
  }
}
