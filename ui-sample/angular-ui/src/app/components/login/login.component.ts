import { Component, OnInit, ViewChild } from '@angular/core';
import { FormService } from '../../services/forms/form.service';
import { ResourceService } from '../../services/resource/resource.service';
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
  constructor(formService: FormService, resouceService: ResourceService) {
    this.formService = formService;
    this.resourceService = resouceService;

  }

  ngOnInit() {
    this.fetchFormData()
    this.resourceService.initialize();
  }
  fetchFormData() {
      this.formService.getFormConfig("login").subscribe(res => {
        this.formFieldProperties = res.fields;
      });
  }


}
