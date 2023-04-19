import { Test, TestingModule } from '@nestjs/testing';
import { INestApplication } from '@nestjs/common';
import * as request from 'supertest';
import { AppModule } from './../src/app.module';

describe('AppController (e2e)', () => {
  let app: INestApplication;
  let id:any;
  const sampleSchemaPayload: any = {
    "schema":{
  "@context": [
    "https://www.w3.org/2018/credentials/v1",
    "https://www.w3.org/2018/credentials/examples/v1",
    "https://playground.chapi.io/examples/alumni/alumni-v1.json",
    "https://w3id.org/security/suites/ed25519-2020/v1"
  ],
  "type": "https://w3c-ccg.github.io/vc-json-schemas/",
  "version": "1.0",
  "id": "did:example:wvfeb1f712ebc6f1a276e12ec21",
  "name": "Proof of Academic Evaluation Credential",
  "author": "did:ulp:0bc51dad-885c-44a8-8e95-e3d160060bd2",
  "authored": "2022-12-19T09:22:23.064Z",
  "schema": {
    "$id": "Proof-of-Academic-Evaluation-Credential-1.0",
    "$schema": "https://json-schema.org/draft/2019-09/schema",
    "description": "The holder has secured the <PERCENTAGE/GRADE> in <PROGRAMME> from <ABC_Institute>.",
    "type": "object",
    "properties": {
      "grade": {
        "type": "string",
        "description": "Grade (%age, GPA, etc.) secured by the holder."
      },
      "programme": {
        "type": "string",
        "description": "Name of the programme pursed by the holder."
      },
      "certifyingInstitute": {
        "type": "string",
        "description": "Name of the instute which certified the said grade in the said skill"
      },
      "evaluatingInstitute": {
        "type": "string",
        "description": "Name of the institute which ran the programme and evaluated the holder."
      }
    },
    "required": [
      "grade",
      "programme",
      "certifyingInstitute",
      "evaluatingInstitute"
    ],
    "additionalProperties": false
  },
  "proof": {
    "type": "Ed25519Signature2020",
    "created": "2022-12-19T09:22:23Z",
    "verificationMethod": "did:key:z6MkqYDbJ5yVgg5UvfRt5DAsk5dvPTgo6H9CZcenziWdHTqN#z6MkqYDbJ5yVgg5UvfRt5DAsk5dvPTgo6H9CZcenziWdHTqN",
    "proofPurpose": "assertionMethod",
    "proofValue": "z5iBktnPCr3hPqN7FViY948ds5yMhrL1qujMmVD1GmzsbtXw5RUCdu4GKrQZw8U9c4G78SUNmPLTS87tz6kGAHgXB"
  }
}

,
    "tags":["academic","marks", "evaluation", "education"],
    "status": "DRAFT"

}

  beforeEach(async () => {
    const moduleFixture: TestingModule = await Test.createTestingModule({
      imports: [AppModule],
    }).compile();

    app = moduleFixture.createNestApplication();
    await app.init();
  });

  it('/credential-schema (GET)', () => {
    return request(app.getHttpServer())
      .get('/credential-schema')
      .expect(200)
      .expect('Hello World!');
  });
});
