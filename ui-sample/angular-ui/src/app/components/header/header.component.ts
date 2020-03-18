import { Component, OnInit } from '@angular/core';
import { Router, ActivatedRoute, NavigationEnd } from '@angular/router';
import { ResourceService } from '../../services/resource/resource.service'
import { UserService } from 'src/app/services/user/user.service';
import { PermissionService } from 'src/app/services/permission/permission.service';
import { KeycloakService } from 'keycloak-angular';
import { CacheService } from 'ng2-cache-service';
import { DataService } from 'src/app/services/data/data.service';
import appConfig from '../../services/app.config.json';
import { ToasterService } from 'src/app/services/toaster/toaster.service';
import _ from 'lodash-es'



declare var jQuery: any;


@Component({
  selector: 'app-header',
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.scss']
})
export class HeaderComponent implements OnInit {
  userLogin: any;
  resourceService: ResourceService;
  avtarMobileStyle = {
    backgroundColor: 'transparent',
    color: '#AAAAAA',
    fontFamily: 'inherit',
    fontSize: '17px',
    lineHeight: '38px',
    border: '1px solid #e8e8e8',
    borderRadius: '50%',
    height: '38px',
    width: '38px'
  };
  avtarDesktopStyle = {
    backgroundColor: 'transparent',
    color: '#AAAAAA',
    fontFamily: 'inherit',
    fontSize: '17px',
    lineHeight: '38px',
    border: '1px solid #e8e8e8',
    borderRadius: '50%',
    height: '38px',
    width: '38px'
  };
  public userService: UserService;
  public userName: any;
  public permissionService: PermissionService;
  userDirectoryViewRole: Array<string>;
  onBoardEmployee: Array<string>;
  public keycloakAngular: KeycloakService;
  public dataService: DataService;
  public keyCloakUserDetails: any;
  private userId: string;
  private userAuthenticated: any;
  private userData: any;
  constructor(public router: Router, public activatedRoute: ActivatedRoute, resourceService: ResourceService, userService: UserService
    , permissionService: PermissionService, keycloakAngular: KeycloakService, private cacheService: CacheService, private _cacheService: CacheService,
    dataService: DataService, public toasterService: ToasterService) {
    this.resourceService = resourceService;
    this.userService = userService;
    this.permissionService = permissionService;
    this.keycloakAngular = keycloakAngular;
    this.dataService = dataService;
  }

  ngOnInit() {
    this.userDirectoryViewRole = appConfig.rolesMapping.adminPageViewRole;
    this.onBoardEmployee = appConfig.rolesMapping.onboardEmployee;
    this.resourceService.getResource();
    this.userAuthenticated = this.cacheService.get(appConfig.cacheServiceConfig.cacheVariables.UserAuthenticated);
    if(this.cacheService.get(appConfig.cacheServiceConfig.cacheVariables.EmployeeDetails)) {
      this.userData = this.cacheService.get(appConfig.cacheServiceConfig.cacheVariables.EmployeeDetails)
    }
    if (this.userAuthenticated) {
      this.userLogin = this.userAuthenticated.status;
      this.userName = this.cacheService.get(appConfig.cacheServiceConfig.cacheVariables.UserKeyCloakData).given_name;
    } else {
      if (this.userService.loggedIn) {
        this.userLogin = this.userService.loggedIn;
        this.userName = this.userService.getUserName;
        this.cacheData()
      }
    }
  }
  showSideBar() {
    jQuery('.ui.sidebar').sidebar('setting', 'transition', 'overlay').sidebar('toggle');
  }

  logout() {
    this.keycloakAngular.logout();
    window.localStorage.clear();
    this.cacheService.removeAll();
  }
  logIn() {
    if (!this.userService.loggedIn) {
      let userDetails = this.keycloakAngular.login();
    } else {
      this.router.navigate(['profile', this.userId, 'owner'])
    }
  }

  cacheData() {
    let userDetails = this.keycloakAngular.getKeycloakInstance().tokenParsed;
    let userToken = this.keycloakAngular.getKeycloakInstance().token;
    this.cacheService.set(appConfig.cacheServiceConfig.cacheVariables.UserToken, userToken, { maxAge: appConfig.cacheServiceConfig.setTimeInMinutes * appConfig.cacheServiceConfig.setTimeInSeconds });
    this.cacheService.set(appConfig.cacheServiceConfig.cacheVariables.UserKeyCloakData, userDetails, { maxAge: appConfig.cacheServiceConfig.setTimeInMinutes * appConfig.cacheServiceConfig.setTimeInSeconds });
    this.cacheService.set(appConfig.cacheServiceConfig.cacheVariables.UserAuthenticated, { status: true }, { maxAge: appConfig.cacheServiceConfig.setTimeInMinutes * appConfig.cacheServiceConfig.setTimeInSeconds });
    if (this.userLogin) {
      // The subject is the keycloak user id
      this.readUserDetails(userDetails.sub, userDetails.realm_access.roles, userToken)
    }

  }
  navigateToAdminConsole() {
    const authroles = this.permissionService.getAdminAuthRoles()
    if (authroles) {
      return authroles.url;
    }
  }
  navigateToProfilePage() {
    this.userId = this.cacheService.get(appConfig.cacheServiceConfig.cacheVariables.EmployeeDetails).osid;
    this.router.navigate(['/profile', this.userId, 'owner'])
  }

  readUserDetails(userdata: String, roles, token) {
    const requestData = {
      header: { Authorization: token },
      data: {
        id: appConfig.API_ID.SEARCH,
        request: {
          entityType: ["Employee"],
          filters: {
            kcid: { eq: userdata }
          }
        }
      },
      url: appConfig.URLS.SEARCH,
    }
    this.dataService.post(requestData).subscribe(response => {
      this.cacheService.set(appConfig.cacheServiceConfig.cacheVariables.EmployeeDetails, response.result.Employee[0], { maxAge: appConfig.cacheServiceConfig.setTimeInMinutes * appConfig.cacheServiceConfig.setTimeInSeconds });
      this.userId = response.result.Employee[0].osid;
      this.userData = response.result.Employee[0];
      if(_.includes(roles, 'x-owner')) {
        this.router.navigate(['/profile', this.userId, 'x-owner'])
      } else {
        this.router.navigate(['/profile', this.userId, 'owner'])
      }
    }, (err => {
      this.toasterService.error(this.resourceService.frmelmnts.msg.errorMsg);
      console.log(err)
    }))
  }

  getAuditReport() {
    this.router.navigate(['/dashboard'])
  }

}
