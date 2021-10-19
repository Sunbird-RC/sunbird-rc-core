# Installation

## Prerequisites

> This guide assumes a some familiarity with basic linux commands. If not,
> [here](https://ubuntu.com/tutorials/command-line-for-beginners#1-overview) is
> a great place to start.

> Don't copy-paste the `$` signs, they indicate that what follows is a terminal
> command

### Terminal emulator

Linux and MacOS will have a terminal installed already. For Windows, it is
recommended that you use `git-bash`, which you can install from
[here](https://git-scm.com/download/win).

Type `echo Hi` in the terminal once it is installed. If installed correctly, you
should see `Hi` appear when you hit enter.

### Git

Installation instructions for Git can be found
[here](https://github.com/git-guides/install-git).

Run `git --version` in the terminal if `git` has been installed correctly:

```sh
$ git --version
git version 2.33.0
```

### NodeJS

Installation instructions for NodeJS can be found
[here](https://nodejs.org/en/download/package-manager/).

Run `node -v` in the terminal if `node` has been installed correctly:

```sh
$ node -v
v16.11.0
```

### Docker

Installation instructions for Docker can be found
[here](https://docs.docker.com/engine/install/).

Run `docker -v` in terminal to check if `docker` has been installed correctly:

```sh
$ docker -v
Docker version 20.10.9, build c2ea9bc90b
```

### Docker Compose

Installation instructions can be found
[here](https://docs.docker.com/engine/install/).

Run `docker-compose -v` in terminal to check if `docker-compose` has been
installed correctly:

```sh
$ docker-compose -v
Docker Compose version 2.0.1
```

## Installing the CLI

To install the CLI, run:

```sh
$ npm install --global registry-cli
```

> In case you encounter a permission denied/access denied error here, prefix the
> command with `sudo`: `sudo npm install --global registry-cli`.

To check if the Registry CLI has been installed correctly, run:

```
$ registry --help
```

This should show you all the commands you can execute using the CLI.

## Next Steps

Now that you have the Registry CLI running, you can go through the
[getting started guide](https://github.com/gamemaker1/registry-cli/blob/main/docs/getting-started.md).
