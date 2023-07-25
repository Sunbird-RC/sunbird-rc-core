import { Test, TestingModule } from '@nestjs/testing';
import { INestApplication } from '@nestjs/common';
import * as request from 'supertest';
import { AppModule } from './../src/app.module';
import { HttpModule, HttpService } from '@nestjs/axios';
import { IdentityUtilsService } from '../src/credentials/utils/identity.utils.service';
import {
  generateCredentialRequestPayload,
  generateCredentialSchemaTestBody,
} from '../src/credentials/credentials.fixtures';

describe('AppController (e2e)', () => {
  let app: INestApplication;
  let id: any;
  let sampleCredReqPayload: any;
  beforeEach(async () => {
    const moduleFixture: TestingModule = await Test.createTestingModule({
      imports: [AppModule, HttpModule],
    }).compile();

    app = moduleFixture.createNestApplication();
    let httpSerivce = moduleFixture.get<HttpService>(HttpService);
    let identityUtilsService =
      moduleFixture.get<IdentityUtilsService>(IdentityUtilsService);

    let issuerDID = (
      await identityUtilsService.generateDID([
        'VerifiableCredentialTESTINGIssuer',
      ])
    )[0].id;

    let subjectDID = (
      await identityUtilsService.generateDID([
        'VerifiableCredentialTESTINGIssuer',
      ])
    )[0].id;

    const schemaPayload = generateCredentialSchemaTestBody();
    schemaPayload.schema.author = issuerDID;
    const schema = await httpSerivce.axiosRef.post(
      `${process.env.SCHEMA_BASE_URL}/credential-schema`,
      schemaPayload
    );
    let credentialSchemaID = schema.data.schema.id;
    sampleCredReqPayload = generateCredentialRequestPayload(
      issuerDID,
      subjectDID,
      credentialSchemaID,
      schema.data.schema.version
    );

    await app.init();
  });

  it('/credentials/issue (POST)', () => {
    return request(app.getHttpServer())
      .post('/credentials/issue')
      .send(sampleCredReqPayload)
      .expect(201)
      .then((res) => {
        sampleCredReqPayload.credential['id'] = res.body.credential.id;
        id = res.body.credential.id;
      });
  });

  it('/credentials/:id (GET)', () => {
    return request(app.getHttpServer())
      .get(`/credentials/${id}`)
      .expect(200)
  });

  it('/credentials/:id/verify (GET)', () => {
    return request(app.getHttpServer())
      .get(`/credentials/${id}/verify`)
      .expect(200)
      .expect({
        status: 'ISSUED',
        checks: [
          {
            active: 'OK',
            revoked: 'OK',
            expired: 'NOK',
            proof: 'OK',
          },
        ],
      });
  });
});
