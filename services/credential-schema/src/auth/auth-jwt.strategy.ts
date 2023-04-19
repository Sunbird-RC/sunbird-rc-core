import { UnauthorizedException } from '@nestjs/common';
import { PassportStrategy } from '@nestjs/passport';
import { passportJwtSecret } from 'jwks-rsa';
import { ExtractJwt, Strategy } from 'passport-jwt';

export class JwtStrategy extends PassportStrategy(Strategy) {
  constructor() {
    console.log('process.env.JWKS_URI : ', process.env.JWKS_URI);
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
    console.log('in validate: ', payload);

    if (!payload) {
      throw new UnauthorizedException();
    }
    console.log('VALID');
    return { roles: payload.roles };
  }
}
