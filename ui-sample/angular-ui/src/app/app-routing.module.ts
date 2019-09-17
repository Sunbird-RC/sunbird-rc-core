import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { SignupComponent } from './components/signup/signup.component';
import { LandingPageComponent } from './components/landingpage/landingpage.component';
import { AdminPageComponent } from './components/admin-page/admin-page.component';
import { ProfileComponent } from './components/profile/profile.component';
import { LoginComponent } from './components/login/login.component';
import { CreateComponent } from './components/create/create.component';
import { UpdateComponent } from './components/update/update.component';
import { environment } from '../environments/environment';

var routes = [];
if (environment.keycloakEnabled) {
  routes = [
    {
      path: '',
      component: LandingPageComponent,
    },
    {
      path: 'signup', component: SignupComponent,
    },
    {
      path: 'admin/:pageNumber', component: AdminPageComponent
    },
    {
      path: 'profile/:id', component: ProfileComponent
    },
    {
      path: 'login', component: LoginComponent
    },
    {
      path: 'edit/:id', component: UpdateComponent
    }
  ];
} else {
  routes = [
    {
      path: '',
      component: LandingPageComponent,
    },
    {
      path: 'signup', component: SignupComponent,
    },
    {
      path: 'admin/:pageNumber', component: AdminPageComponent
    },
    {
      path: 'profile/:id', component: ProfileComponent
    },
    {
      path: 'login', component: LoginComponent
    },
    {
      path: 'edit/:id', component: UpdateComponent
    }
  ];
}

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
