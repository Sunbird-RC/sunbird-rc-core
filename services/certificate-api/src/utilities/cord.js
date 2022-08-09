// src/utilities/cord.js
// CORD-related functions.

import * as cord from '@cord.network/sdk'

import { CORD_CHAIN_ADDRESS } from '../../config/index.js'

// First, connect to the chain.
await cord.init({ address: CORD_CHAIN_ADDRESS })

// Retrieve the identities used to create schemas and credentials.
// TODO: `Alice` works, but a newly created account doesn't. Why?
const signer = cord.Identity.buildFromURI('//Alice', {
	signingKeyPairType: 'sr25519',
})
// Create the schema and register it once `createClient` is called.
const schemaContent = {
	"$schema": "http://json-schema.org/draft-07/schema#",
	"title": "Basic Demo",
	"description": "Test Demo Schema",
	"$metadata": {
		"version": "1.0.0",
		"discoverable": true
	},
	"properties": {
		"name": {
			"type": "string"
		},
		"age": {
			"type": "integer"
		},
		"gender": {
			"type": "string"
		},
		"credit": {
			"type": "integer"
		}
	},
	"type": "object"
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
	// TODO: Make this `holderId`, but that would need all the holders to be
	// registered on the chain.
	const holder = cord.Identity.buildFromURI(`//Bob`, {
		signingKeyPairType: 'sr25519',
	})

	// Save the certificate data as a stream.
	const streamContent = cord.ContentStream.fromContent(
		cord.Content.fromSchemaAndContent(
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
