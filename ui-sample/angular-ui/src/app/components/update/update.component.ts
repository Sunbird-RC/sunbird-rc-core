import { Component, OnInit, ViewChild } from '@angular/core';
import { ResourceService } from '../../services/resource/resource.service'
import { FormService } from '../../services/forms/form.service'
import { from } from 'rxjs';
import { DefaultTemplateComponent } from '../default-template/default-template.component';
import { DataService } from 'src/app/services/data/data.service';
import { Router, ActivatedRoute } from '@angular/router';
import urlConfig from '../../services/urlConfig.json';

@Component({
  selector: 'app-update',
  templateUrl: './update.component.html',
  styleUrls: ['./update.component.scss']
})
export class UpdateComponent implements OnInit {
  @ViewChild('formData') formData: DefaultTemplateComponent;

  resourceService: ResourceService;
  formService: FormService;
  public formFieldProperties: any;
  dataService: DataService;
  router: Router;
  userId: String;
  activatedRoute: ActivatedRoute;

  constructor(resourceService: ResourceService, formService: FormService, dataService: DataService, route: Router, activatedRoute: ActivatedRoute) {
    this.resourceService = resourceService;
    this.formService = formService;
    this.dataService = dataService;
    this.router = route;
    this.activatedRoute = activatedRoute
  }

  ngOnInit() {
    this.userId = this.activatedRoute.snapshot.params.id;
    this.formFieldProperties = this.formService.getPersonForm().fields;
  }

  updateInfo() {
    this.formData.formInputData['osid'] = this.userId; 
    const requestData = {
      data: {
        "id": "open-saber.registry.update",
        "request": {
          "Person": this.formData.formInputData
        }
      },
      url: urlConfig.URLS.UPDATE
    };
    this.dataService.post(requestData).subscribe(response => {
      console.log(response)
      this.navigateToProfilePage();
    }, err => {
      // this.toasterService.error(this.resourceService.messages.fmsg.m0078);
    });
  }

  navigateToProfilePage() {
    this.router.navigate(['/profile', this.userId]);
  }
}
