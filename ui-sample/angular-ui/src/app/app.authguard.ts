import { Injectable } from '@angular/core';
import { CanActivate, Router, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { KeycloakService, KeycloakAuthGuard } from 'keycloak-angular';
import { getLocaleExtraDayPeriodRules } from '../../node_modules/@angular/common';
import { calcBindingFlags } from '../../node_modules/@angular/core/src/view/util';

@Injectable()
export class AppAuthGuard implements CanActivate {
    granted: Boolean = undefined;

    constructor(protected router: Router, protected keycloakAngular: KeycloakService) {
        //super(router, keycloakAngular);
        this.granted = undefined;
        this.keycloakAngular = keycloakAngular;
    }

    canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Promise<boolean> {
        return new Promise(async (resolve, reject) => {
            var isLoggedIn = await this.keycloakAngular.isLoggedIn()
            if (!isLoggedIn) {
                this.keycloakAngular.login().then(() => {
                    console.log('isloggedin = ' + isLoggedIn)
                    console.log('role restriction given at app-routing.module for this route', route.data.roles);
                    this.checkRoles(route, resolve, reject) 
                }).catch(() => { reject() });
            } else {
                // Already computed granted. Rethrow it.
                console.log("is logged in already. Use existing granted")
                this.checkRoles(route, resolve, reject)
            }
        });
    }

    fetchUserRoles() {
        var roles: string[] = this.keycloakAngular.getUserRoles()
        console.log('User roles coming after login from keycloak :', roles);
        return roles;
    }

    checkRoles(route, resolve, reject) {
        if (this.granted === undefined) {
            this.granted = false; // set it, so that we marked it as found one time atleast.

            // Fetch roles only once.
            var roles: string[] = this.fetchUserRoles() 
            const requiredRoles = route.data.roles;

            if (!requiredRoles || requiredRoles.length === 0) {
                console.log("No roles mentioned. Allowing access")
                this.granted = true;
            } else {
                for (const requiredRole of requiredRoles) {
                    if (roles.indexOf(requiredRole) > -1) {
                        this.granted = true;
                        break;
                    }
                }
            }
        }

        if (this.granted === false) {
            console.log("Not granted due to required roles missing")
            reject(new Error("Access denied. Required roles are not set."))
        } else {
            resolve()
        }
    }
}