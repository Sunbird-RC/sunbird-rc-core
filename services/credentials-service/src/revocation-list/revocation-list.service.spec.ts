import { RevocationListService } from './revocation-list.service';

describe('RevocationListService', () => {
  const issuer = 'did:rcw:issuer-1';

  const makeMockRL = () => ({
    setRevoked: jest.fn(),
    encode: jest.fn().mockResolvedValue('encoded-list-data'),
  });

  const makeMockRLImpl = (rl?: any) => ({
    createList: jest.fn().mockResolvedValue(rl || makeMockRL()),
    decodeList: jest.fn().mockResolvedValue(rl || makeMockRL()),
  });

  const makeMockIdentity = () => ({
    generateDID: jest.fn().mockResolvedValue([{ id: 'did:rcw:rl-list-1' }]),
    signVC: jest.fn().mockResolvedValue({ proof: { type: 'Ed25519Signature2020', proofValue: 'sig' } }),
  });

  const makeMockPrisma = (overrides: {
    revocationListInfo?: any;
    verifiableCred?: any;
  } = {}) => ({
    revocationLists: {
      findUnique: jest.fn().mockResolvedValue(overrides.revocationListInfo || null),
      create: jest.fn().mockResolvedValue({}),
      update: jest.fn().mockResolvedValue({}),
    },
    verifiableCredentials: {
      create: jest.fn().mockResolvedValue({}),
      findUnique: jest.fn().mockResolvedValue(overrides.verifiableCred || null),
      update: jest.fn().mockResolvedValue({}),
    },
    $transaction: jest.fn().mockImplementation((ops: any[]) => Promise.all(ops)),
  });

  describe('createNewRevocationList', () => {
    it('generates DID, creates bitstring, signs, and persists to DB (first list for issuer)', async () => {
      const identity = makeMockIdentity();
      const rlImpl = makeMockRLImpl();
      const prisma = makeMockPrisma();

      const service = new RevocationListService(prisma as any, rlImpl as any, identity as any);
      const result = await service.createNewRevocationList(issuer);

      expect(identity.generateDID).toHaveBeenCalledWith(['verifiable credential']);
      expect(rlImpl.createList).toHaveBeenCalledWith({ length: 100000 });
      expect(identity.signVC).toHaveBeenCalled();
      expect(prisma.$transaction).toHaveBeenCalled();
      expect(result.id).toBe('did:rcw:rl-list-1');
      expect(result.type).toContain('RevocationList2020Credential');
    });

    it('updates existing revocation list record when issuer already has one', async () => {
      const identity = makeMockIdentity();
      const rlImpl = makeMockRLImpl();
      const prisma = makeMockPrisma({
        revocationListInfo: { issuer, allRevocationLists: ['old-list-id'] },
      });

      const service = new RevocationListService(prisma as any, rlImpl as any, identity as any);
      await service.createNewRevocationList(issuer);

      expect(prisma.revocationLists.update).toHaveBeenCalled();
    });

    it('throws when DID generation fails', async () => {
      const identity = makeMockIdentity();
      identity.generateDID.mockRejectedValue(new Error('did gen fail'));
      const prisma = makeMockPrisma();
      const rlImpl = makeMockRLImpl();

      const service = new RevocationListService(prisma as any, rlImpl as any, identity as any);
      await expect(service.createNewRevocationList(issuer)).rejects.toThrow('Error generating DID');
    });

    it('throws when DB save fails', async () => {
      const identity = makeMockIdentity();
      const rlImpl = makeMockRLImpl();
      const prisma = makeMockPrisma();
      prisma.$transaction.mockRejectedValue(new Error('db fail'));

      const service = new RevocationListService(prisma as any, rlImpl as any, identity as any);
      await expect(service.createNewRevocationList(issuer)).rejects.toThrow('Error saving');
    });
  });

  describe('updateRevocationList', () => {
    it('decodes list, sets bit, re-signs, and updates proof in DB', async () => {
      const rl = makeMockRL();
      const rlImpl = makeMockRLImpl(rl);
      const identity = makeMockIdentity();
      const prisma = makeMockPrisma({
        revocationListInfo: {
          latestRevocationListId: 'list-1',
          lastCredentialIdx: 10,
        },
        verifiableCred: {
          id: 'list-1',
          subject: { encodedList: 'old-encoded' },
        },
      });

      const service = new RevocationListService(prisma as any, rlImpl as any, identity as any);
      const result = await service.updateRevocationList(issuer, 5);

      expect(rl.setRevoked).toHaveBeenCalledWith(5, true);
      expect(rl.encode).toHaveBeenCalled();
      expect(identity.signVC).toHaveBeenCalled();
      expect(prisma.verifiableCredentials.update).toHaveBeenCalled();
      expect(result).toBe('list-1');
    });

    it('creates a new list when lastCredentialIdx >= 100000 (rollover)', async () => {
      const rl = makeMockRL();
      const rlImpl = makeMockRLImpl(rl);
      const identity = makeMockIdentity();
      const prisma = makeMockPrisma({
        revocationListInfo: {
          latestRevocationListId: 'list-1',
          lastCredentialIdx: 100000,
        },
        verifiableCred: {
          id: 'list-1',
          subject: { encodedList: 'old-encoded' },
        },
      });

      // Stub createNewRolledList to avoid deep DB mocking in rollover path
      const service = new RevocationListService(prisma as any, rlImpl as any, identity as any);
      const createSpy = jest.spyOn(service, 'createNewRevocationList').mockResolvedValue({ id: 'new-list-id' } as any);
      const result = await service.updateRevocationList(issuer, 5);

      expect(createSpy).toHaveBeenCalledWith(issuer);
      expect(result).toBe('new-list-id');
    });

    it('throws when DB fetch fails', async () => {
      const prisma = makeMockPrisma();
      prisma.revocationLists.findUnique.mockRejectedValue(new Error('db fail'));

      const service = new RevocationListService(prisma as any, makeMockRLImpl() as any, makeMockIdentity() as any);
      await expect(service.updateRevocationList(issuer, 0)).rejects.toThrow('Error fetching revocation list');
    });

    it('throws when bit decode/update fails', async () => {
      const rlImpl = makeMockRLImpl();
      rlImpl.decodeList.mockRejectedValue(new Error('decode fail'));
      const prisma = makeMockPrisma({
        revocationListInfo: { latestRevocationListId: 'list-1', lastCredentialIdx: 0 },
        verifiableCred: { id: 'list-1', subject: { encodedList: 'bad' } },
      });

      const service = new RevocationListService(prisma as any, rlImpl as any, makeMockIdentity() as any);
      await expect(service.updateRevocationList(issuer, 0)).rejects.toThrow();
    });
  });

  describe('getDecodedRevocationString', () => {
    it('fetches credential and decodes the bitstring', async () => {
      const rl = makeMockRL();
      const rlImpl = makeMockRLImpl(rl);
      const prisma = makeMockPrisma({
        verifiableCred: {
          id: 'list-1',
          subject: { encodedList: 'encoded-data' },
        },
      });

      const service = new RevocationListService(prisma as any, rlImpl as any, makeMockIdentity() as any);
      const result = await service.getDecodedRevocationString('list-1');

      expect(rlImpl.decodeList).toHaveBeenCalledWith({ encodedList: 'encoded-data' });
      expect(result).toBe(rl);
    });

    it('throws on DB error', async () => {
      const prisma = makeMockPrisma();
      prisma.verifiableCredentials.findUnique.mockRejectedValue(new Error('not found'));

      const service = new RevocationListService(prisma as any, makeMockRLImpl() as any, makeMockIdentity() as any);
      await expect(service.getDecodedRevocationString('bad-id')).rejects.toThrow('Error fetching');
    });
  });
});
