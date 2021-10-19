// Show/edit the current configuration

import Chalk from 'chalk'

import Config from '../utils/config'
import Print from '../utils/print'

const recursivelyPrint = (objectToPrint: any, namespace?: string) => {
	for (const [name, value] of Object.entries(objectToPrint)) {
		if (typeof value === 'object' && !Array.isArray(value)) {
			recursivelyPrint(
				value,
				Chalk.yellow(`${namespace ? `${namespace}.` : ''}${name}.`)
			)
		} else if (typeof value === 'object' && Array.isArray(value)) {
			Print.data(
				`${namespace ?? ''}${Chalk.yellow(name)}: ${Chalk.magenta(
					value.join(', ')
				)}`
			)
		} else {
			Print.data(
				`${namespace ?? ''}${Chalk.yellow(name)}: ${Chalk.magenta(value)}`
			)
		}
	}
}

export default async (variableToRetrieve: string, valueToSet?: string) => {
	// If the variables to get is all, show all
	if (variableToRetrieve === 'all') {
		Print.success(Chalk.bold.underline('CLI configuration'))
		recursivelyPrint(Config.store)
		process.exit(0)
	}

	// If only the variable name is passed, return it's current value
	if (variableToRetrieve && !valueToSet) {
		const variableValue = Config.get(variableToRetrieve)
		if (!variableValue) {
			Print.error(
				Chalk.red(`${Chalk.yellow(variableToRetrieve)} does not exist.`)
			)
			process.exit(1)
		}

		if (typeof variableValue === 'object') {
			recursivelyPrint(variableValue)
		} else {
			Print.data(
				`${Chalk.yellow(variableToRetrieve)}: ${Chalk.magenta(variableValue)}`
			)
		}

		process.exit(0)
	}

	// If the variable name and value are passed, set it
	if (variableToRetrieve && valueToSet) {
		try {
			Config.set(variableToRetrieve, valueToSet)
		} catch (errorObject) {
			const error = errorObject as Error
			Print.error(
				Chalk.red(
					`Cannot set ${Chalk.yellow(variableToRetrieve)}: ${error.message}`
				)
			)
			process.exit(1)
		}

		Print.data(
			`Set ${Chalk.yellow(variableToRetrieve)} to ${Chalk.magenta(valueToSet)}`
		)

		process.exit(0)
	}
}
