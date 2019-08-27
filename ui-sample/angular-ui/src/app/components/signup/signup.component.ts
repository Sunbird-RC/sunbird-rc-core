import { Component, OnInit } from '@angular/core';
import { FormService } from '../../services/forms/form.service';
import { ResourceService } from '../../services/resource/resource.service';



@Component({
  selector: 'app-signup',
  templateUrl: './signup.component.html',
  styleUrls: ['./signup.component.scss']
})
export class SignupComponent implements OnInit {

  public formService: FormService;

  public formFieldProperties: any;

  private action;

  public resourceService: ResourceService;
  constructor(formService: FormService, resouceService: ResourceService) {
    this.formService = formService;
    this.resourceService = resouceService;

  }

  ngOnInit() {
    this.fetchFormData()
    this.resourceService.initialize();
  }
  fetchFormData() {
    this.formFieldProperties = this.formService.getFormConfig("signUp").fields;
    
  }

}
