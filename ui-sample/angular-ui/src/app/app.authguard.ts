import { Injectable } from '@angular/core';
import { CanActivate, Router, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { KeycloakService, KeycloakAuthGuard } from 'keycloak-angular';

@Injectable()
export class AppAuthGuard implements CanActivate {
    granted: boolean = false;

    constructor(protected router: Router, protected keycloakAngular: KeycloakService) {
        //super(router, keycloakAngular);
        this.keycloakAngular = keycloakAngular;
    }

//     isAccessAllowed(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Promise<boolean> {
//         return new Promise(async (resolve, reject) => {
//             resolve(true)
//             var isLoggedIn = await this.keycloakAngular.isLoggedIn()
//             if (!isLoggedIn) {
//                 this.keycloakAngular.login().then (()=> {
//                     console.log('isloggedin = ')
//                     console.log('role restriction given at app-routing.module for this route', route.data.roles);
//                     var roles: string[] = this.keycloakAngular.getUserRoles()
//                     console.log('User roles coming after login from keycloak :', );
//                     const requiredRoles = route.data.roles;
                    
//                     if (!requiredRoles || requiredRoles.length === 0) {
//                         this.granted = true;
//                     } else {
//                         for (const requiredRole of requiredRoles) {
//                             if (roles.indexOf(requiredRole) > -1) {
//                                 this.granted = true;
//                                 break;
//                             }
//                         }
//                     }

//                     if (this.granted === false) {
//                         console.log("Not granted due to required roles =" + requiredRoles)
//                         this.router.navigateByUrl('/')
//                     }
//                     resolve()
//                 }).catch(()=>{reject()});
//             } else {
//                 // Already computed granted. Rethrow it.
//                 resolve()
//             }
//     });
//    }

   canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Promise<boolean> {
       return new Promise(async (resolve, reject) => {
         resolve(true)  
       });
   }
    
}