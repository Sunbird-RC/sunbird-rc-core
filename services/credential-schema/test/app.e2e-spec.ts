import { Test, TestingModule } from '@nestjs/testing';
import { INestApplication } from '@nestjs/common';
import * as request from 'supertest';
import { AppModule } from './../src/app.module';
import {
  generateCredentialSchemaTestBody,
  generateTestDIDBody,
} from '../src/schema/schema.fixtures';
import { UtilsService } from '../src/utils/utils.service';

describe('AppController (e2e)', () => {
  let app: INestApplication;
  let utilsService: UtilsService;
  let httpServer;
  beforeEach(async () => {
    const moduleFixture: TestingModule = await Test.createTestingModule({
      imports: [AppModule],
    }).compile();

    app = moduleFixture.createNestApplication();
    utilsService = moduleFixture.get<UtilsService>(UtilsService);
    await app.init();
    httpServer = app.getHttpServer();
  });

  it('/ (GET)', () => {
    return request(httpServer).get('/health').expect(200);
  });

  it('should create and return a new schema', async () => {
    const schemaPayload = generateCredentialSchemaTestBody();
    const did = await utilsService.generateDID(generateTestDIDBody());
    schemaPayload.schema.author = did.id;
    await request(app.getHttpServer())
      .post('/credential-schema')
      .send(schemaPayload)
      .expect(201);
  });

  it('should create a new schema and publish it', async () => {
    const schemaPayload = generateCredentialSchemaTestBody();
    const did = await utilsService.generateDID(generateTestDIDBody());
    schemaPayload.schema.author = did.id;
    const { body: schema } = await request(httpServer)
      .post('/credential-schema')
      .send(schemaPayload)
      .expect(201);
    const { body: res } = await request(httpServer)
      .put(`/credential-schema/${schema.schema.id}/${schema.schema.version}`)
      .send({ status: 'PUBLISHED' })
      .expect(200);
    expect(res.schema.status).toBe('PUBLISHED');
  });

  it('should create a new schema and then update its metadata', () => {
    return;
  });

  it('should create a schema and then update its core fields', () => {
    return;
  });

  it('should create a schema publish it and then update its metadata', () => {
    return;
  });

  it('should create a schema publish it and then update its core fields', () => {
    return;
  });

  it('should create a schema and then deprecate it', () => {
    return;
  });

  it('should create a schema, publish it and then deprecate it', () => {
    return;
  });

  it('should create a schema, publish it and then revoke it', () => {
    return;
  });

  it('should create a schema, publish it, update it, deprecate it', () => {
    return;
  });
});
