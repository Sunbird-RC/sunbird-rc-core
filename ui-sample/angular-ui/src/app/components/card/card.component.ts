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
import _ from 'lodash-es';

@Component({
  selector: 'app-card',
  templateUrl: './card.component.html',
  styleUrls: ['./card.component.scss']
})
export class CardComponent implements OnInit {

  @ViewChild('modalTemplate')
  public modalTemplate: ModalTemplate<{ data: string }, string, string>;
  @Input() data: ICard;
  @Output() clickEvent = new EventEmitter<any>();
  resourceService: ResourceService;
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

  getRandColor() {
    let colors = ["#DD4132", "#727289", "#642F7A", "#A34B25", "#872C6F", "#A34B25", "#8FB339", "#157A7F", "#51504E", "#334A66", "#F7786B", "#CE3175", "#5B5EA6", "#B565A7", "#66B7B0"]
    let randNum = Math.floor(Math.random() * 15);
    return colors[randNum];
  }
  public onAction(data, event) {
    this.clickEvent.emit({ 'action': event, 'data': data });
  }
  approveConfirmModal(userId) {
    this.modalHeader = "Approve"
    const config = new TemplateModalConfig<{ data: string }, string, string>(this.modalTemplate);
    config.isClosable = true;
    config.size = 'mini';
    config.context = {
      data: 'Do you want to approve before viewing the profile?'
    };
    this.modalService
      .open(config)
      .onApprove(result => {
        this.approve(userId);
      })
      .onDeny(result => {
        if (result === 'view') {
          this.router.navigate(['/profile', userId]);
        }
      });
  }

  deBoardConfirmModal(userId) {
    this.modalHeader = "Deboard"
    const config = new TemplateModalConfig<{ data: string }, string, string>(this.modalTemplate);
    config.isClosable = true;
    config.size = 'mini';
    config.context = {
      data: 'Do you want to Deboard before viewing the profile?'
    };
    this.modalService
      .open(config)
      .onApprove(result => {
        this.offBoard(userId);
      })
      .onDeny(result => {
        if (result === 'view') {
          this.router.navigate(['/profile', userId]);
        }
      });
  }

  offBoard(userId) {
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
            osid: userId
          }
        }
      },
      url: "/offboard/user"
    };
    this.dataService.post(requestData).subscribe(response => {
      if (response.params.status === "SUCCESSFUL") {
        this.toasterService.success(this.data.name + " " + this.resourceService.frmelmnts.msg.deBoardSuccess);
        this.router.navigate(['/search']);
      }
    }, err => {
      this.toasterService.error(this.data.name + " " + this.resourceService.frmelmnts.msg.deBoardFailure);
    });
  }


  approve(userId) {
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
        this.toasterService.success(this.data.name + " " + this.resourceService.frmelmnts.msg.OnboardedSuccess);
        this.router.navigate(['/search']);
      }
    }, err => {
      this.toasterService.error(this.data.name + " " + this.resourceService.frmelmnts.msg.OnboardUnSuccess);
    });
  }
}
