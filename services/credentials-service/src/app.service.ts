import { Injectable } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { IssueRequest, VCRequest } from './app.interface';
import { PrismaService } from './prisma.service';
import { VC } from '@prisma/client';
import {
  transformCredentialInput,
  CredentialPayload,
  W3CCredential,
  Verifiable,
  JwtCredentialPayload,
} from 'did-jwt-vc';
import { HttpService } from '@nestjs/axios';
import { DIDDocument } from 'did-resolver';
import { AxiosResponse } from '@nestjs/terminus/dist/health-indicator/http/axios.interfaces';

// eslint-disable-next-line @typescript-eslint/no-var-requires
const ION = require('@decentralized-identity/ion-tools');

// eslint-disable-next-line @typescript-eslint/no-var-requires
const QRCode = require('qrcode');

@Injectable()
export class AppService {
  constructor(
    private prisma: PrismaService,
    private configService: ConfigService,
    private httpService: HttpService,
  ) {}

  async claim(vcReqestData: VCRequest): Promise<VC> {
    //TODO: Veify the VCRequest
    // Verify Issuer
    // Verify Subject
    // Verify Type
    // Verify Credential Schema
    // Verify Credential

    const unsignedVC = await this.prisma.vC.create({
      data: {
        subject: vcReqestData.subject,
        type: vcReqestData.type,
        issuer: vcReqestData.issuer,
        unsigned: vcReqestData.credential,
        credential_schema: vcReqestData.schema,
      },
    });
    return unsignedVC;
  }

  async signVC(credentialPlayload: JwtCredentialPayload, did: string) {
    //console.log(credentialPlayload);
    // did = 'did:ulp:5d7682f4-3cca-40fb-9fa2-1f6ebef4803b';
    //console.log(process.env.IDENTITY_BASE_URL);
    console.time("Axios Resp");
    const signedVCResponse: AxiosResponse =
      await this.httpService.axiosRef.post(
        `${process.env.IDENTITY_BASE_URL}/utils/sign`,
        {
          DID: did,
          payload: JSON.stringify(credentialPlayload),
        },
      );
    console.timeEnd("Axios Resp");
    return signedVCResponse.data.signed as string;
  }

  async issue(vcReqestData: IssueRequest): Promise<VC> {
    const credential = await this.prisma.vC.findUnique({
      where: { id: vcReqestData.id },
    });
    const signedCredential: any = credential.unsigned;
    // TODO: Sign the VC and udpate proof value
    signedCredential.proof = {
      proofValue: await this.signVC(
        transformCredentialInput(credential.unsigned as CredentialPayload),
        credential.issuer,
      ),
      type: 'Ed25519Signature2020',
      created: new Date().toISOString(),
      verificationMethod: credential.issuer,
      proofPurpose: 'assertionMethod',
    };

    const signedVC = await this.prisma.vC.update({
      data: {
        signed: signedCredential,
      },
      where: { id: vcReqestData.id },
    });
    return signedVC;
  }

  getVCBySubject(sub: string) {
    return this.prisma.vC.findMany({
      where: {
        subject: sub,
      },
      select: {
        signed: true,
      },
    });
  }

  getVCByIssuer(issuer: string) {
    return this.prisma.vC.findMany({
      where: {
        issuer: issuer,
      },
      select: {
        signed: true,
      },
    });
  }

  async verify(credential: Verifiable<W3CCredential>): Promise<boolean> {
    // resolve DID
    // const verificationMethod: VerificationMethod =
    //   credential.proof.verificationMethod;
    const verificationMethod = 'did:ulp:5d7682f4-3cca-40fb-9fa2-1f6ebef4803b';
    const dIDResponse: AxiosResponse = await this.httpService.axiosRef.get(
      `${process.env.IDENTITY_BASE_URL}/did/resolve/${verificationMethod}`,
    );
    const did: DIDDocument = dIDResponse.data as DIDDocument;
    try {
      const verified = await ION.verifyJws({
        jws: credential.proof.proofValue,
        publicJwk: did.verificationMethod[0].publicKeyJwk,
      });
      console.debug(verified);
      return true;
    } catch (e) {
      return false;
    }
  }

  async renderAsQR(credentialId: string): Promise<any> {
    const credential = await this.prisma.vCV2.findUnique({
      where: { id: credentialId },
    });

    try {
      const QRData = await QRCode.toDataURL(
        (credential.signed as Verifiable<W3CCredential>).proof.proofValue,
      );
      return QRData;
    } catch (err) {
      console.error(err);
      return err;
    }
  }

  // updateStatus(req: VCUpdateRequest, token: string): any {
  //   let vc = this.getVCBySub(req.sub)
  //   vc['vc']['credentialStatus'] = req.crdentialStatus;
  //   //TODO: get signed proof from MS from token
  //   const proof = "Response: Signed String"
  //   vc['vc']['proof'] = proof;
  //   this.prisma.vC.update({
  //     where: {
  //       sub_iss: {
  //         sub: req.sub,
  //         iss: req.iss,
  //       }
  //     },
  //     data: vc
  //   })
  //   return "Credential status successfully updated"
  // }
}
