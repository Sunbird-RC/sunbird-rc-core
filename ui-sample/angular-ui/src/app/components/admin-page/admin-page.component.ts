import { Component, OnInit, EventEmitter } from '@angular/core';
import { DataService } from '../../services/data/data.service'
import urlConfig from '../../services/urlConfig.json'
import * as _ from 'lodash-es';
import { ResourceService } from '../../services/resource/resource.service'
import { Router, ActivatedRoute } from '@angular/router'
import { ICard } from '../../services/interfaces/Card';
import { takeUntil, map, first, debounceTime, delay } from 'rxjs/operators';
import { combineLatest, Subject } from 'rxjs';




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
export class AdminPageComponent implements OnInit {

  dataService: DataService;
  public showLoader = true;
  resourceService: ResourceService;
  router: Router;
  activatedRoute: ActivatedRoute;
  public paginationDetails: IPagination;
  pageLimit: any
  public dataDrivenFilterEvent = new EventEmitter();
  private listOfEmployees: ICard[];
  public initFilters = false;
  public dataDrivenFilters: any = {};
  public queryParams: any;
  public unsubscribe$ = new Subject<void>();
  public key: string;

  constructor(dataService: DataService, resourceService: ResourceService, route: Router, activatedRoute: ActivatedRoute) {
    this.dataService = dataService;
    this.resourceService = resourceService;
    this.router = route;
    this.activatedRoute = activatedRoute;
    this.pageLimit = urlConfig.PAGE_LIMIT;
    this.paginationDetails = this.getPager(0, 1, urlConfig.PAGE_LIMIT);
  }

  ngOnInit() {
    this.initFilters = true;
    this.dataDrivenFilterEvent.pipe(first()).
      subscribe((filters: any) => {
        this.dataDrivenFilters = filters;
        this.fetchDataOnParamChange();
        // this.setNoResultMessage();
      });
    this.activatedRoute.queryParams.subscribe(queryParams => {
      this.queryParams = { ...queryParams };
      this.key = this.queryParams['key'];
    });
  }


  getDataForCard(data) {
    const list: Array<ICard> = [];
    _.forEach(data, (item, key) => {
      const card = this.processContent(item);
      list.push(card);
    });
    return <ICard[]>list;
  }


  processContent(data) {
    const content: any = {
      name: data.name,
      subProjectName: data.Team,
      role: data.Role,
      isApproved: data.isApproved,
      startDate: data.StartDate,
      identifier: data.identifier
    };
    return content;
  }

  navigateToProfilePage(user: any) {
    this.router.navigate(['/profile', user.data.identifier]);
  }



  onEnter(key) {
    this.key = key;
    this.queryParams = {};
    this.queryParams['key'] = this.key;
    if (this.key && this.key.length > 0) {
      this.queryParams['key'] = this.key;
    } else {
      delete this.queryParams['key'];
    }
    this.router.navigate(["admin/1"], {
      queryParams: this.queryParams
    });
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

  public getFilters(filters) {
    const defaultFilters = _.reduce(filters, (collector: any, element) => {
      return collector;
    }, {});
    this.dataDrivenFilterEvent.emit(defaultFilters);
  }

  private fetchDataOnParamChange() {
    combineLatest(this.activatedRoute.params, this.activatedRoute.queryParams)
      .pipe(debounceTime(5), // wait for both params and queryParams event to change
        delay(10),
        map(result => ({ params: { pageNumber: Number(result[0].pageNumber) }, queryParams: result[1] })),
        takeUntil(this.unsubscribe$)
      ).subscribe(({ params, queryParams }) => {
        this.showLoader = true;
        this.paginationDetails.currentPage = params.pageNumber;
        this.queryParams = { ...queryParams };
        this.listOfEmployees = [];
        this.fetchEmployees();
      });
  }

  private fetchEmployees() {
    const option = {
      url: urlConfig.URLS.SEARCH,
      data: {
        id: "open-saber.registry.search",
        request: {
          entityType: ["Employee"],
          filters: {
          },
          viewTemplateId: "Employee_SearchResult.json"
        }
      }
    }
    let filters = _.pickBy(this.queryParams, (value: Array<string> | string) => value && value.length);
    filters = _.omit(filters, ['key', 'sort_by', 'sortType', 'appliedFilters']);
    option.data.request.filters = this.getFilterObject(filters);
    this.dataService.post(option)
      .subscribe(data => {
        this.showLoader = false;
        this.listOfEmployees = this.getDataForCard(data.result.Employee);
      }, err => {
        this.showLoader = false;
        this.listOfEmployees = [];
      });
  }
  getFilterObject(filter) {
    let option = {}
    if (filter) {
      _.forEach(filter, (elem, key) => {
        let filterType = {}
        if(_.isArray(elem)) {
          filterType['or'] = elem;
        } else {
          filterType['contains'] = elem;
        }
        option[key] = filterType;
      });
    }
    //search by name
    if (this.queryParams.key) {
      let filterTypes = {}
      filterTypes["startsWith"] = this.queryParams.key;
      option["name"] = filterTypes
    }
    return option;
  }
}
