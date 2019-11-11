import { Component, OnInit } from '@angular/core';
import { PermissionService } from './services/permission/permission.service';
import { UserService } from './services/user/user.service';
import { KeycloakService } from 'keycloak-angular';
import { CacheService } from 'ng2-cache-service';
import appConfig from './services/app.config.json';




@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit {
  title = 'open-saber-ui';
  public keycloakAngular: KeycloakService;
  public cacheService: CacheService;
  private isUserLoggedIn: boolean;

  constructor(private permissionService: PermissionService, public userService: UserService, keycloakAngular: KeycloakService,
    cacheService: CacheService) {
    this.keycloakAngular = keycloakAngular;
    this.cacheService = cacheService;
  }

  ngOnInit() {
    let authenticated = this.cacheService.get(appConfig.cacheServiceConfig.cacheVariables.UserAuthenticated);
    if (authenticated) {
      this.isUserLoggedIn = authenticated.status;
    } else {
      this.isUserLoggedIn = this.userService.loggedIn;
    }
    if (this.isUserLoggedIn) {
      this.permissionService.initialize();
    }
  }

}


