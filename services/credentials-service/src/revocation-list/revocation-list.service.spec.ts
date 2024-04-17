import { Test, TestingModule } from '@nestjs/testing';
import { RevocationListService } from './revocation-list.service';

describe('RevocationListService', () => {
  let service: RevocationListService;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [RevocationListService],
    }).compile();

    service = module.get<RevocationListService>(RevocationListService);
  });

  it('should be defined', () => {
    expect(service).toBeDefined();
  });
});
