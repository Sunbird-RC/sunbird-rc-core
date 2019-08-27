import { Component, OnInit } from '@angular/core';
import {ResourceService} from '../../services/resource/resource.service'

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent implements OnInit {

  resourceService : ResourceService;
  constructor(resourceService: ResourceService) { 
    this.resourceService = resourceService;
    this.resourceService.getResource();
  }

  ngOnInit() {

    console.log()
  }

}
