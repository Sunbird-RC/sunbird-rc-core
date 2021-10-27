// Fancy output

import Chalk from 'chalk'

const data = (...text: (string | number | boolean | object)[]) => {
	console.info(' ', ...text)
}
const info = (...text: (string | number | boolean | object)[]) => {
	console.info(Chalk.cyan(':'), ...text)
}
const success = (...text: (string | number | boolean | object)[]) => {
	console.info(Chalk.green('>'), ...text)
}
const warn = (...text: (string | number | boolean | object)[]) => {
	console.warn(Chalk.yellow('='), ...text)
}
const error = (...text: (string | number | boolean | object)[]) => {
	console.error(Chalk.red('!'), ...text)
}

// Export everything as one object
export default {
	data,
	info,
	success,
	warn,
	error,
}
