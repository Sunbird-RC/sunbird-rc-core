import { Component, OnInit, ViewChild, OnChanges } from '@angular/core';
import { FormService } from '../../services/forms/form.service';
import { ResourceService } from '../../services/resource/resource.service';
import { KeycloakService } from 'keycloak-angular';
import { DefaultTemplateComponent } from '../default-template/default-template.component';
import { Subject } from 'rxjs';
import { UserService } from '../../services/user/user.service';
import { Router } from '@angular/router'

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

  public keycloakAngular: KeycloakService;

  public userService: UserService;

  public userDetails: any;

  private userLogin: any;

  router: Router;

  private userRoles = [];


  constructor(formService: FormService, resouceService: ResourceService, keycloakAngular: KeycloakService, userService: UserService, route: Router) {
    this.formService = formService;
    this.resourceService = resouceService;
    this.keycloakAngular = keycloakAngular;
    this.userService = userService;
    this.router = route;

  }

  ngOnInit() {
    this.fetchFormData()
    this.resourceService.initialize();
    let userDetails = this.keycloakAngular.getToken();
    this.userLogin = this.userService.loggedIn;
    this.userDetails = this.userService.getUserInfo();
    this.userRoles = this.userService.getUserRoles;
    if(this.userLogin)  {
      this.router.navigate(['home']);
    }
  }

  fetchFormData() {
    this.formService.getFormConfig("login").subscribe(res => {
      this.formFieldProperties = res.fields;
    });
  } 
  
}
