import { Logger, CanActivate, Injectable, Inject } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { Reflector } from '@nestjs/core';
import * as jwt from 'jsonwebtoken';
import * as jwksClient from 'jwks-rsa';

@Injectable()
export class AuthGuard implements CanActivate {
  private client: any;
  private getKey: any;
  private readonly logger = new Logger(AuthGuard.name);

  public constructor(
    private readonly reflector: Reflector,
    private readonly configService: ConfigService,
  ) {
    this.client = jwksClient({
      jwksUri: process.env.JWKS_URI,
      requestHeaders: {}, // Optional
      timeout: 30000, // Defaults to 30s
    });

    this.getKey = (header, callback) => {
      this.client.getSigningKey(header.kid, function (err, key) {
        if (err) callback(err, null);
        const signingKey = key?.publicKey || key?.rsaPublicKey;
        callback(null, signingKey);
      });
    };
  }

  async canActivate(context: any): Promise<boolean> {
    const isPublic = this.reflector.get<boolean>(
      'isPublic',
      context.getHandler(),
    );
    if (isPublic) return true;

    if (process.env.ENABLE_AUTH === undefined) {
      this.logger.warn('ENABLE_AUTH is not set, defaulting to true');
    }
    if (process.env.ENABLE_AUTH && process.env.ENABLE_AUTH.trim() === 'false')
      return true;

    const request = context.switchToHttp().getRequest();
    const authHeader = request.headers.authorization;

    if (!authHeader || !authHeader.startsWith('Bearer')) {
      this.logger.log('No Bearer token found');
      return false;
    }
    const bearerToken = authHeader.substring(7, authHeader.length);
    return new Promise((resolve) => {
      jwt.verify(bearerToken, this.getKey, (err, decoded) => {
        if (err) {
          this.logger.log(err);
          resolve(false);
        }
        if (decoded) resolve(true);
        resolve(false);
      });
    });
  }
}
