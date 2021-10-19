#!/usr/bin/env -S node --no-warnings --es-module-specifier-resolution node
// Entrypoint for CLI

import { Command } from 'commander'

import createRegistryInstance from './commands/init'
import restartContainers from './commands/restart'
import stopContainers from './commands/stop'
import startContainers from './commands/start'
import showContainerStatus from './commands/status'
import showOrChangeConfig from './commands/config'
import PackageMetadata from '../package.json'

// Create the command-and-option parser
const program = new Command()

// Set the name and version
program
	.name('registry')
	.version(
		PackageMetadata.version,
		'-v, --version',
		'view the current version of the CLI'
	)

// Register all commands
program
	.command('init')
	.description('create a new registry instance in the current directory')
	.action(createRegistryInstance)
program
	.command('restart')
	.description('restarts registry-related containers')
	.action(restartContainers)
program
	.command('stop')
	.description('stops registry-related containers')
	.action(stopContainers)
program
	.command('start')
	.description('starts registry-related containers')
	.action(startContainers)
program
	.command('status')
	.description('shows the status of registry-related containers')
	.action(showContainerStatus)
program
	.command('config')
	.argument('[variable]', 'config variable to retrieve', 'all')
	.argument('[value]', 'value to set on the mentioned config variable')
	.description('shows/changes the current configuration')
	.action(showOrChangeConfig)

// Parse
program.parseAsync()
