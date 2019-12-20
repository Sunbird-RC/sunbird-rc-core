import { Component, OnInit } from '@angular/core';
import { DataService } from '../../services/data/data.service';
import { ResourceService } from '../../services/resource/resource.service';
import { ActivatedRoute, Router } from '@angular/router'
import appConfig from '../../services/app.config.json'
import { DomSanitizer } from '@angular/platform-browser'
import { CacheService } from 'ng2-cache-service';
import { UserService } from '../../services/user/user.service';
import _ from 'lodash-es';
import { PermissionService } from 'src/app/services/permission/permission.service';

@Component({
  selector: 'app-profile',
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss']
})
export class ProfileComponent implements OnInit {

  dataService: DataService;
  resourceService: ResourceService;
  permissionService: PermissionService;
  router: Router;
  activatedRoute: ActivatedRoute;
  userId: String;
  userProfile: any = {};
  downloadJsonHref: any;
  userService: UserService;
  public formFieldProperties: any;
  public showLoader = true;
  public viewOwnerProfile: string;
  public editProfile: Array<string>;
  enable: boolean = false;
  constructor(dataService: DataService, resourceService: ResourceService, activatedRoute: ActivatedRoute, private sanitizer: DomSanitizer, router: Router, userService: UserService, public cacheService: CacheService
    , permissionService: PermissionService) {
    this.dataService = dataService
    this.resourceService = resourceService;
    this.router = router
    this.activatedRoute = activatedRoute;
    this.userService = userService;
    this.permissionService = permissionService;
  }

  ngOnInit() {
    this.editProfile = appConfig.rolesMapping.editProfileRole;
    _.pull(this.editProfile, 'owner')
    this.activatedRoute.params.subscribe((params) => {
      this.userId = params.userId;
      this.viewOwnerProfile = params.role
    });
    if(_.isEmpty(this.viewOwnerProfile) && this.viewOwnerProfile == undefined) {
        this.enable = true;
    } 
    this.getFormTemplate();
  }

  getFormTemplate() {
    var requestData = {}
    if (this.viewOwnerProfile === 'owner') {
      requestData = {
        url: appConfig.URLS.OWNER_FORM_TEMPLATE
      }
    } else {
      let token = this.cacheService.get(appConfig.cacheServiceConfig.cacheVariables.UserToken);
      if (_.isEmpty(token)) {
        token = this.userService.getUserToken;
      }
       requestData = {
        url: appConfig.URLS.FORM_TEPLATE,
        header: {
          Authorization: token
        }
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
        if (field.inputType === 'select')
          field['inputType'] = "text";
      }
    });
    this.showLoader = false;
  }
  
  navigateToEditPage() {
    if(this.viewOwnerProfile) {
      this.router.navigate(['/edit', this.userId, this.viewOwnerProfile]);
    } else {
      this.router.navigate(['/edit', this.userId]);
    }
  }

  navigateToHomePage() {
    this.router.navigate(['/search'])
  }
}

