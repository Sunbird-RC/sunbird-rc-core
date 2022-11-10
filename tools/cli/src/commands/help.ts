// @/commands/help
// `registry help` command
// Show the user the help menu

import { Toolbox } from '../types'

export default {
	name: 'help',
	run: async (toolbox: Toolbox) => {
		const { meta, print } = toolbox

		// prettier-ignore
		print.info(`
${print.colors.green(print.colors.bold('registry-cli'))} ${print.colors.cyan(meta.version())}

${print.colors.bold('Usage')}
  ${print.colors.dim('>')} ${print.colors.green('registry')} ${print.colors.yellow('<command>')} ${print.colors.cyan('[options...]')}

${print.colors.bold('Commands')}
  ${print.colors.dim('>')} ${print.colors.yellow('init')}
    Creates a new registry instance
  ${print.colors.dim('>')} ${print.colors.yellow('status')}
    Shows status of a registry and its containers
  ${print.colors.dim('>')} ${print.colors.yellow('start')}
    Starts all the containers of a registry instance
  ${print.colors.dim('>')} ${print.colors.yellow('restart')}
    Restart all the containers in a registry instance
  ${print.colors.dim('>')} ${print.colors.yellow('down')}
    Stop and remove resources
  ${print.colors.dim('>')} ${print.colors.yellow('health')}
    Shows health check of a registry
  ${print.colors.dim('>')} ${print.colors.yellow('version')}
    Shows the current version of the CLI
    `)
	},
}
