import { Test, TestingModule } from '@nestjs/testing';
import { VaultService } from '../utils/vault.service';
import { VaultHealthIndicator } from './vault.health';

describe('VaultHealthIndicator', () => {
  let service: VaultHealthIndicator;

  beforeEach(async () => {
    jest.restoreAllMocks();
    const module: TestingModule = await Test.createTestingModule({
      providers: [VaultService, VaultHealthIndicator],
    }).compile();
    service = module.get<VaultHealthIndicator>(VaultHealthIndicator);
  });

  it('should be defined', () => {
    expect(service).toBeDefined();
  });
  it('should be defined', () => {
    const result = service.isHealthy("did");
    expect(result).toBeDefined();
  });
});