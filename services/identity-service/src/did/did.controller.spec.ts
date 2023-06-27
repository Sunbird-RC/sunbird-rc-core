import { Test, TestingModule } from '@nestjs/testing';
import { DidController } from './did.controller';
import { InternalServerErrorException } from '@nestjs/common';

describe('DidController', () => {
  let controller: DidController;
  const content = {
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

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      controllers: [DidController],
    }).compile();

    controller = module.get<DidController>(DidController);
  });

  it('should be defined', () => {
    expect(controller).toBeDefined();
  });

  it('should test DID generation', async () => {
    const dids = await controller.generateDID(content);
    expect(dids).toBeDefined();
    expect(dids).toHaveLength(1);
    expect(dids[0].id).toBeDefined();
    expect(dids[0].verificationMethod).toBeDefined();
  });

  it('should test bulk DID generation', async () => {
    const toGenerate = content;
    for (let i = 0; i < 10; i++) {
      toGenerate.content.push(content.content[0]);
    }
    try {
      const dids = await controller.generateDID(toGenerate);
      expect(dids).toBeDefined();
      expect(dids).toHaveLength(11);
    } catch (err) {
      expect(err).toBeDefined();
      expect(err).toBeInstanceOf(InternalServerErrorException);
      expect(err.message).toBe('Error generating one or more DIDs')
    }
  });
});
