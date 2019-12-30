import { TestBed } from '@angular/core/testing';

import { ToasterService } from './toaster.service';

describe('ToasterService', () => {
  beforeEach(() => TestBed.configureTestingModule({}));

  it('should be created', () => {
    const service: ToasterService = TestBed.get(ToasterService);
    expect(service).toBeTruthy();
  });
});
