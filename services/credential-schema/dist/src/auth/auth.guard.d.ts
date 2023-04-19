import { ExecutionContext } from '@nestjs/common';
import { IAuthGuard } from '@nestjs/passport';
import { Reflector } from '@nestjs/core';
declare const JwtAuthGuard_base: import("@nestjs/passport").Type<IAuthGuard>;
export declare class JwtAuthGuard extends JwtAuthGuard_base implements IAuthGuard {
    private reflector;
    constructor(reflector: Reflector);
    canActivate(context: ExecutionContext): Promise<boolean>;
    handleRequest(err: any, user: any, info: any): any;
}
export {};
