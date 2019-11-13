import { Injectable } from '@angular/core';
import { KeycloakService } from 'keycloak-angular';


@Injectable({
  providedIn: 'root'
})
export class UserService {

  private _authenticated: boolean;

  public logIn: boolean;
  public keycloakAngular: KeycloakService;
  public userInfo: any = {};
  constructor(keycloakAngular: KeycloakService) {
    this.keycloakAngular = keycloakAngular;
    this._authenticated = this.keycloakAngular.getKeycloakInstance().authenticated;
  }


  /**
 * returns login status.
 */
  get loggedIn() : boolean{
    return this._authenticated;
  }

  async getUserInfo() {
    await this.keycloakAngular.loadUserProfile().then(userInfo => {
      this.userInfo = userInfo;
    })
    return this.userInfo;
  }

  get getUserRoles(): string[] {
    return this.keycloakAngular.getUserRoles();
  }

  get getUserName(): string {
    return this.keycloakAngular.getUsername()
  }

  get getUserToken(): string {
    return this.keycloakAngular.getKeycloakInstance().token;
  }
}
