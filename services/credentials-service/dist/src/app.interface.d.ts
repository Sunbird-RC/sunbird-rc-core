import { W3CCredential } from 'did-jwt-vc';
export type Extensible<T> = T & {
    [x: string]: any;
};
export interface CredentialStatus {
    id: string;
    type: string;
}
export type JwtCredentialSubject = Record<string, any>;
export type Credential = Extensible<{
    '@context': string[] | string;
    type: string[] | string;
    issuer: string;
    credentialSubject: JwtCredentialSubject;
    credentialStatus?: CredentialStatus;
    termsOfUse?: any;
    proof?: string;
}>;
export declare class VCRequest {
    issuer?: string;
    subject: string;
    schema: string;
    type: string;
    credential: W3CCredential;
}
export declare class IssueRequest {
    id: string;
}
export interface vc {
    '@context': Array<string>;
    type: Array<string>;
}
export interface credential {
    '@context': Array<string>;
    id: string;
    type: Array<string>;
    issuer: string;
    issueance_date: string;
    expiration_date: string;
    credentialSubject: any;
}
export interface credentialOptions {
    created: string;
    challenge: string;
    domain: string;
    credentialStatus: any;
}
export declare class VCResponse {
    '@context': Array<string>;
    id: string;
    type: string[];
    issuer: string;
    issuanceDate: Date;
    credentialSubject: any;
    proof: Proof;
}
export interface Proof {
    type: string;
    created?: Date;
    proofPurpose?: string;
    verificationMethod?: string;
    jws: string;
}
export declare class VCUpdateRequest {
    sub: string;
    iss: string;
    crdentialStatus: any;
}
