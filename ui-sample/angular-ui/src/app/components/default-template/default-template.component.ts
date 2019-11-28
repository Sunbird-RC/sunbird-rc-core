import { Component, Input, OnInit, AfterViewInit, Output, EventEmitter } from '@angular/core';
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
  public formInputData = {};
  router: Router;
  activatedRoute: ActivatedRoute;
  userId: String;
  dataService: DataService;
  userService: UserService;
  constructor(activatedRoute: ActivatedRoute, dataService: DataService, public cacheService: CacheService, userService: UserService) {
    this.activatedRoute = activatedRoute;
    this.dataService = dataService;
    this.userService = userService;
  }

  ngOnInit() {
    this.activatedRoute.params.subscribe((params) => {
      this.userId = params.userId;
    });
    if (this.userId) {
      this.getUserDetails();
    }
  }

  getUserDetails() {
    let token = this.cacheService.get(appConfig.cacheServiceConfig.cacheVariables.UserToken);
    if (_.isEmpty(token)) {
      token = this.userService.getUserToken;
    }
    const requestData = {
      header: { Authorization: token },
      data: {
        "id": "open-saber.registry.read",
        'request': {
          "Employee": {
            "osid": this.userId
          },
          "includeSignatures": true,
        }
      },
      url: appConfig.URLS.READ,
    }
    this.dataService.post(requestData).subscribe(response => {
      this.formInputData = response.result.Employee;
    }, (err => {
      console.log(err)
    }))
  }

}