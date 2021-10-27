// Show the status of registry related containers

import Chalk from 'chalk'
import spin from 'ora'
import doesCommandExist from 'command-exists'

import * as Docker from '../helpers/docker'
import Print from '../utils/print'

export default async () => {
	const spinner = spin('Checking environment...').start()
	// Check for docker
	if (!(await doesCommandExist('docker'))) {
		spinner.fail(
			Chalk.red(
				'Could not find `docker` installed on your system. Please install docker from https://docs.docker.com/engine/install/ before all this command.'
			)
		)
		process.exit(1)
	}

	// List all containers
	spinner.text = 'Listing containers...'
	const allContainers = await Docker.listContainers({
		all: true,
	}).catch((error: Error) => {
		spinner.fail(Chalk.red(`Failed to list containers: ${error.message}`))
		process.exit(1)
	})
	spinner.stop()

	Print.success(Chalk.bold.underline('Registry containers'))
	for (const container of allContainers) {
		Print.data(
			`${Chalk.yellow(container.id)}: ${Chalk.magenta(
				container.name
			)} - ${Chalk.blue(container.status)} - ${container.ports
				.map((port) => Chalk.green(port))
				.join(', ')} [${Chalk.cyan(container.image)}]`
		)
	}

	process.exit(0)
}
