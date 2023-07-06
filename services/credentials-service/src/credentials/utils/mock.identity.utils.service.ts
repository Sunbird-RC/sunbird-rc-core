import { HttpService } from '@nestjs/axios';
import { JwtCredentialPayload } from 'did-jwt-vc';
import { IssuerType } from 'did-jwt-vc/lib/types';
import { DIDDocument } from 'did-resolver';
import { Injectable } from '@nestjs/common';
import { randomUUID } from 'crypto';

@Injectable()
export class MockIdentityUtilsService {
  constructor(private readonly httpService: HttpService) {}

  async signVC(
    credentialPlayload: JwtCredentialPayload,
    did: IssuerType
  ): Promise<String> {
    const mockSign = {
      publicKey: {
        crv: 'Ed25519',
        x: 'teP8Iy5wgLQNYzqLjpi91ookk7q-6R8jnAPm_3CFbcE',
        kty: 'OKP',
      },
      signed:
        'eyJhbGciOiJFZERTQSJ9.eyJAY29udGV4dCI6WyJodHRwczovL3d3dy53My5vcmcvMjAxOC9jcmVkZW50aWFscy92MSIsImh0dHBzOi8vd3d3LnczLm9yZy8yMDE4L2NyZWRlbnRpYWxzL2V4YW1wbGVzL3YxIl0sImNyZWRlbnRpYWxTdWJqZWN0Ijp7ImNlcnRpZnlpbmdJbnN0aXR1dGUiOiJJSUlUIFNvbmVwYXQiLCJldmFsdWF0aW5nSW5zdGl0dXRlIjoiTklUIEt1cnVrc2hldHJhIiwiZ3JhZGUiOiI5LjIzIiwiaWQiOiJkaWQ6dWxwOjA1YTcxMGZlLTg1NzMtNDNiNy05MGQ1LWRhNGE0MzAzMzMyZSIsInByb2dyYW1tZSI6IkIuVGVjaCJ9LCJleHBpcmF0aW9uRGF0ZSI6IjIwMjMtMDItMDhUMTE6NTY6MjcuMjU5WiIsImlkIjoiZGlkOnVscDpiNGExOTFhZi1kODZlLTQ1M2MtOWQwZS1kZDQ3NzEwNjcyMzUiLCJpc3N1ZXIiOiJkaWQ6dWxwOjU2OTk2ODhjLTZkNzAtNGM4MC1hYTdjLWM2NDc2YWI5Zjk0MCIsIm9wdGlvbnMiOnsiY3JlYXRlZCI6IjIwMjAtMDQtMDJUMTg6NDg6MzZaIiwiY3JlZGVudGlhbFN0YXR1cyI6eyJ0eXBlIjoiUmV2b2NhdGlvbkxpc3QyMDIwU3RhdHVzIn19LCJ0eXBlIjpbIlZlcmlmaWFibGVDcmVkZW50aWFsIiwiVW5pdmVyc2l0eURlZ3JlZUNyZWRlbnRpYWwiXX0.6oYa_lYOlRvYeVCmRMuwAivsDoWAlfHAfAezFD2xzPaZgiCXFThc-gAvcBTdB0K9VfTNH_PES0czOqDf7tN2Bg',
    };
    return mockSign.signed;
  }

  async resolveDID(issuer): Promise<DIDDocument> {
    const mockResolveDIDResponse = {
      '@context': 'https://w3id.org/did/v1',
      id: 'did:rcw:22cee0da-919a-4e42-9ec0-88bf3beb75e3',
      alsoKnownAs: ['did.yash@gmail.com.yashtest'],
      verificationMethod: [
        {
          id: 'auth-key',
          type: 'Ed25519VerificationKey2020',
          publicKeyJwk: {
            crv: 'Ed25519',
            x: 'HbuYMbh-h3itjJSR54hRPk1nQFUEuAlj_NG-hIi8svY',
            kty: 'OKP',
          },
          controller: 'did:rcw:22cee0da-919a-4e42-9ec0-88bf3beb75e3',
        },
      ],
      authentication: ['auth-key'],
    };
    return mockResolveDIDResponse as DIDDocument;
  }

  async generateDID(
    alsoKnownAs: ReadonlyArray<String>
  ): Promise<ReadonlyArray<DIDDocument>> {
    const mockGenerateDIDResponse = [
      {
        '@context': 'https://w3id.org/did/v1',
        id: randomUUID(),
        alsoKnownAs: ['did.yash@gmail.com.yashtest'],
        verificationMethod: [
          {
            id: 'auth-key',
            type: 'Ed25519VerificationKey2020',
            publicKeyJwk: {
              crv: 'Ed25519',
              x: 'q2GMse-F_sx9CxtfwFb-5CTBuRgkhJdAALzamK6t3mI',
              kty: 'OKP',
            },
            controller: randomUUID(),
          },
        ],
        authentication: ['auth-key'],
      },
    ];

    return mockGenerateDIDResponse as ReadonlyArray<DIDDocument>;
  }
}
