// import { Injectable } from '@nestjs/common';
// import {
//   HealthCheckError,
//   HealthIndicator,
//   HealthIndicatorResult,
// } from '@nestjs/terminus';
// import { PrismaService } from 'src/prisma.service';

// @Injectable()
// export class PrismaHealthIndicator extends HealthIndicator {
//   constructor(private readonly prismaService: PrismaService) {
//     super();
//   }

//   async isHealthy(key: string): Promise<HealthIndicatorResult> {
//     try {
//       await this.prismaService.vC.findFirst({
//         filter: {
//           subject: 'alumni',
//         },
//       });
//       return this.getStatus(key, true);
//     } catch (e) {
//       throw new HealthCheckError('Prisma check failed', e);
//     }
//   }
// }
