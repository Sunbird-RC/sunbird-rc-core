import { Test, TestingModule } from '@nestjs/testing';
import { INestApplication } from '@nestjs/common';
import * as request from 'supertest';
import { AppModule } from './../src/app.module';

describe('AppController (e2e)', () => {
  let app: INestApplication;

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

  it('should generate a DID with a custom method and successfully sign and verify a credential', async () => {
    const result = request(app.getHttpServer()).post('/did/generate').send(
      {
        "content": [
          {
            "alsoKnownAs": [
              "C4GT",
              "https://www.codeforgovtech.in/"
            ],
            "services": [
              {
                "id": "C4GT",
                "type": "IdentityHub",
                "serviceEndpoint": {
                  "@context": "schema.c4gt.acknowledgment",
                  "@type": "UserServiceEndpoint",
                  "instance": [
                    "https://www.codeforgovtech.in"
                  ]
                }
              }
            ],
            "method": "C4GT"
          }
        ]
      }
    )
  });

  it('should generate a DID with a custom method and unsuccessfully sign and verify a credential', async () => {

  });

  it('should generate a DID with the default method and successfully sign and verify a credential', async () => {

  });

  it('should generate a DID with the default method and unsuccessfully sign and verify a credential', async () => {

  });

});
