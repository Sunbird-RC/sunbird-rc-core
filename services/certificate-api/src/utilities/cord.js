// src/utilities/cord.js
// CORD-related functions.

import * as cord from '@cord.network/sdk'

import { CORD_CHAIN_ADDRESS } from '../../config/index.js'

// First, connect to the chain.
await cord.init({ address: CORD_CHAIN_ADDRESS })

// Retrieve the identities used to create schemas and credentials.
const signer = cord.Identity.buildFromURI('//SA', {
	signingKeyPairType: 'sr25519',
})
// Create the schema and register it once `createClient` is called.
const schemaContent = {
	$schema: 'http://json-schema.org/draft-07/schema#',
	title: 'Sunbird RC VC Schema',
	description: 'Schema for signed VCs.',
	$metadata: {
		version: '1.0.0',
		discoverable: true
	},
	properties: {},
	type: 'object',
	additionalProperties: true
}
const spaceContent = {
	title: 'Signed VC Space',
	description: 'Space for signed VCs.',
}
let createdSpace, createdSchema

/**
 * Initialize a client to connect to the chain.
 */
export const createClient = async () => {
	let extrinsic

	// Create a new space.
  createdSpace = cord.Space.fromSpaceProperties(spaceContent, signer)
	extrinsic = await cord.Space.create(createdSpace)
	await cord.Chain.signAndSubmitTx(extrinsic, signer, {
		resolveOn: cord.Chain.IS_IN_BLOCK,
		rejectOn: cord.Chain.IS_ERROR,
	})

	// Register the schema for the credentials.
	createdSchema = cord.Schema.fromSchemaProperties(schemaContent, signer, createdSpace.identifier)
	extrinsic = await cord.Schema.create(createdSchema)
	await cord.Chain.signAndSubmitTx(extrinsic, signer, {
		resolveOn: cord.Chain.IS_IN_BLOCK,
		rejectOn: cord.Chain.IS_ERROR,
	})
}

/**
 * Saves the certificate as a VC on the CORD blockchain.
 *
 * @param {object} certificate - The JSON data of the certificate.
 * @param {string} holderId - The ID of the holder of the certificate.
 *
 * @returns {Promise<stream>} - The saved stream on the chain.
 */
export const createCredential = async (certificate, holderId) => {
	// Get the identity of the holder.
	const holder = cord.Identity.buildFromURI(`//${holderId}`, {
		signingKeyPairType: 'sr25519',
	})

	// Save the certificate data as a stream.
	const streamContent = cord.ContentStream.fromContent(
		content.fromSchemaAndContent(
			createdSchema,
			certificate,
			signer.address,
			holder.address,
		),
		signer,
	)
	const createdStream = cord.Stream.fromContentStream(streamContent)
	const extrinsic = await cord.Stream.create(createdStream)

	await cord.Chain.signAndSubmitTx(extrinsic, signer, {
		resolveOn: cord.Chain.IS_IN_BLOCK,
		rejectOn: cord.Chain.IS_ERROR,
	})

	return createdStream
}
