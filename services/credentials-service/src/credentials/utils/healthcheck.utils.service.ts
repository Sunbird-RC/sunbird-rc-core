import { HttpService } from '@nestjs/axios';
import { Injectable } from '@nestjs/common';
import {
  HealthCheckError,
  HealthIndicator,
  HealthIndicatorResult,
} from '@nestjs/terminus';
import { PrismaClient } from '@prisma/client';

@Injectable()
export class HealthCheckUtilsService extends HealthIndicator {
  constructor(
    private readonly prismaService: PrismaClient,
    private readonly httpService: HttpService
  ) {
    super();
  }

  async prismaIsHealthy(key: string): Promise<HealthIndicatorResult> {
    try {
      await this.prismaService.$queryRaw`SELECT 1`;
      return this.getStatus(key, true);
    } catch (err) {
      throw new HealthCheckError('Prisma health check failed', err);
    }
  }

  async identityIsHealthy(key: string): Promise<HealthIndicatorResult> {
    try {
      await this.httpService.axiosRef.get(
        `${process.env.IDENTITY_BASE_URL}/health`
      );
      return this.getStatus(key, true);
    } catch (err) {
      throw new HealthCheckError('Identity health check failed', err);
    }
  }

  async credSchemaIsHealthy(key: string): Promise<HealthIndicatorResult> {
    try {
      await this.httpService.axiosRef.get(
        `${process.env.SCHEMA_BASE_URL}/health`
      );
      return this.getStatus(key, true);
    } catch (err) {
      throw new HealthCheckError('Credential Schema health check failed', err);
    }
  }
}
