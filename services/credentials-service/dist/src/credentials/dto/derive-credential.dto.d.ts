import { IssuedVerifiableCredential } from '../schema/VC.schema';
export declare class DeriveCredentialDTO {
    verifiableCredential: IssuedVerifiableCredential;
    frame: JSON;
    options: {
        nonce: string;
    };
}
