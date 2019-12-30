import { Injectable } from '@angular/core';
import { Ng2IzitoastService } from 'ng2-izitoast';

@Injectable({
  providedIn: 'root'
})
export class ToasterService {

  constructor(public iziToast: Ng2IzitoastService) { 
    this.iziToast.settings({
      position: 'topCenter',
      titleSize: '18'
    });
  }

  success(message: string) {
    this.iziToast.success({
      title: message
    });
  }

  info(message: string) {
    this.iziToast.info({
      title: message
    });
  }

  error(message: string) {
    this.iziToast.error({
      title: message
    });
  }

  warning(message: string) {
    this.iziToast.warning({
      title: message
    });
  }

}
