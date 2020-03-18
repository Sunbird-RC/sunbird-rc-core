import { Component, OnInit, ViewChild } from '@angular/core';
import { ResourceService } from '../../services/resource/resource.service';
import { FormService } from '../../services/forms/form.service'
import { DefaultTemplateComponent } from '../default-template/default-template.component';
import { DataService } from '../../services/data/data.service';
import { Router, ActivatedRoute } from '@angular/router'
import { UserService } from 'src/app/services/user/user.service';
import { CacheService } from 'ng2-cache-service';
import appConfig from '../../services/app.config.json';
import * as $ from 'jquery';
import { ToasterService } from 'src/app/services/toaster/toaster.service';
import _ from 'lodash-es';

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
  success = false;
  isError = false;
  errMessage: string;
  formInputDta = {}
  constructor(resourceService: ResourceService, formService: FormService, dataService: DataService, route: Router, public userService: UserService, private cacheService: CacheService,
    public toasterService: ToasterService) {
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

  /**
   * validates required fields
   */
  validate() {
    let emptyFields = [];
    _.map(this.formFieldProperties, field => {
      if (field.required) {
        if (!this.formData.formInputData[field.code]) {
          let findObj = _.find(this.formFieldProperties, { code: field.code });
          emptyFields.push(findObj.label);
        }
      }
    });
    if (emptyFields.length === 0) {
      this.registerNewUser();
    }
    else {
      this.toasterService.warning("Employee registration failed please provide required fields " + emptyFields.join(', '));
    }

  }

  registerNewUser() {
    let token;
    if (this.cacheService.get(appConfig.cacheServiceConfig.cacheVariables.UserToken)) {
      token = this.cacheService.get(appConfig.cacheServiceConfig.cacheVariables.UserToken);
    } else {
      token = this.userService.getUserToken
    }
    this.formData.formInputData['isActive'] = true;
    this.formData.formInputData['isOnboarded'] = false;
    const requestData = {
      data: {
        id: appConfig.API_ID.CREATE,
        request: {
          Employee: this.formData.formInputData
        }
      },
      header: {
        Authorization: token
      },
      url: appConfig.URLS.REGISTER
    }
    this.dataService.post(requestData).subscribe(response => {
      if (response.params.status === "SUCCESSFUL") {
        this.toasterService.success(this.resourceService.frmelmnts.msg.createUserSuccess);
        this.router.navigate(['/search'])
      }
    }, err => {
      this.errMessage = err.error.errorMessage;
      this.toasterService.error(this.resourceService.frmelmnts.msg.createUserUnSuccess + " : " + err.error.errorMessage);
    });
  }
  navigateToProfilePage(id: String) {
    this.router.navigate(['/profile', id])
  }

  close() {
    $(".close.icon").click(function () {
      $(this).parent().hide();
    });
    this.isError = false;
    this.success = false;
  }

}
