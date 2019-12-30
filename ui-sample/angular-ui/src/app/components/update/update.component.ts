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
import { ToasterService } from 'src/app/services/toaster/toaster.service';

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
  viewOwnerProfile: string;
  categories: any = {};
  sections = []
  formInputData = {}
  userInfo: string;
  userToken: string;

  constructor(resourceService: ResourceService, formService: FormService, dataService: DataService, route: Router, activatedRoute: ActivatedRoute,
    userService: UserService, public cacheService: CacheService, public toasterService: ToasterService) {
    this.resourceService = resourceService;
    this.formService = formService;
    this.dataService = dataService;
    this.router = route;
    this.activatedRoute = activatedRoute
    this.userService = userService;
  }

  ngOnInit() {
    this.activatedRoute.params.subscribe((params) => {
      this.userId = params.userId;
      this.viewOwnerProfile = params.role;
    });
    this.getUserDetails();
    this.userToken = this.cacheService.get(appConfig.cacheServiceConfig.cacheVariables.UserToken);
    if (_.isEmpty(this.userToken )) {
      this.userToken = this.userService.getUserToken;
    }
    this.getFormTemplate();
  }

  getUserDetails() {
    let token = this.cacheService.get(appConfig.cacheServiceConfig.cacheVariables.UserToken);
    if (_.isEmpty(token)) {
      token = this.userService.getUserToken;
    }
    const requestData = {
      header: { Authorization: token },
      data: {
        id: "open-saber.registry.read",
        request: {
          Employee: {
            osid: this.userId
          },
        }
      },
      url: appConfig.URLS.READ,
    }
    this.dataService.post(requestData).subscribe(response => {
      this.formInputData = response.result.Employee;
      this.userInfo = JSON.stringify(response.result.Employee)
    }, (err => {
      console.log(err)
    }))
  }

  getFormTemplate() {
    var requestData = {};
    if (this.viewOwnerProfile === 'owner') {
      requestData = { url: appConfig.URLS.OWNER_FORM_TEMPLATE }
    } else {
      requestData = {
        url: appConfig.URLS.FORM_TEPLATE,
        header: { Authorization: this.userToken }
      }
    }
    this.dataService.get(requestData).subscribe(res => {
      if (res.responseCode === 'OK') {
        this.formFieldProperties = res.result.formTemplate.data.fields;
        this.categories = res.result.formTemplate.data.categories;
        this.showLoader = false;
        this.getCategory();
      }
    });
  }
  getCategory() {
    _.map(this.categories, (value, key) => {
      var filtered_people = _.filter(this.formFieldProperties, function (field) {
        return _.includes(value, field.code);
      });
      this.sections.push({ name: key, fields: filtered_people });
    });
  }

  /**
   * to validate requried fields
   */
  validate() {
    const userData = JSON.parse(this.userInfo);
    //get only updated fields
    const diffObj = Object.keys(userData).filter(i => userData[i] !== this.formData.formInputData[i]);
    const updatedFields = {}
    let emptyFields = [];
    if (diffObj.length > 0) {
      _.map(diffObj, (value) => {
        updatedFields[value] = this.formData.formInputData[value];
      });
      updatedFields['osid'] = this.userId;
    }
    if (Object.keys(updatedFields).length > 0) {
      _.map(this.formFieldProperties, field => {
        if (field.required) {
          if (!this.formData.formInputData[field.code]) {
            let findObj = _.find(this.formFieldProperties, { code: field.code });
            emptyFields.push(findObj.label);
          }
        }
      });
      if (emptyFields.length === 0) {
        this.updateInfo(updatedFields, diffObj);
      }
      else {
        this.toasterService.warning("Profile updation failed please provide required fields " + emptyFields.join(', '));
      }
    }
  }

  updateInfo(updatedFieldValues, diffObj) {
    const requestData = {
      data: {
        id: "open-saber.registry.update",
        request: {
          Employee: updatedFieldValues
        },
      },
      header: { Authorization: this.userToken },
      url: appConfig.URLS.UPDATE
    };
    this.dataService.post(requestData).subscribe(response => {
      if (response.params.status === "SUCCESSFUL") {
        this.toasterService.success(diffObj + " successfully updated");
        this.navigateToProfilePage();
      }
    }, err => {
      this.toasterService.error(this.resourceService.frmelmnts.msg.updateFailure);
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
