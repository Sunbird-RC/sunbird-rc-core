import { BasicStrategy as Strategy } from 'passport-http';
import { Injectable, Logger, UnauthorizedException } from '@nestjs/common';
import { PassportStrategy } from '@nestjs/passport';

@Injectable()
export class BasicStrategy extends PassportStrategy(Strategy) {
  private logger = new Logger(BasicStrategy.name);
  constructor() {
    super({
      passReqToCallback: true,
    });
  }

  public validate = async (req, username, password): Promise<boolean> => {
    if (
      process.env.HTTP_BASIC_USER === username &&
      process.env.HTTP_BASIC_PASS === password
    ) {
      this.logger.debug('Authorized request, validation successfull');
      return true;
    }
    this.logger.log(
      'Unauthorized Exception, Username and Password is not correct',
    );
    throw new UnauthorizedException();
  };
}
