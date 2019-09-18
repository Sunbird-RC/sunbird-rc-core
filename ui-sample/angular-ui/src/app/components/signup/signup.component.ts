import { Component, OnInit, ViewChild } from '@angular/core';
import { FormService } from '../../services/forms/form.service';
import { ResourceService } from '../../services/resource/resource.service';
import { DefaultTemplateComponent } from '../default-template/default-template.component';
import { Subject } from 'rxjs';



@Component({
  selector: 'app-signup',
  templateUrl: './signup.component.html',
  styleUrls: ['./signup.component.scss']
})
export class SignupComponent implements OnInit {
  @ViewChild('formData') formData: DefaultTemplateComponent;

  public formService: FormService;

  public formFieldProperties: any;

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
      this.formService.getFormConfig("signup").subscribe(res => {
        this.formFieldProperties = res.fields;
      });
  }

}
