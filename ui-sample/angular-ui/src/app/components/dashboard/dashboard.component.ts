import { Component, OnInit } from '@angular/core';
import appConfig from '../../services/app.config.json';
import { ActivatedRoute } from '@angular/router';
import { DataService } from 'src/app/services/data/data.service.js';
import { CacheService } from 'ng2-cache-service';
import _ from 'lodash-es'
import { UserService } from 'src/app/services/user/user.service.js';


@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit {

  userId: string;
  dataService: DataService
  userService: UserService
  result: { "headers": any; "row": any; };
  auditInfo = [];

  constructor(public activatedRoute: ActivatedRoute, dataService: DataService, public cacheService: CacheService, userService: UserService) {
    this.dataService = dataService;
  }

  ngOnInit() {
    this.activatedRoute.params.subscribe((params) => {
      this.userId = params.userId;
    });
    this.getAdditReport();
  }

  getAdditReport() {
    let token = this.cacheService.get(appConfig.cacheServiceConfig.cacheVariables.UserToken);
    if (_.isEmpty(token)) {
      token = this.userService.getUserToken;
    }
    const requestData = {
      header: { Authorization: token },
      data: {
        id: appConfig.API_ID.AUDIT,
        request: {
          Employee: {
            filters: {
            }
          }
        }
      },
      url: appConfig.URLS.AUDIT,
    }
    if (this.userId) {
      requestData.data.request.Employee.filters['recordId'] = {
        eq: this.userId.substring(this.userId.indexOf('-') + 1)
      }
    } else {
      requestData.data.request.Employee.filters["action"] = {
        neq: "AUDIT"
      }
    }
    this.dataService.post(requestData).subscribe(response => {
      this.auditInfo = response.result.Employee_Audit;
    }, (err => {
      console.log(err)
    }))
  }

}
