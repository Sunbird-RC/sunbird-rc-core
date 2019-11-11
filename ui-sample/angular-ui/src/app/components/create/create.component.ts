import { Component, OnInit, ViewChild } from '@angular/core';
import { ResourceService } from '../../services/resource/resource.service';
import { FormService } from '../../services/forms/form.service'
import { DefaultTemplateComponent } from '../default-template/default-template.component';
import urlConfig from '../../services/urlConfig.json';
import { DataService } from '../../services/data/data.service';
import { Router, ActivatedRoute } from '@angular/router'

@Component({
  selector: 'app-create',
  templateUrl: './create.component.html',
  styleUrls: ['./create.component.scss']
})
export class CreateComponent implements OnInit {

  @ViewChild('formData') formData: DefaultTemplateComponent;

  resourceService: ResourceService;
  formService: FormService;
  public formFieldProperties: any;
  dataService: DataService;
  router: Router;

  constructor(resourceService: ResourceService, formService: FormService, dataService: DataService, route: Router) {
    this.resourceService = resourceService;
    this.formService = formService;
    this.dataService = dataService;
    this.router = route;
  }

  ngOnInit() {
      this.formService.getFormConfig("person").subscribe(res => {
      this.formFieldProperties = res.fields;
    })
  }

  validate()  {
      if(!!this.formData) {
        console.log("please fill the form");
      } else {
        this.createUser();
      }
  }

  createUser() {
    const requestData = {
      data: {
        id: "open-saber.registry.create",
        request: {
          Employee: this.formData.formInputData
        }
      },
      url: urlConfig.URLS.ADD
    };
    console.log("request data :", requestData)
    this.dataService.post(requestData).subscribe(response => {
      this.navigateToProfilePage(response.result.Employee.osid);
    }, err => {
      console.log("error", err);
    });
  }
  navigateToProfilePage(id: String) {
      this.router.navigate(['/profile', id])
  }
}
