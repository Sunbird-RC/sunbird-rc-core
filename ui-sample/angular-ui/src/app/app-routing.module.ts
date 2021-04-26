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
import { DashboardComponent } from './components/dashboard/dashboard.component';
import { ActionComponent } from './components/action/action.component';

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
    path: 'search',
    component: AdminPageComponent,
    canActivate: [AppAuthGuard],
    data: { roles: 'adminPageViewRole' }
  },
  {
    path: 'profile/:userId', component: ProfileComponent,
    canActivate: [AppAuthGuard],
    data: { roles: 'profilePageViewRole' }
  },
  {
    path: 'profile/:userId/:role', component: ProfileComponent,
    canActivate: [AppAuthGuard],
    data: { roles: 'profilePageViewRole' }
  },
  {
    path: 'login', component: LoginComponent,
    canActivate: [AppAuthGuard],
    data: { roles: ['admin', 'partner-admin', 'owner'] }
  },
  {
    path: 'create', component: CreateComponent,
    canActivate: [AppAuthGuard],
    data: {roles:'onboardEmployee'}
  },
  {
    path: 'edit/:userId', component: UpdateComponent,
    canActivate: [AppAuthGuard],
    data: { roles: 'editProfileRole' }
  },
  {
    path: 'edit/:userId/:role', component: UpdateComponent,
    canActivate: [AppAuthGuard],
    data: { roles: 'editOwnProfileRole' }
  },
  {
    path: 'dashboard/:userId', component: DashboardComponent,
    canActivate: [AppAuthGuard],
    data: { roles: 'dashboardView' }
  },
  {
    path: 'dashboard', component: DashboardComponent,
    canActivate: [AppAuthGuard],
    data: { roles: 'onboardEmployee' }
  },
  {
    path: 'actions', component: ActionComponent,
    canActivate: [AppAuthGuard],
    data: { roles: 'actionPageViewRole'}
  }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
