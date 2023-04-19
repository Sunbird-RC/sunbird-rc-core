"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.JwtStrategy = void 0;
const common_1 = require("@nestjs/common");
const passport_1 = require("@nestjs/passport");
const jwks_rsa_1 = require("jwks-rsa");
const passport_jwt_1 = require("passport-jwt");
class JwtStrategy extends (0, passport_1.PassportStrategy)(passport_jwt_1.Strategy) {
    constructor() {
        console.log('process.env.JWKS_URI : ', process.env.JWKS_URI);
        super({
            jwtFromRequest: passport_jwt_1.ExtractJwt.fromAuthHeaderAsBearerToken(),
            ignoreExpiration: false,
            secretOrKeyProvider: (0, jwks_rsa_1.passportJwtSecret)({
                cache: true,
                rateLimit: true,
                jwksRequestsPerMinute: 5,
                jwksUri: process.env.JWKS_URI,
            }),
            algorithms: ['RS256'],
        });
    }
    async validate(payload) {
        console.log('in validate: ', payload);
        if (!payload) {
            throw new common_1.UnauthorizedException();
        }
        console.log('VALID');
        return { roles: payload.roles };
    }
}
exports.JwtStrategy = JwtStrategy;
//# sourceMappingURL=auth-jwt.strategy.js.map