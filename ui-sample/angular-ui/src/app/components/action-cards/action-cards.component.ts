import { Component, OnInit, Input, EventEmitter, Output, ViewChild } from '@angular/core';
import { ICard } from '../../services/interfaces/Card';
import { ResourceService } from '../../services/resource/resource.service';
import appConfig from '../../services/app.config.json';
import { PermissionService } from 'src/app/services/permission/permission.service';
import { SuiModalService, TemplateModalConfig, ModalTemplate } from 'ng2-semantic-ui';
import { Router } from '@angular/router';
import { DataService } from 'src/app/services/data/data.service';
import { ToasterService } from 'src/app/services/toaster/toaster.service';
import { CacheService } from 'ng2-cache-service';
import { UserService } from '../../services/user/user.service';
import _ from 'lodash';

@Component({
  selector: 'app-action-cards',
  templateUrl: './action-cards.component.html',
  styleUrls: ['./action-cards.component.scss']
})
export class ActionCardsComponent implements OnInit {

  @ViewChild('modalTemplate')
  public modalTemplate: ModalTemplate<{ data: string }, string, string>;
  @Input() data: any;
  @Output() clickEvent = new EventEmitter<any>();
  resourceService: ResourceService;
  backgroundColor = this.getbackgroundColor();
  color = this.getRandColor();
  public permissionService: PermissionService;
  public approveEmployee: Array<string>;
  public enableViewProfile = true;
  router: Router;
  public dataService: DataService;
  userService: UserService;
  modalHeader = ""

  constructor(resourceService: ResourceService, permissionService: PermissionService, public modalService: SuiModalService, route: Router,
    dataService: DataService, public toasterService: ToasterService, userService: UserService, public cacheService: CacheService) {
    this.resourceService = resourceService;
    this.permissionService = permissionService;
    this.router = route;
    this.dataService = dataService;
    this.userService = userService;
  }

  ngOnInit() {
    this.approveEmployee = appConfig.rolesMapping.approveEmployee;
    if (this.permissionService.checkRolesPermissions(this.approveEmployee)) {
      this.enableViewProfile = false;
    }
  }

  getbackgroundColor() {
    let colors = ["#ccf0f3", "#DEF1E1", "#e1e9ee"]
    let randNum = Math.floor(Math.random() * 3);
    return colors[randNum];
  }


  getRandColor() {
    let colors = ["#DD4132", "#727289", "#642F7A", "#A34B25", "#872C6F", "#A34B25", "#8FB339", "#157A7F", "#51504E", "#334A66", "#F7786B", "#CE3175", "#5B5EA6", "#B565A7", "#66B7B0"]
    let randNum = Math.floor(Math.random() * 15);
    return colors[randNum];
  }

  public onAction(data, event) {
    this.clickEvent.emit({ 'action': event, 'data': data });
  }
  
  validateConfirmModal(userId) {
    const config = new TemplateModalConfig<{ data: string }, string, string>(this.modalTemplate);
    config.isClosable = true;
    config.size = 'mini';
    config.context = {
      data: 'Do you want to validate before viewing the profile?'
    };
    this.modalService
      .open(config)
      .onApprove(result => {
        this.validate(userId);
      })
      .onDeny(result => {
        if (result === 'view') {
          this.router.navigate(['/profile', userId]);
        }
      });
  }

  validate(userId) {
    let token = this.cacheService.get(appConfig.cacheServiceConfig.cacheVariables.UserToken);
    if (_.isEmpty(token)) {
      token = this.userService.getUserToken;
    }
    const requestData = {
      header: { Authorization: token },
      data: {
        id: appConfig.API_ID.UPDATE,
        request: {
          Employee: {
            osid: userId,
            isOnboarded: true,
            isActive: true
          }
        }
      },
      url: appConfig.URLS.UPDATE
    };
    this.dataService.post(requestData).subscribe(response => {
      if (response.params.status === "SUCCESSFUL") {
        this.clickEvent.emit({'action': event, 'data': {status: response.params.status}});
        this.toasterService.success(this.data.name + " " + this.resourceService.frmelmnts.msg.OnboardedSuccess);
        this.router.navigate(['/actions']);
      }
    }, err => {
    });
  }
}
