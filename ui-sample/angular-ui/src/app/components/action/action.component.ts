import { Component, OnInit } from '@angular/core';
import { DataService } from 'src/app/services/data/data.service';
import { ResourceService } from 'src/app/services/resource/resource.service';
import { PermissionService } from 'src/app/services/permission/permission.service';
import { Router, ActivatedRoute } from '@angular/router';
import { ToasterService } from 'src/app/services/toaster/toaster.service';
import { UserService } from 'src/app/services/user/user.service';
import { CacheService } from 'ng2-cache-service';
import appConfig from '../../services/app.config.json'
import _ from 'lodash-es';

@Component({
  selector: 'app-action',
  templateUrl: './action.component.html',
  styleUrls: ['./action.component.scss']
})
export class ActionComponent implements OnInit {
  public dataService: DataService;
  resourceService: ResourceService;
  permissionService: PermissionService;
  router: Router;
  activatedRoute: ActivatedRoute;
  userService: UserService;
  public listOfEmployees = [];
  public showLoader = true;
  userDetail: any;

  constructor(dataService: DataService, resourceService: ResourceService, activatedRoute: ActivatedRoute, router: Router, userService: UserService, public cacheService: CacheService
    , permissionService: PermissionService, public toasterService: ToasterService) {
    this.dataService = dataService;
    this.dataService = dataService
    this.resourceService = resourceService;
    this.router = router
    this.activatedRoute = activatedRoute;
    this.userService = userService;
    this.permissionService = permissionService;
  }

  ngOnInit() {
    let userCacheData = this.cacheService.get(appConfig.cacheServiceConfig.cacheVariables.EmployeeDetails);
    this.getUserDetails(userCacheData.osid);
  }

  getManagerActions() {
    this.showLoader = true
    let token = this.cacheService.get(appConfig.cacheServiceConfig.cacheVariables.UserToken);
    if (_.isEmpty(token)) {
      token = this.userService.getUserToken;
    }
    const requestData = {
      header: { Authorization: token },
      data: {
        id: appConfig.API_ID.SEARCH,
        request: {
          entityType: ["Employee"],
          filters: {
            manager: {
              eq: this.userDetail.email
            },
            isActive: {
              eq: false
            },
            isOnboarded: {
              eq: false
            }
          }
        }
      },
      url: appConfig.URLS.SEARCH,
    }
    this.dataService.post(requestData).subscribe(response => {
      if (response.result.Employee && response.result.Employee.length > 0) {
        this.showLoader = false;
        this.listOfEmployees = response.result.Employee;
      } else {
        this.showLoader = false;
        this.listOfEmployees = [];
      }
    }, (err => {
      console.log(err)
    }))
  }

  /**
   * 
   * @param userId to get email id of the logged in user
   */
  getUserDetails(userId) {
    this.showLoader = true
    let token = this.cacheService.get(appConfig.cacheServiceConfig.cacheVariables.UserToken);
    if (_.isEmpty(token)) {
      token = this.userService.getUserToken;
    }
    const requestData = {
      header: { Authorization: token },
      data: {
        id: appConfig.API_ID.READ,
        request: {
          Employee: {
            osid: userId
          },
          viewTemplateId:"6245495a-4745-11ea-b77f-2e728ce88125.json",
          includeSignatures: true
        }
      },
      url: appConfig.URLS.READ,
    }
    this.dataService.post(requestData).subscribe(response => {
      this.userDetail = response.result.Employee;
      this.getManagerActions();
    }, (err => {
      console.log(err)
    }))
  }

  navigateToProfilePage(user: any) {
    if (user.data.status === "SUCCESSFUL") {
      this.getManagerActions();
    } else {
      this.router.navigate(['/profile', user.data.osid]);
    }
  }

}
