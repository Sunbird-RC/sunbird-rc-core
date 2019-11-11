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
import { AppAuthGuard } from './app.authguard';

var routes = [
  {
    path: '',
    component: LandingPageComponent
  },
  {
    path: 'signup',
    component: SignupComponent,
    canActivate: [AppAuthGuard],
    data: { roles: [] }
  },
  {
    path: 'admin/:pageNumber',
    component: AdminPageComponent,
    canActivate: [AppAuthGuard],
    data: { roles: 'adminRole' }
  },
  {
    path: 'profile/:id', component: ProfileComponent,
    canActivate: [AppAuthGuard],
    data: { roles: 'profileRole' }
  },
  {
    path: 'login', component: LoginComponent,
    canActivate: [AppAuthGuard],
    data: { roles: ['admin', 'partner-admin', 'owner'] }
  },
  {
    path: 'create', component: CreateComponent,
    canActivate: [AppAuthGuard],
    data: {roles:[]}
  },
  {
    path: 'edit/:id', component: UpdateComponent,
    canActivate: [AppAuthGuard],
    data: { roles: ['admin', 'partner-admin', 'owner'] }
  }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
