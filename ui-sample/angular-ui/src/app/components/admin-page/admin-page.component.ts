import { Component, OnInit, EventEmitter, OnDestroy } from '@angular/core';
import { DataService } from '../../services/data/data.service'
import * as _ from 'lodash-es';
import { ResourceService } from '../../services/resource/resource.service'
import { Router, ActivatedRoute } from '@angular/router'
import { ICard } from '../../services/interfaces/Card';
import { takeUntil, map, first, debounceTime, delay } from 'rxjs/operators';
import { combineLatest, Subject } from 'rxjs';
import appConfig from '../../services/app.config.json';
import { UserService } from 'src/app/services/user/user.service';
import { CacheService } from 'ng2-cache-service';



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
export class AdminPageComponent implements OnInit, OnDestroy {

  dataService: DataService;
  public showLoader = true;
  resourceService: ResourceService;
  router: Router;
  activatedRoute: ActivatedRoute;
  public paginationDetails: IPagination;
  pageLimit: any
  public dataDrivenFilterEvent = new EventEmitter();
  public listOfEmployees: ICard[] = [];
  public initFilters = false;
  public dataDrivenFilters: any = {};
  public queryParams: any;
  public unsubscribe$ = new Subject<void>();
  public key: string;
  public buttonIcon: string = 'list';
  public buttonText: string = 'list view'
  result: { "headers": string; "row": {}; };
  totalItems: any;
  userService: UserService;

  constructor(dataService: DataService, resourceService: ResourceService, route: Router, activatedRoute: ActivatedRoute,  userService: UserService, public cacheService: CacheService) {
    this.dataService = dataService;
    this.userService = userService;
    this.resourceService = resourceService;
    this.router = route;
    this.activatedRoute = activatedRoute;
    this.pageLimit = appConfig.PAGE_LIMIT
    this.paginationDetails = this.getPager(0, 1, appConfig.PAGE_LIMIT);
  }

  ngOnInit() {
    this.getTotalItems();
    this.result = {
      "headers": '',
      "row": ''
    }
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

  getTotalItems() {
    const option = {
      url: appConfig.URLS.SEARCH,
      data: {
        id: "open-saber.registry.search",
        request: {
          entityType: ["Employee"],
          filters: {}
        }
      }
    }
    this.dataService.post(option).subscribe(data => {
      if (data.result.Employee) {
        this.totalItems = data.result.Employee.length;
      }
    })
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
      subProjectName: data.subProjectName,
      role: data.Role,
      isActive: data.isApproved,
      startDate: data.StartDate,
      identifier: data.identifier
    };
    return content;
  }

  navigateToProfilePage(user: any) {
    this.router.navigate(['/profile', user.data.identifier]);
  }

  changeView() {
    if (this.buttonIcon === 'list') {
      this.buttonIcon = 'block layout';
      this.buttonText = 'grid view'
    } else {
      this.buttonIcon = 'list'
      this.buttonText = 'list view'
    }
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
    this.router.navigate(["search/1"], {
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
    let token = this.cacheService.get(appConfig.cacheServiceConfig.cacheVariables.UserToken);
    if (_.isEmpty(token)) {
      token = this.userService.getUserToken;
    }
    const option = {
      url: appConfig.URLS.SEARCH,
      header: { Authorization: token },
      data: {
        id: "open-saber.registry.search",
        request: {
          entityType: ["Employee"],
          filters: {
          },
          limit: this.pageLimit,
          viewTemplateId: "Employee_SearchResult.json"
        }
      }
    }
    let filters = _.pickBy(this.queryParams, (value: Array<string> | string) => value && value.length);
    filters = _.omit(filters, ['key', 'sort_by', 'sortType', 'appliedFilters']);
    option.data.request.filters = this.getFilterObject(filters);
    option.data.request['offset'] = (this.paginationDetails.currentPage - 1) * this.pageLimit
    this.dataService.post(option)
      .subscribe(data => {
        this.showLoader = false;
        this.paginationDetails = this.getPager(this.totalItems, this.paginationDetails.currentPage, appConfig.PAGE_LIMIT);
        this.listOfEmployees = this.getDataForCard(data.result.Employee);
        this.result = {
          "headers": _.keys(this.listOfEmployees[0]),
          "row": this.listOfEmployees
        }
      }, err => {
        this.showLoader = false;
        this.listOfEmployees = [];
        this.paginationDetails = this.getPager(0, this.paginationDetails.currentPage, appConfig.PAGE_LIMIT);
      });
  }
  getFilterObject(filter) {
    let option = {}
    if (filter) {
      _.forEach(filter, (elem, key) => {
        let filterType = {}
        if (_.isArray(elem)) {
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

  clearQuery() {
    let redirectUrl = this.router.url.split('?')[0];
    redirectUrl = decodeURI(redirectUrl);
    this.router.navigate([redirectUrl]);
  }

  ngOnDestroy() {
    this.unsubscribe$.next();
    this.unsubscribe$.complete();
  }
}
