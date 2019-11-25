import { Component, OnInit } from '@angular/core';
import { DataService } from '../../services/data/data.service';
import { ResourceService } from '../../services/resource/resource.service';
import { ActivatedRoute, Router } from '@angular/router'
import appConfig from '../../services/app.config.json'
import { DomSanitizer } from '@angular/platform-browser'
import { CacheService } from 'ng2-cache-service';
import { UserService } from '../../services/user/user.service';
import _ from 'lodash-es';

@Component({
  selector: 'app-profile',
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss']
})
export class ProfileComponent implements OnInit {

  dataService: DataService;
  resourceService: ResourceService;
  router: Router;
  activatedRoute: ActivatedRoute;
  userId: String;
  userProfile: any = {};
  downloadJsonHref: any;
  userService: UserService;
  public formFieldProperties: any;
  public showLoader = true;
  public viewOwnerProfile : string;

  constructor(dataService: DataService, resourceService: ResourceService, activatedRoute: ActivatedRoute, private sanitizer: DomSanitizer, router: Router, userService: UserService, public cacheService: CacheService) {
    this.dataService = dataService
    this.resourceService = resourceService;
    this.router = router
    this.activatedRoute = activatedRoute;
    this.userService = userService;
  }

  ngOnInit() {
    this.activatedRoute.params.subscribe((params) => {
      this.userId = params.userId;
      this.viewOwnerProfile = params.role
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
        Authorization: token,
        role: this.viewOwnerProfile
      }
    }
    this.dataService.get(requestData).subscribe(res => {
      if (res.responseCode === 'OK') {
        this.formFieldProperties = res.result.formTemplate.data.fields;
        this.disableEditMode()
      }
    });
  }

  disableEditMode() {
    _.map(this.formFieldProperties, field => {
      if (field.hasOwnProperty('editable')) {
        field['editable'] = false;
        field['required'] = false;
        field['inputType'] = "text";
      }
    });
    this.showLoader = false;
  }

  getUserDetails() {
    const requestData = {
      data: {
        "id": "open-saber.registry.read",
        'request': {
          "Employee": {
            "osid": this.userId
          },
          "includeSignatures": true,
          "viewTemplateId": "Employee_SearchResult.json",
        }
      },
      url: appConfig.URLS.READ,
    }
    this.dataService.post(requestData).subscribe(response => {
      console.log(response);
      this.userProfile = response.result.Employee;
    })
  }
  dowloadJson() {
    var theJSON = JSON.stringify(this.userProfile);
    var uri = this.sanitizer.bypassSecurityTrustUrl("data:text/json;charset=UTF-8," + encodeURIComponent(theJSON));
    this.downloadJsonHref = uri;
  }
  navigateToEditPage() {
    this.router.navigate(['/edit', this.userId], {queryParams: {
      role:this.viewOwnerProfile
    }});
  }
}

