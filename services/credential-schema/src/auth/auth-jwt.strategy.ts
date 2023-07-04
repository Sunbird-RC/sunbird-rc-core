import { Logger, UnauthorizedException } from '@nestjs/common';
import { PassportStrategy } from '@nestjs/passport';
import { passportJwtSecret } from 'jwks-rsa';
import { ExtractJwt, Strategy } from 'passport-jwt';

export class JwtStrategy extends PassportStrategy(Strategy) {
  private logger = new Logger(JwtStrategy.name);
  constructor() {
    super({
      jwtFromRequest: ExtractJwt.fromAuthHeaderAsBearerToken(),
      ignoreExpiration: false,
      secretOrKeyProvider: passportJwtSecret({
        cache: true,
        rateLimit: true,
        jwksRequestsPerMinute: 5,
        jwksUri: process.env.JWKS_URI,
      }),
      algorithms: ['RS256'],
    });
  }

  async validate(payload: any) {
    if (!payload) {
      this.logger.log('Invalid JWT, Authorization failed');
      throw new UnauthorizedException();
    }
    this.logger.debug('Valid JWT, Authorization passed');
    return { roles: payload.roles };
  }
}
