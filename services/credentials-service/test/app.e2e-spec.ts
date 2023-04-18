import { Test, TestingModule } from '@nestjs/testing';
import { INestApplication } from '@nestjs/common';
import * as request from 'supertest';
import { AppModule } from './../src/app.module';

describe('AppController (e2e)', () => {
  let app: INestApplication;
  let id: any;
  const sampleCredReqPayload: any = {
    credential: {
      '@context': [
        'https://www.w3.org/2018/credentials/v1',
        'https://www.w3.org/2018/credentials/examples/v1',
      ],
      type: ['VerifiableCredential', 'UniversityDegreeCredential'],
      issuer: 'did:ulp:76687f0a-e5bb-4176-86e1-28d5a76fdfa8',
      issuanceDate: '2023-02-06T11:56:27.259Z',
      expirationDate: '2023-02-08T11:56:27.259Z',
      credentialSubject: {
        id: 'did:ulp:b4a191af-d86e-453c-9d0e-dd4771067235',
        grade: '9.23',
        programme: 'B.Tech',
        certifyingInstitute: 'IIIT Sonepat',
        evaluatingInstitute: 'NIT Kurukshetra',
      },
    },
    credentialSchemaId: 'did:ulpschema:c9cc0f03-4f94-4f44-9bcd-b24a86596fa2',
    tags: ['tag1', 'tag2', 'tag3'],
  };

  beforeEach(async () => {
    const moduleFixture: TestingModule = await Test.createTestingModule({
      imports: [AppModule],
    }).compile();

    app = moduleFixture.createNestApplication();
    await app.init();
  });

  it('/ (GET)', () => {
    return request(app.getHttpServer())
      .get('/')
      .expect(200)
      .expect('Hello World!');
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
      .expect(sampleCredReqPayload.credential);
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
