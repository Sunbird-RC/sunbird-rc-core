# Creating A Plugin

The Registry CLI is built using the Gluegun framework, which allows you to
create plugins that the user can install and use as part of the Registry CLI.
Plugins can be used to add new commands and more functionality.

To create a plugin for the CLI, first
[prepare your environment](../../contributing.md#prepare-your-environment) and
then follow
[Gluegun's official tutorial on creating a plugin](https://github.com/infinitered/gluegun/blob/master/docs/tutorial-making-a-plugin.md).

Once you have created the plugin and published it to NPM (the package name must
be of the following format: `registry-cli-<plugin-name>`), users of the CLI can
install it by running the following:

```sh
$ npm install --global registry-cli-<plugin-name>
```
