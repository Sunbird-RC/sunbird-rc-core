import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { UserService } from '../user/user.service';
import appConfig from '../app.config.json';
import * as _ from 'lodash-es';
import { CacheService } from 'ng2-cache-service'

@Injectable({
  providedIn: 'root'
})
export class PermissionService {

  /**
   * main roles with action
   */
  private mainRoles: Array<string> = [];
  /**
   * all user role
   */
  private userRoles: Array<string> = [];
  /**
   * flag to store permission availability
   */
  permissionAvailable = false;

  public permissionAvailable$ = new BehaviorSubject<string>(undefined);

  /**
   * reference of UserService service.
   */
  public userService: UserService;

  public cacheService: CacheService;

  constructor(userService: UserService, cacheService: CacheService) {
    this.userService = userService;
    this.cacheService = cacheService;
  }
  public initialize() {
    this.getPermissionsData();
  }
  /**
   * method to fetch roles.
   */
  private getPermissionsData(): void {
    this.setCurrentRoleActions();
  }
  /**
   * method to process roles 
   * @param {Array<Roles>} data 
   */
  private setRolesAndPermissions(roles: Array<string>): void {
    _.forEach(roles, (role) => {
      this.mainRoles.push(role);
    });
    this.setCurrentRoleActions();
  }
  /**
   * method to process logged in user roles
   * @param {ServerResponse} data 
   */
  private setCurrentRoleActions(): void {
    let userDetails = this.cacheService.get(appConfig.cacheServiceConfig.cacheVariables.UserKeyCloakData);
    if (userDetails) {
      this.userRoles = userDetails.realm_access.roles;
    } else {
      this.userRoles = this.userService.getUserRoles;
    }
    if (this.userRoles != undefined && this.userRoles.length > 0) {
      this.permissionAvailable$.next('success');
      this.permissionAvailable = true;
    }
    else {
      this.permissionAvailable$.next('error');
    }

  }
  /**
   * method to validate permission
   * @param {Array<string>}  roles roles to validate.
   */
  public checkRolesPermissions(roles: Array<string>): boolean {
    if ((_.intersection(roles, this.userRoles).length)) {
      return true;
    }
    return false;
  }

  get allRoles(): Array<string> {
    return this.mainRoles;
  }
  getAdminAuthRoles() {
    let adminAuthRoles = [{
      roles: appConfig.rolesMapping['adminPageViewRole'],
      url: "search",
      tab: "User Directory"
    }];
    const authRoles = _.find(adminAuthRoles, (role, key) => {
      if (this.checkRolesPermissions(role.roles)) {
        return role;
      }
    });
    return authRoles;
  }
}
