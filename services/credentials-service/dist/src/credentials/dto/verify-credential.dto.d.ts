import { IssuedVerifiableCredential } from '../schema/VC.schema';
export declare class VerifyCredentialDTO {
    verifiableCredential: IssuedVerifiableCredential;
    options: {
        challenge: string;
        domain: string;
    };
}
