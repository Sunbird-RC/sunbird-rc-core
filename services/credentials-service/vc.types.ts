const { IssuerType, Proof } = require('did-jwt-vc/lib/types');
const {
  CredentialPayload,
  Verifiable,
  W3CCredential,
  JwtCredentialPayload,
} = require('did-jwt-vc');
const { DIDDocument, VerificationMethod } = require('did-resolver');
export type W3CCredential = typeof W3CCredential;
export type Verifiable<T> = typeof Verifiable;
export type CredentialPayload = typeof CredentialPayload;
export type IssuerType = typeof IssuerType;
export type Proof = typeof Proof;
export type DIDDocument = typeof DIDDocument;
export type VerificationMethod = typeof VerificationMethod;
export type JwtCredentialPayload = typeof JwtCredentialPayload;

