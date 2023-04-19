"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.JwtAuthGuard = void 0;
const common_1 = require("@nestjs/common");
const passport_1 = require("@nestjs/passport");
const core_1 = require("@nestjs/core");
let JwtAuthGuard = class JwtAuthGuard extends (0, passport_1.AuthGuard)('jwt') {
    constructor(reflector) {
        super();
        this.reflector = reflector;
    }
    async canActivate(context) {
        await super.canActivate(context);
        console.log('context: ', context.getHandler());
        console.log('context.switchToHttp().getRequest(): ', context.switchToHttp().getRequest()['user']['roles']);
        const roles = this.reflector.get('roles', context.getHandler());
        console.log('roles: ', roles);
        if (!roles) {
            return true;
        }
        let isAllowed = false;
        const request = context.switchToHttp().getRequest();
        try {
            const tokenRoles = request['user']['roles'];
            for (const role of roles) {
                if (tokenRoles.indexOf(role) > -1) {
                    isAllowed = true;
                    break;
                }
            }
            if (tokenRoles.indexOf('Student') > -1) {
                isAllowed = true;
            }
        }
        catch (error) {
            console.log({ err: error });
            isAllowed = false;
        }
        return isAllowed;
    }
    handleRequest(err, user, info) {
        console.log('in handle request!');
        console.log({ handleRequest: info, err: err, user: user });
        return user;
    }
};
JwtAuthGuard = __decorate([
    (0, common_1.Injectable)(),
    __metadata("design:paramtypes", [core_1.Reflector])
], JwtAuthGuard);
exports.JwtAuthGuard = JwtAuthGuard;
//# sourceMappingURL=auth.guard.js.map