import { ConfigService } from '@nestjs/config';
import { Verifiable, W3CCredential } from 'did-jwt-vc';
import { IssueRequest } from './app.interface';
import { AppService } from './app.service';
export declare class AppController {
    private readonly appService;
    private configService;
    constructor(appService: AppService, configService: ConfigService);
    handleHealthCheck(): string;
    issue(issueRequest: IssueRequest): any;
    createQR(id: any): Promise<{
        image: any;
    }>;
    renderCredential(id: any): Promise<{
        image: any;
    }>;
    verify(credential: Verifiable<W3CCredential>): any;
}
