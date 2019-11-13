import { Component, OnInit, ViewChild } from '@angular/core';
import { ResourceService } from '../../services/resource/resource.service';
import { FormService } from '../../services/forms/form.service'
import { DefaultTemplateComponent } from '../default-template/default-template.component';
import urlConfig from '../../services/urlConfig.json';
import { DataService } from '../../services/data/data.service';
import { Router, ActivatedRoute } from '@angular/router'
import { UserService } from 'src/app/services/user/user.service';
import { CacheService } from 'ng2-cache-service';
import appConfig from '../../services/app.config.json';

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

  constructor(resourceService: ResourceService, formService: FormService, dataService: DataService, route: Router, public userService: UserService, private cacheService: CacheService) {
    this.resourceService = resourceService;
    this.formService = formService;
    this.dataService = dataService;
    this.router = route;
  }

  ngOnInit() {
    this.formService.getFormConfig("employee").subscribe(res => {
      this.formFieldProperties = res.fields;
    })
  }

  registerNewUser() {
    let token;
    if (this.cacheService.get(appConfig.cacheServiceConfig.cacheVariables.UserToken)) {
      token = this.cacheService.get(appConfig.cacheServiceConfig.cacheVariables.UserToken);
    } else {
      token = this.userService.getUserToken
    }
    const requestData = {
      data: {
        request: this.formData.formInputData
      },
      header: {
        Authorization: token
      },
      url: urlConfig.URLS.REGISTER
    }
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
