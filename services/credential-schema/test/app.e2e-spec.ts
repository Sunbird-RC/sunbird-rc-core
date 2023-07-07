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

  it('should create and return a new schema', () => {
    return;
  });

  it('should create a new schema and publish it', () => {
    return;
  });

  it('should create a new schema and then update its metadata', () => {
    return;
  });

  it('should createa a schema and then update its core fields', () => {
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
