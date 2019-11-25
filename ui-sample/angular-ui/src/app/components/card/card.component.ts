import { Component, OnInit, Input, EventEmitter, Output } from '@angular/core';
import { ICard } from '../../services/interfaces/Card';
import { ResourceService } from '../../services/resource/resource.service';
import  appConfig  from '../../services/app.config.json';
import { PermissionService } from 'src/app/services/permission/permission.service';
 
@Component({
  selector: 'app-card',
  templateUrl: './card.component.html',
  styleUrls: ['./card.component.scss']
})
export class CardComponent implements OnInit {

  @Input() data: ICard;
  @Output() clickEvent = new EventEmitter<any>();
  resourceService: ResourceService;
  color =this.getRandColor();
  public permissionService: PermissionService;
  public approveEmployee: Array<string>;
  public enableViewProfile = true;

  constructor(resourceService: ResourceService, permissionService: PermissionService) {
    this.resourceService = resourceService;
    this.permissionService = permissionService;
   }

  ngOnInit() {
    this.approveEmployee = appConfig.rolesMapping.approveEmployee;
    if(this.permissionService.checkRolesPermissions(this.approveEmployee)) {
      this.enableViewProfile = false;
    }
  }

  getRandColor() {
    let colors = ["#DD4132", "#727289", "#642F7A", "#A34B25", "#872C6F", "#A34B25", "#8FB339", "#157A7F", "#51504E", "#334A66", "#F7786B","#CE3175","#5B5EA6","#B565A7","#66B7B0"]
    let randNum = Math.floor(Math.random() * 15);
    return colors[randNum];
  }
  public onAction(data, event) {
    this.clickEvent.emit({ 'action': event, 'data': data });
  }
}
