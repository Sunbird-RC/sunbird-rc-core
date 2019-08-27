import { Component, OnInit } from '@angular/core';
import { DataService } from '../../services/data/data.service';
import { ResourceService } from '../../services/resource/resource.service';
import { ActivatedRoute, Router } from '@angular/router'
import urlConfig from '../../services/urlConfig.json';
import { DomSanitizer } from '@angular/platform-browser'

@Component({
  selector: 'app-profile',
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss']
})
export class ProfileComponent implements OnInit {

  dataService: DataService;
  resourceService: ResourceService;
  router: Router;
  activatedRoute: ActivatedRoute;
  userId: String;
  userProfile: any;
  downloadJsonHref: any;
  constructor(dataService: DataService, resourceService: ResourceService, activatedRoute: ActivatedRoute, private sanitizer: DomSanitizer, router: Router) {
    this.dataService = dataService
    this.resourceService = resourceService;
    this.router = router
    this.activatedRoute = activatedRoute;
  }

  ngOnInit() {
    this.activatedRoute.params.subscribe((params) => {
      this.userId = params.id;
    });
    if (!this.userProfile) {
      this.userProfile = {}
    }
    this.getUserDetails();
  }

  getUserDetails() {
    const requestData = {
      data: {
        "id": "open-saber.registry.read",
        'request': {
          "Person": {
            "osid": this.userId
          },
          "includeSignatures": true,
          "viewTemplateId": "Person_Default.json"
        }
      },
      url: urlConfig.URLS.READ,
    }
    this.dataService.post(requestData).subscribe(response => {
      console.log(response);
      this.userProfile = response.result.Person;
    })
  }
  dowloadJson() {
    var theJSON = JSON.stringify(this.userProfile);
    var uri = this.sanitizer.bypassSecurityTrustUrl("data:text/json;charset=UTF-8," + encodeURIComponent(theJSON));
    this.downloadJsonHref = uri;
  }
  navigateToEditPage()  {
    console.log(this.userId, )
    this.router.navigate(['/edit', this.userId])
  }
}

