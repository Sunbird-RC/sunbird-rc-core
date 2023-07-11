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
    expect(res.status).toBe('PUBLISHED');
  });

  it('should create a new schema and then update its tags', async () => {
    const schemaPayload = generateCredentialSchemaTestBody();
    const did = await utilsService.generateDID(generateTestDIDBody());
    schemaPayload.schema.author = did.id;
    const { body: schema } = await request(httpServer)
      .post('/credential-schema')
      .send(schemaPayload)
      .expect(201);
    const { body: res } = await request(httpServer)
      .put(`/credential-schema/${schema.schema.id}/${schema.schema.version}`)
      .send({ tags: ['T1', 'T2'] })
      .expect(200);
    expect(res.tags).toEqual(['T1', 'T2']);
  });

  it('should create a schema and then update its core fields', async () => {
    const schemaPayload = generateCredentialSchemaTestBody();
    const did = await utilsService.generateDID(generateTestDIDBody());
    schemaPayload.schema.author = did.id;
    const { body: schema } = await request(httpServer)
      .post('/credential-schema')
      .send(schemaPayload)
      .expect(201);
    const newSchemaPayload = generateCredentialSchemaTestBody();
    newSchemaPayload.schema.author = schema.schema.id;
    const { body: newSchema } = await request(httpServer)
      .put(`/credential-schema/${schema.schema.id}/${schema.schema.version}`)
      .send(newSchemaPayload)
      .expect(200);
    expect(newSchema.schema.id).toBe(schema.schema.id);
    expect(newSchema.schema.version).toEqual('1.1.0');
    expect(newSchema.schema.author).toEqual(schema.schema.id);
  });

  it('should create a schema publish it and then update its metadata', async () => {
    const schemaPayload = generateCredentialSchemaTestBody();
    const did = await utilsService.generateDID(generateTestDIDBody());
    schemaPayload.schema.author = did.id;
    const { body: schema } = await request(httpServer)
      .post('/credential-schema')
      .send(schemaPayload)
      .expect(201);
    const { body: resPublish } = await request(httpServer)
      .put(
        `/credential-schema/publish/${schema.schema.id}/${schema.schema.version}`,
      )
      .expect(200);
    expect(resPublish.status).toBe('PUBLISHED');
    expect(resPublish.schema.version).toBe('1.0.0');

    const { body: resTags } = await request(httpServer)
      .put(`/credential-schema/${schema.schema.id}/${schema.schema.version}`)
      .send({ tags: ['T1', 'T2'] })
      .expect(200);

    expect(resTags.status).toBe('PUBLISHED');
    expect(resTags.schema.version).toBe('1.1.0');
  });

  it('should create a schema publish it and then update its core fields', async () => {
    const schemaPayload = generateCredentialSchemaTestBody();
    const did = await utilsService.generateDID(generateTestDIDBody());
    schemaPayload.schema.author = did.id;
    const { body: schema } = await request(httpServer)
      .post('/credential-schema')
      .send(schemaPayload)
      .expect(201);
    const { body: resPublish } = await request(httpServer)
      .put(
        `/credential-schema/publish/${schema.schema.id}/${schema.schema.version}`,
      )
      .expect(200);
    expect(resPublish.status).toBe('PUBLISHED');
    expect(resPublish.schema.version).toBe('1.0.0');

    const newSchemaPayload = generateCredentialSchemaTestBody();
    newSchemaPayload.schema.author = schema.schema.id;
    const { body: newSchema } = await request(httpServer)
      .put(
        `/credential-schema/${resPublish.schema.id}/${resPublish.schema.version}`,
      )
      .send(newSchemaPayload)
      .expect(200);
    expect(newSchema.schema.id).toBe(schema.schema.id);
    expect(newSchema.schema.version).toEqual('2.0.0');
    expect(newSchema.schema.author).toEqual(schema.schema.id);
    expect(newSchema.status).toEqual('DRAFT');
  });

  it('should create a schema and then deprecate it', async () => {
    const schemaPayload = generateCredentialSchemaTestBody();
    const did = await utilsService.generateDID(generateTestDIDBody());
    schemaPayload.schema.author = did.id;
    const { body: schema } = await request(httpServer)
      .post('/credential-schema')
      .send(schemaPayload)
      .expect(201);
    const { body: res } = await request(httpServer)
      .put(
        `/credential-schema/deprecate/${schema.schema.id}/${schema.schema.version}`,
      )
      .expect(200);
    expect(res.status).toBe('DEPRECATED');
    expect(res.schema.version).toBe('1.0.0');
  });

  it('should create a schema, publish it and then deprecate it', async () => {
    const schemaPayload = generateCredentialSchemaTestBody();
    const did = await utilsService.generateDID(generateTestDIDBody());
    schemaPayload.schema.author = did.id;
    const { body: schema } = await request(httpServer)
      .post('/credential-schema')
      .send(schemaPayload)
      .expect(201);
    const { body: resPublish } = await request(httpServer)
      .put(
        `/credential-schema/publish/${schema.schema.id}/${schema.schema.version}`,
      )
      .expect(200);
    expect(resPublish.status).toBe('PUBLISHED');
    expect(resPublish.schema.version).toBe('1.0.0');
    const { body: res } = await request(httpServer)
      .put(
        `/credential-schema/deprecate/${schema.schema.id}/${schema.schema.version}`,
      )
      .expect(200);
    expect(res.status).toBe('DEPRECATED');
    expect(res.schema.version).toBe('1.0.0');
  });

  it('should create a schema, publish it and then revoke it', async () => {
    const schemaPayload = generateCredentialSchemaTestBody();
    const did = await utilsService.generateDID(generateTestDIDBody());
    schemaPayload.schema.author = did.id;
    const { body: schema } = await request(httpServer)
      .post('/credential-schema')
      .send(schemaPayload)
      .expect(201);
    const { body: resPublish } = await request(httpServer)
      .put(
        `/credential-schema/publish/${schema.schema.id}/${schema.schema.version}`,
      )
      .expect(200);
    expect(resPublish.status).toBe('PUBLISHED');
    expect(resPublish.schema.version).toBe('1.0.0');
    const { body: res } = await request(httpServer)
      .put(
        `/credential-schema/revoke/${schema.schema.id}/${schema.schema.version}`,
      )
      .expect(200);
    expect(res.status).toBe('REVOKED');
    expect(res.schema.version).toBe('1.0.0');
  });

  it('should create a schema, publish it, update it, deprecate it', () => {
    return;
  });
});
