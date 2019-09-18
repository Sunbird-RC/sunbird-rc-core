import { Component, OnInit, ViewChild } from '@angular/core';
import { FormService } from '../../services/forms/form.service';
import { ResourceService } from '../../services/resource/resource.service';
import { KeycloakService } from 'keycloak-angular';
import { DefaultTemplateComponent } from '../default-template/default-template.component';
import {takeUntil} from 'rxjs/operators'
import { Subject } from 'rxjs';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent implements OnInit {
  @ViewChild('formData') formData: DefaultTemplateComponent;

  public formService: FormService;

  public formFieldProperties: any;

  public unsubscribe = new Subject<void>();

  public resourceService: ResourceService;

  public keycloakAngular : KeycloakService;
  constructor(formService: FormService, resouceService: ResourceService, keycloakAngular: KeycloakService) {
    this.formService = formService;
    this.resourceService = resouceService;
    this.keycloakAngular = keycloakAngular;
  }

  ngOnInit() {
    this.fetchFormData()
    this.resourceService.initialize();
    let userDetails = this.keycloakAngular.getToken();
  }

  fetchFormData() {
      this.formService.getFormConfig("login").subscribe(res => {
        this.formFieldProperties = res.fields;
      });
  }

}
