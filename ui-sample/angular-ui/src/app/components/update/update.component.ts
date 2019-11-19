import { Component, OnInit, ViewChild } from '@angular/core';
import { ResourceService } from '../../services/resource/resource.service'
import { FormService } from '../../services/forms/form.service'
import { from } from 'rxjs';
import { DefaultTemplateComponent } from '../default-template/default-template.component';
import { DataService } from 'src/app/services/data/data.service';
import { Router, ActivatedRoute } from '@angular/router';
import urlConfig from '../../services/urlConfig.json';
import { CacheService } from 'ng2-cache-service';
import appConfig from '../../services/app.config.json';
import { UserService } from '../../services/user/user.service';

@Component({
  selector: 'app-update',
  templateUrl: './update.component.html',
  styleUrls: ['./update.component.scss']
})
export class UpdateComponent implements OnInit {
  @ViewChild('formData') formData: DefaultTemplateComponent;

  resourceService: ResourceService;
  formService: FormService;
  public formFieldProperties: any;
  dataService: DataService;
  router: Router;
  userId: String;
  activatedRoute: ActivatedRoute;
  userService: UserService;
  constructor(resourceService: ResourceService, formService: FormService, dataService: DataService, route: Router, activatedRoute: ActivatedRoute,
    userService: UserService, public cacheService: CacheService) {
    this.resourceService = resourceService;
    this.formService = formService;
    this.dataService = dataService;
    this.router = route;
    this.activatedRoute = activatedRoute
    this.userService = userService;
  }

  ngOnInit() {
    this.userId = this.activatedRoute.snapshot.queryParams.userId;
    this.getFormTemplate();
  }

  getFormTemplate() {
    let token = this.cacheService.get(appConfig.cacheServiceConfig.cacheVariables.UserToken);
    if (!token) {
      token = this.cacheService.get(appConfig.cacheServiceConfig.cacheVariables.UserToken);
    } 
    const requestData = {
      url: urlConfig.URLS.FORM_TEPLATE,
      header: {
       userToken: token
      }
    }
    this.dataService.get(requestData).subscribe(res =>{
      if(res.responseCode === 'OK')
      {
        this.formFieldProperties = res.result.formTemplate.data.fields;
      }
    });
  }

  updateInfo() {
    this.formData.formInputData['osid'] = this.userId; 
    const requestData = {
      data: {
        "id": "open-saber.registry.update",
        "request": {
          "Employee": this.formData.formInputData
        }
      },
      url: urlConfig.URLS.UPDATE
    };
    this.dataService.post(requestData).subscribe(response => {
      console.log(response)
      this.navigateToProfilePage();
    }, err => {
      // this.toasterService.error(this.resourceService.messages.fmsg.m0078);
    });
  }

  navigateToProfilePage() {
    this.router.navigate(['/profile', this.userId]);
  }
}
