import { z } from 'zod';

const ContextValidator = z.array(z.string().or(z.record(z.any())));
export const ImageValidator = z.string().or(
  z.object({
    id: z.string(),
    type: z.string(),
    caption: z.string().optional(),
  }),
);
export const GeoCoordinatesValidator = z.object({
  type: z.string().min(1).or(z.string().array().nonempty()),
  latitude: z.number(),
  longitude: z.number(),
});
export const AddressValidator = z.object({
  type: z.string().min(1).or(z.string().array().nonempty()),
  addressCountry: z.string().optional(),
  addressCountryCode: z.string().optional(),
  addressRegion: z.string().optional(),
  addressLocality: z.string().optional(),
  streetAddress: z.string().optional(),
  postOfficeBoxNumber: z.string().optional(),
  postalCode: z.string().optional(),
  geo: GeoCoordinatesValidator.optional(),
});
export const IdentifierTypeValidator = z
  .enum([
    'sourcedId',
    'systemId',
    'productId',
    'userName',
    'accountId',
    'emailAddress',
    'nationalIdentityNumber',
    'isbn',
    'issn',
    'lisSourcedId',
    'oneRosterSourcedId',
    'sisSourcedId',
    'ltiContextId',
    'ltiDeploymentId',
    'ltiToolId',
    'ltiPlatformId',
    'ltiUserId',
    'identifier',
  ])
  .or(z.string());
export const IdentifierEntryValidator = z.object({
  type: z.string().min(1).or(z.string().array().nonempty()),
  identifier: z.string(),
  identifierType: IdentifierTypeValidator,
});
const ProfileValidator = z.string().or(
  z
    .object({
      id: z.string().optional(),
      type: z.string().or(z.string().array().nonempty().optional()),
      name: z.string().optional(),
      url: z.string().optional(),
      phone: z.string().optional(),
      description: z.string().optional(),
      endorsement: z.any().array().optional(), // Recursive type
      image: ImageValidator.optional(),
      email: z.string().email().optional(),
      address: AddressValidator.optional(),
      otherIdentifier: IdentifierEntryValidator.array().optional(),
      official: z.string().optional(),
      parentOrg: z.any().optional(), // Recursive types are annoying =(
      familyName: z.string().optional(),
      givenName: z.string().optional(),
      additionalName: z.string().optional(),
      patronymicName: z.string().optional(),
      honorificPrefix: z.string().optional(),
      honorificSuffix: z.string().optional(),
      familyNamePrefix: z.string().optional(),
      dateOfBirth: z.string().optional(),
    })
    .catchall(z.any()),
);
export const CredentialSubjectValidator = z
  .object({ id: z.string().optional() })
  .catchall(z.any());
export const CredentialStatusValidator = z.object({
  type: z.string(),
  id: z.string(),
});
export const CredentialSchemaValidator = z.object({
  id: z.string(),
  type: z.string(),
});
export const RefreshServiceValidator = z
  .object({ id: z.string(), type: z.string() })
  .catchall(z.any());
export const UnsignedVCValidator = z
  .object({
    '@context': ContextValidator,
    id: z.string().optional(),
    type: z.string().array().nonempty(),
    issuer: ProfileValidator,
    issuanceDate: z.string(),
    expirationDate: z.string().optional(),
    credentialSubject: CredentialSubjectValidator.or(
      CredentialSubjectValidator.array(),
    ),
    credentialStatus: CredentialStatusValidator.optional(),
    credentialSchema: CredentialSchemaValidator.array().optional(),
    refreshService: RefreshServiceValidator.optional(),
  })
  .catchall(z.any());
export const ProofValidator = z
  .object({
    type: z.string(),
    created: z.string(),
    challenge: z.string().optional(),
    domain: z.string().optional(),
    nonce: z.string().optional(),
    proofPurpose: z.string(),
    verificationMethod: z.string(),
    jws: z.string().optional(),
  })
  .catchall(z.any());
export const VCValidator = UnsignedVCValidator.extend({
  proof: ProofValidator.or(ProofValidator.array()),
});
