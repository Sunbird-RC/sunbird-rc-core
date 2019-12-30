import { Component, Input, OnInit } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router'
import { DataService } from '../../services/data/data.service';
import appConfig from '../../services/app.config.json';
import * as _ from 'lodash-es';
import { CacheService } from 'ng2-cache-service';
import { UserService } from 'src/app/services/user/user.service';


@Component({
  selector: 'app-default-template',
  templateUrl: './default-template.component.html',
  styleUrls: ['./default-template.component.scss']
})
export class DefaultTemplateComponent implements OnInit {
  @Input() formFieldProperties: any;
  @Input() formInputData: any ={};
  router: Router;
  activatedRoute: ActivatedRoute;
  userId: String;
  dataService: DataService;
  userService: UserService;
  userInfo: string;
  constructor(activatedRoute: ActivatedRoute, dataService: DataService, public cacheService: CacheService, userService: UserService) {
    this.activatedRoute = activatedRoute;
    this.dataService = dataService;
    this.userService = userService;
  }

  ngOnInit() {
    this.activatedRoute.params.subscribe((params) => {
      this.userId = params.userId;
    });
  }

}