import { Component, OnInit, ViewChild } from '@angular/core';
import { ResourceService } from '../../services/resource/resource.service'
import { FormService } from '../../services/forms/form.service'
import { from } from 'rxjs';
import { DefaultTemplateComponent } from '../default-template/default-template.component';
import { DataService } from 'src/app/services/data/data.service';
import { Router, ActivatedRoute } from '@angular/router';
import { CacheService } from 'ng2-cache-service';
import appConfig from '../../services/app.config.json';
import { UserService } from '../../services/user/user.service';
import _ from 'lodash-es';

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
  public showLoader = true;
  viewOwnerProfile : string;

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
   this.viewOwnerProfile = this.activatedRoute.snapshot.queryParams.role;
    this.activatedRoute.params.subscribe((params) => {
      this.userId = params.userId;
    });
    this.getFormTemplate();
  }

  getFormTemplate() {
    let token = this.cacheService.get(appConfig.cacheServiceConfig.cacheVariables.UserToken);
    if (_.isEmpty(token)) {
      token = this.userService.getUserToken;
    } 
    const requestData = {
      url: appConfig.URLS.FORM_TEPLATE,
      header: {
       userToken: token,
       role: this.viewOwnerProfile
      }
    }
    this.dataService.get(requestData).subscribe(res =>{
      if(res.responseCode === 'OK')
      {
        this.showLoader =false;
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
      url: appConfig.URLS.UPDATE
    };
    this.dataService.post(requestData).subscribe(response => {
      console.log(response)
      this.navigateToProfilePage();
    }, err => {
      // this.toasterService.error(this.resourceService.messages.fmsg.m0078);
    });
  }

  navigateToProfilePage() {
    if(this.viewOwnerProfile) {
      this.router.navigate(['/profile', this.userId, this.viewOwnerProfile]);
    }
    else {
      this.router.navigate(['/profile', this.userId]);
    }
  }
}
