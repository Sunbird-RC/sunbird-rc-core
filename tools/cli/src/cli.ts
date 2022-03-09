// @/cli
// Create and run the CLI

import path from 'path'

import { system } from 'gluegun'
import { build } from 'gluegun'
import createNotifier from 'update-notifier'

import packageMetadata from '../package.json'

// Create the CLI using gluegun
async function run(argv: unknown[]) {
	// Check for updates
	const updateNotifier = createNotifier({
		pkg: packageMetadata, // package.json metadata
		updateCheckInterval: 1000 * 60 * 60, // Every hour
	})
	// Show a notification just before the process exits
	updateNotifier.notify()

	// Get path to global node modules
	const globalModules = path.resolve(
		(await system.run('npm config get prefix')) +
			(process.platform !== 'win32' ? '/lib' : '') +
			'/node_modules'
	)

	// Create a CLI runtime
	const cli = build()
		.brand('registry-cli')
		.src(__dirname)
		// Accept plugins that are globally installed
		.plugins(globalModules, { matching: 'registry-cli-*' })
		.create()

	// Run run run!
	const toolbox = await cli.run(argv)

	// Return the toolbox instance (for testing, in case we add it)
	return toolbox
}

export default { run }
