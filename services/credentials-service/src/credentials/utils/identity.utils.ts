import { HttpService } from "@nestjs/axios";
import { JwtCredentialPayload } from "did-jwt-vc";
import { IssuerType } from "did-jwt-vc/lib/types";
import { AxiosResponse } from '@nestjs/terminus/dist/health-indicator/http/axios.interfaces';
import { DIDDocument } from "did-resolver";
import { InternalServerErrorException, Logger } from "@nestjs/common";

export const signVC = async (credentialPlayload: JwtCredentialPayload, did: IssuerType, httpService: HttpService): Promise<String> => {
  try {
    const signedVCResponse: AxiosResponse =
      await httpService.axiosRef.post(
        `${process.env.IDENTITY_BASE_URL}/utils/sign`,
        {
          DID: did,
          payload: JSON.stringify(credentialPlayload),
        },
      );
    return signedVCResponse.data.signed as string;
  } catch (err) {
    Logger.error('Error signing VC: ', err);
    throw new InternalServerErrorException('Error signing VC');
  }
}

export const resolveDID = async (issuer, httpService: HttpService): Promise<DIDDocument> => {
  try {
    const verificationURL = `${process.env.IDENTITY_BASE_URL}/did/resolve/${issuer}`;
    const dIDResponse: AxiosResponse = await httpService.axiosRef.get(
      verificationURL,
    );

    return dIDResponse.data as DIDDocument;
  } catch (err) {
    Logger.error('Error resolving DID: ', err);
    throw new InternalServerErrorException('Error resolving DID');
  }
}

export const generateDID = async (alsoKnownAs: ReadonlyArray<String>, httpService: HttpService): Promise<ReadonlyArray<DIDDocument>> => {
  try {
    const didGenRes: AxiosResponse = await httpService.axiosRef.post(
      `${process.env.IDENTITY_BASE_URL}/did/generate`,
      {
        content: [
          {
            alsoKnownAs: alsoKnownAs,
            services: [
              {
                id: 'IdentityHub',
                type: 'IdentityHub',
                serviceEndpoint: {
                  '@context': 'schema.identity.foundation/hub',
                  '@type': 'UserServiceEndpoint',
                  instance: ['did:test:hub.id'],
                },
              },
            ],
          },
        ],
      },
    );
    return didGenRes.data as ReadonlyArray<DIDDocument>;
  } catch (err) {
    Logger.error('Error generating DID: ', err);
    throw new InternalServerErrorException('Error generating DID');
  }
} 