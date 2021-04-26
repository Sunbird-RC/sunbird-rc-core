import { TestBed } from '@angular/core/testing';

import { DownloadCSVFileService } from './download-csvfile.service';

describe('DownloadCSVFileService', () => {
  beforeEach(() => TestBed.configureTestingModule({}));

  it('should be created', () => {
    const service: DownloadCSVFileService = TestBed.get(DownloadCSVFileService);
    expect(service).toBeTruthy();
  });
});
