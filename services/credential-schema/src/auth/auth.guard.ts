import { ExecutionContext, Injectable, Logger } from '@nestjs/common';
import { AuthGuard, IAuthGuard } from '@nestjs/passport';
import { Reflector } from '@nestjs/core';

@Injectable()
export class JwtAuthGuard extends AuthGuard('jwt') implements IAuthGuard {
  constructor(private reflector: Reflector) {
    super();
  }
  private logger = new Logger(JwtAuthGuard.name);

  public async canActivate(context: ExecutionContext): Promise<boolean> {
    await super.canActivate(context);
    this.logger.debug('context', context.getHandler());
    const roles = this.reflector.get<string[]>('roles', context.getHandler());
    this.logger.debug('roles', roles);
    if (!roles) {
      // if no roles are specified in the auth guard decorator, allow access
      return true;
    }

    let isAllowed = false;
    const request: Request = context.switchToHttp().getRequest();
    try {
      const tokenRoles: string[] = request['user']['roles'];
      for (const role of roles) {
        if (tokenRoles.indexOf(role) > -1) {
          isAllowed = true;
          break;
        }
      }
      if (tokenRoles.indexOf('Student') > -1) {
        isAllowed = true;
      }
    } catch (error) {
      this.logger.error('Error', error);
      isAllowed = false;
    }
    return isAllowed;
  }

  handleRequest(err, user, info) {
    this.logger.debug('Handle request', {
      handleRequest: info,
      err: err,
      user: user,
    });
    return user;
  }
}
