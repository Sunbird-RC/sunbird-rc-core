import { Component, OnInit, AfterViewInit, EventEmitter} from '@angular/core';
import { DataService } from '../../services/data/data.service'
import urlConfig from '../../services/urlConfig.json'
import * as _ from 'lodash-es';
import { ResourceService } from '../../services/resource/resource.service'
import { Router, ActivatedRoute } from '@angular/router'
import { takeUntil, map, mergeMap, first, filter, debounceTime } from 'rxjs/operators';



export interface IPagination {
  totalItems: number;
  currentPage: number;
  pageSize: number;
  totalPages: number;
  startPage: number;
  endPage: number;
  startIndex: number;
  endIndex: number;
  pages: Array<number>;
}

@Component({
  selector: 'app-admin-page',
  templateUrl: './admin-page.component.html',
  styleUrls: ['./admin-page.component.scss']
})
export class AdminPageComponent implements OnInit, AfterViewInit {

  dataService: DataService;
  resourceService: ResourceService;
  router: Router;
  users: Array<Object>;
  result: any;
  activatedRoute: ActivatedRoute;
  public paginationDetails: IPagination;
  pageLimit: any
  public dataDrivenFilterEvent = new EventEmitter();


  constructor(dataService: DataService, resourceService: ResourceService, route: Router, activatedRoute: ActivatedRoute) {
    this.dataService = dataService;
    this.resourceService = resourceService;
    this.router = route;
    this.activatedRoute = activatedRoute;
    console.log('resource service ', this.resourceService)
    this.pageLimit = urlConfig.PAGE_LIMIT;
    this.paginationDetails = this.getPager(0, 1, urlConfig.PAGE_LIMIT);
  }

  ngOnInit() {
    this.result = {
      "headers": '',
      "row": ''
    }
    // this.dataDrivenFilterEvent.pipe(first()).
    //         subscribe((filters: any) => {
    //            this.getUsers(null);
    //         });
    this.getUsers(null);
  }

  ngAfterViewInit() { }

  getUsers(key: string) {
    this.activatedRoute.params.subscribe((params) => {
      this.paginationDetails.currentPage = Number(params.pageNumber)
    });

    const option = {
      url: urlConfig.URLS.SEARCH,
      data: {
        "id": "open-saber.registry.search",
        'request': {
          "entityType": ["Person"],
          "filters": {
          },
          "viewTemplateId": "Person_SearchResult.json"
        }
      }
    }
    if (key !== null) {

      option.data.request.filters = {
        "name": { "contains": key }
      }
    }

    this.dataService.post(option).subscribe(data => {
      this.result = {
        "headers": _.keys(data.result.Person[0]),
        "row": data.result.Person.slice(0, this.pageLimit)
      }
      this.paginationDetails = this.getPager(data.result.Person.length, this.paginationDetails.currentPage,
        urlConfig.PAGE_LIMIT);
    })
  }

  navigateToProfilePage(user: any) {
    console.log('user profile', user)
    this.router.navigate(['/profile', user.osid]);
  }

  onEnter(key: string) {
    this.getUsers(key)
  }



  getPager(totalItems: number, currentPage: number = 1, pageSize: number = 10, pageStrip: number = 5) {
    const totalPages = Math.ceil(totalItems / pageSize);
    let startPage: number, endPage: number;
    if (totalPages <= pageStrip) {
      startPage = 1;
      endPage = totalPages;
    } else {
      // when pagination is on the first section
      if (currentPage <= 1) {
        startPage = 1;
        endPage = pageStrip;
        // when pagination is on the last section
      } else if (currentPage + (pageStrip - 1) >= totalPages) {
        startPage = totalPages - (pageStrip - 1);
        endPage = totalPages;
        // when pagination is not on the first/last section
      } else {
        startPage = currentPage;
        endPage = currentPage + (pageStrip - 1);
      }
    }
    // calculate start and end item indexes
    const startIndex = (currentPage - 1) * pageSize;
    const endIndex = Math.min(startIndex + pageSize - 1, totalItems - 1);

    // create an array of pages to *nFort in the pager control
    const pages = _.range(startPage, endPage + 1);

    // return object with all pager properties required by the view
    return {
      totalItems: totalItems,
      currentPage: currentPage,
      pageSize: pageSize,
      totalPages: totalPages,
      startPage: startPage,
      endPage: endPage,
      startIndex: startIndex,
      endIndex: endIndex,
      pages: pages
    };
  }

  navigateToPage(page: number): void {
    if (page < 1 || page > this.paginationDetails.totalPages) {
        return;
    }
    console.log(this.router.url)
    const url = this.router.url.split('?')[0].replace(/.$/, page.toString());
    this.router.navigate([url]);
}
  
}
