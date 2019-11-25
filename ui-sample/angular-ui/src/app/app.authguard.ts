import { Injectable } from '@angular/core';
import { CanActivate, Router, ActivatedRouteSnapshot, RouterStateSnapshot, CanLoad } from '@angular/router';
import { KeycloakService, KeycloakAuthGuard } from 'keycloak-angular';
import { UserService } from './services/user/user.service';
import { Observable } from 'rxjs';
import { PermissionService } from './services/permission/permission.service';
import appConfig from './services/app.config.json';

@Injectable()
export class AppAuthGuard implements CanActivate, CanLoad {

    granted: boolean = false;

    public userService: UserService;
    public permissionService: PermissionService;

    constructor(protected router: Router, protected keycloakAngular: KeycloakService, userService: UserService
        , permissionService: PermissionService, ) {
        //super(router, keycloakAngular);
        this.permissionService = permissionService;
        this.keycloakAngular = keycloakAngular;
        this.userService = userService;
    }

    canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean> {
        return this.getPermission(route.data.roles)
    }

    getPermission(roles) {
        return Observable.create(observer => {
            this.permissionService.permissionAvailable$.subscribe(
                permissionAvailable => {
                    if (permissionAvailable && permissionAvailable === 'success') {
                        if (roles && appConfig.rolesMapping[roles]) {
                            if (this.permissionService.checkRolesPermissions(appConfig.rolesMapping[roles])) {
                                observer.next(true);
                            } else {
                                this.navigateToHome(observer);
                            }
                        } else {
                            this.navigateToHome(observer);
                        }
                    } else if (permissionAvailable && permissionAvailable === 'error') {
                        this.navigateToHome(observer);
                    } else if(permissionAvailable === undefined) {
                        this.navigateToHome(observer);
                    }
                }
            );
        });
    }

    navigateToHome(observer) {
        this.router.navigate(['']);
        observer.next(false);
        observer.complete();
    }

    canLoad(): boolean {
        if (this.userService.loggedIn) {
            return true;
        } else {
            return false;
        }
    }

}