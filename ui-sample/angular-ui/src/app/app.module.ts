import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';

import { AppRoutingModule } from './app-routing.module';
import { AppAuthGuard } from './app.authguard';
import { AppComponent } from './app.component';
import { SignupComponent } from './components/signup/signup.component';
import { HeaderComponent } from './components/header/header.component';
import { FooterComponent } from './components/footer/footer.component';
import { LandingPageComponent } from './components/landingpage/landingpage.component';
import { AvatarModule } from 'ngx-avatar';
import { HttpClientModule } from '@angular/common/http';
import { HttpModule } from '@angular/http'
import {
  SuiSelectModule, SuiModalModule, SuiAccordionModule, SuiPopupModule, SuiDropdownModule, SuiProgressModule,
  SuiRatingModule, SuiCollapseModule, SuiCheckboxModule
} from 'ng2-semantic-ui';
import { RouterModule } from '@angular/router';
import { DefaultTemplateComponent } from './components/default-template/default-template.component';
import { FormsModule, ReactiveFormsModule, } from '@angular/forms';
import { AdminPageComponent } from './components/admin-page/admin-page.component';
import { ProfileComponent } from './components/profile/profile.component';
import { LoginComponent } from './components/login/login.component';
import { CreateComponent } from './components/create/create.component';
import { UpdateComponent } from './components/update/update.component';
import { environment } from '../environments/environment';
import { ProvidersFeature } from '../../node_modules/@angular/core/src/render3';
import { KeycloakService, KeycloakAngularModule, KeycloakOptions } from 'keycloak-angular';
import { PermissionDirective } from './directives/permission/permission.directive';
import { CacheService } from 'ng2-cache-service';
import { CacheStorageAbstract } from 'ng2-cache-service/dist/src/services/storage/cache-storage-abstract.service';
import { CacheSessionStorage } from 'ng2-cache-service/dist/src/services/storage/session-storage/cache-session-storage.service';

let moduleOptions = {
  declarations: [
    AppComponent,
    SignupComponent,
    HeaderComponent,
    FooterComponent,
    LandingPageComponent,
    DefaultTemplateComponent,
    AdminPageComponent,
    ProfileComponent,
    LoginComponent,
    CreateComponent,
    UpdateComponent,
    PermissionDirective],
  imports: [
    BrowserModule,
    AppRoutingModule,
    AvatarModule,
    HttpClientModule,
    HttpModule,
    SuiSelectModule, SuiModalModule, SuiAccordionModule, SuiPopupModule, SuiDropdownModule, SuiProgressModule,
    SuiRatingModule, SuiCollapseModule, SuiCheckboxModule,
    RouterModule,
    FormsModule,
    ReactiveFormsModule,
    KeycloakAngularModule
  ],
  providers: [AppAuthGuard,
    CacheService,
    { provide: CacheStorageAbstract, useClass: CacheSessionStorage }],
  bootstrap: [],
  entryComponents: [AppComponent]
}

let kcOptions: KeycloakOptions = {
  config: environment.keycloakConfig,
  loadUserProfileAtStartUp: true,
  bearerExcludedUrls: ['/', '/assets', '/clients/public']
};

@NgModule(moduleOptions)

export class AppModule {
  constructor(private keycloakService: KeycloakService) {
    this.keycloakService = keycloakService
  }

  ngDoBootstrap(app) {
    this.keycloakService
      .init(kcOptions)
      .then(() => {
        console.log('KC inited.');
      })
      .catch(error => {
        console.error('KC init failed', error);
      })
      .finally(() => {
        app.bootstrap(AppComponent);
      });
  }
}
