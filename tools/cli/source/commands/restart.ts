// Restart registry related containers

import Chalk from 'chalk'
import spin from 'ora'
import doesCommandExist from 'command-exists'

import EnquirerPackage from 'enquirer'
const { prompt } = EnquirerPackage

import * as Docker from '../helpers/docker'

export default async () => {
	let spinner = spin('Checking environment...').start()
	// Check for docker
	if (!(await doesCommandExist('docker'))) {
		spinner.fail(
			Chalk.red(
				'Could not find `docker` installed on your system. Please install docker from https://docs.docker.com/engine/install/ before running this command.'
			)
		)
		process.exit(1)
	}

	// List running containers
	const runningContainers = await Docker.listContainers().catch(
		(error: Error) => {
			spinner.fail(Chalk.red(`Failed to list containers: ${error.message}`))
			process.exit(1)
		}
	)

	spinner.stop()
	const { containersToRestart } = (await prompt({
		type: 'multiselect',
		name: 'containersToRestart',
		message: Chalk.reset('Choose the containers to restart'),
		choices: runningContainers.map(
			(container) =>
				`${Chalk.yellow(container.id)}: ${Chalk.magenta(
					container.name
				)} - ${Chalk.cyan(container.image)}`
		),
		// @ts-expect-error -- Weird typings
		onSubmit() {
			// @ts-expect-error -- Weird typings
			if (this.selected.length === 0) {
				// @ts-expect-error -- Weird typings
				this.enable(this.focused)
			}
		},
	})) as { containersToRestart: string[] }
	spinner = spin('Restarting containers...').start()

	for (const container of runningContainers) {
		if (
			containersToRestart.some((containerName) =>
				containerName.includes(container.name)
			)
		) {
			spinner.text = `Restarting ${Chalk.yellow(container.id)} (${Chalk.magenta(
				container.name
			)} - ${Chalk.cyan(container.image)})...`
			await Docker.restartContainer(container.id)

			spinner.succeed(
				`Restarted ${Chalk.yellow(container.id)} (${Chalk.magenta(
					container.name
				)} - ${Chalk.cyan(container.image)})`
			)
			spinner = spin('Restarting containers...').start()
		}
	}
	// Once the restart succeeds, wait 20 seconds for the containers to complete startup
	spinner.text = 'Waiting for containers to start...'
	await new Promise<void>((resolve) => {
		setTimeout(resolve, 20000)
	})

	spinner.stop()
	process.exit(0)
}
