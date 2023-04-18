import { Injectable, NotFoundException } from '@nestjs/common';
import * as ION from '@decentralized-identity/ion-tools';
import { PrismaService } from 'src/prisma.service';
import { Identity, Prisma } from '@prisma/client';
import { DIDDocument } from 'did-resolver';
import { domainToASCII } from 'url';
import { uuid } from 'uuidv4';
import { GenerateDidDTO } from './dtos/GenerateDid.dto';
import { VaultService } from './vault.service';

@Injectable()
export class DidService {
  constructor(private prisma: PrismaService, private vault: VaultService) {}

  async generateDID(doc: GenerateDidDTO): Promise<DIDDocument> {
    // Create private/public key pair
    const authnKeys = await ION.generateKeyPair('Ed25519');
    console.log(authnKeys);

    // Create a UUID for the DID using uuidv4
    const didUri = 'did:ulp:' + uuid();

    // Create a DID Document
    const document: DIDDocument = {
      '@context': 'https://w3id.org/did/v1',
      id: didUri,
      alsoKnownAs: doc.alsoKnownAs,
      service: doc.service,
      verificationMethod: [
        {
          id: 'auth-key',
          type: 'Ed25519VerificationKey2020',
          publicKeyJwk: authnKeys.publicJwk,
          controller: didUri,
        },
      ],
      authentication: ['auth-key'],
    };
    // print the document using pretty json
    console.log(JSON.stringify(document, null, 2));

    await this.prisma.identity.create({
      data: {
        id: didUri,
        didDoc: JSON.stringify(document),
      },
    });
    this.vault.writePvtKey(authnKeys.privateJwk, didUri)
    return document;

    // const did = new ION.DID({
    //   content: content,
    // });

    // const anchorRequestBody = await did.generateRequest();
    // const didUri: string = await did.getURI('short');
    // const anchorRequest = new ION.AnchorRequest(anchorRequestBody);
    // const anchorResponse = await anchorRequest.submit();
    // if (anchorResponse) {
    //   await this.prisma.identity.create({
    //     data: {
    //       id: didUri,
    //       didDoc: anchorResponse as DIDDocument,
    //       privateKey: authnKeys.privateJwk,
    //     },
    //   });

    //   return anchorResponse;
    // } else {
    //   throw new Error('err');
    // }
  }

  async resolveDID(id: string): Promise<any> {
    console.log('did in resolveDID: ', id);
    const artifact = await this.prisma.identity.findUnique({
      where: { id },
    });
    console.log('artifact.didDoc: ', artifact.didDoc);
    if (artifact) {
      return JSON.parse(artifact.didDoc as string) as DIDDocument;
    } else {
      return null;
    }
  }

  // async resolveDID(id: string): Promise<DIDDocument> {
  //   // check on the blockchain and update status
  //   // URI: https://identity.foundation/ion/explorer/?did={DID}
  //   try {
  //     // check in db
  //     const artifact = await this.prisma.identity.findUnique({
  //       where: { id },
  //     });

  //     if (artifact) {
  //       if (!artifact.blockchainStatus) {
  //         try {
  //           const response = await ION.resolve(id);
  //           console.log(JSON.stringify(response));
  //           this.prisma.identity.update({
  //             where: {
  //               id,
  //             },
  //             data: {
  //               blockchainStatus: true,
  //             },
  //           });
  //         } catch (err) {
  //           console.log('not updated on blockchain');
  //         }
  //         return artifact.didDoc as DIDDocument;
  //       }
  //     } else {
  //       throw new Error('Not Found');
  //     }
  //   } catch (err) {
  //     throw new NotFoundException(`${id} not found`);
  //   }
  // }

  // async updateDID(id: string, data: any) {
  //   let artifact: Identity | null = null;
  //   try {
  //     artifact = await this.prisma.identity.findUnique({
  //       where: {
  //         id,
  //       },
  //     });
  //     console.log(
  //       'artifact: ',
  //       JSON.parse(artifact.didDoc as string)?.didDocument
  //         ?.verificationMethod[0].publicKeyJwk,
  //     );

  //     const content = data.content;
  //     content['publicKeys'] = [
  //       {
  //         id: 'auth-key',
  //         type: 'EcdsaSecp256k1VerificationKey2019',
  //         publicKeyJwk: JSON.parse(artifact.didDoc as string)?.didDocument
  //           ?.verificationMethod[0].publicKeyJwk,
  //         purposes: ['authentication'],
  //       },
  //     ];

  //     const did = new ION.DID({
  //       content: content,
  //     });

  //     console.log('did: ', did);

  //     const updateOperation = await did.generateOperation('update', {
  //       addServices: content.services,
  //     });

  //     console.log('updateOperation: ', updateOperation);
  //     const updateRequestBody = await did.generateRequest(1, updateOperation);
  //     const updateRequest = new ION.AnchorRequest(updateRequestBody);
  //     console.log('updateRequestBody: ', updateRequestBody);

  //     const updateResponse = await updateRequest.submit();

  //     console.log('updateResponse: ', updateResponse);

  //     // if (updateResponse) {
  //     return 'Successfully Updated';
  //   } catch (err) {
  //     throw new Error(err);
  //   }
  //   // }
  //   // throw new Error('Not Registered');
  // }
}
