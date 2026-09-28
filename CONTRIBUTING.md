<!-- omit in toc -->
# Sunbird RC Contributing Guide

First off, thanks for taking the time to contribute! ❤️

This repository is **Sunbird Registry and Credentials (RC)** — a framework for building electronic registries, attestation flows and verifiable credentialling.

It's a polyglot monorepo. The registry core is Java 11 built with Maven, under `java/`. Around it sit independent services under `services/` — identity, credential schema, credentials, OID4VC, notification, metrics, ID generation and encryption. Everything is built into container images and wired together with Docker Compose, so **Docker is required for almost any work here**, including running the tests.

<!-- omit in toc -->
## Table of Contents

<!-- - [Code of Conduct](#code-of-conduct) -->
- [I Have a Question](#i-have-a-question)
- [I Want To Contribute](#i-want-to-contribute)
  - [Before You Start](#before-you-start)
  - [Reporting Bugs](#reporting-bugs)
  - [Suggesting Enhancements](#suggesting-enhancements)
  - [Your First Code Contribution](#your-first-code-contribution)
  - [Improving The Documentation](#improving-the-documentation)
- [Contribution Standards](#contribution-standards)
  - [Using AI Tools](#using-ai-tools)
- [Styleguides](#styleguides)
- [Submitting a Pull Request](#submitting-a-pull-request)
- [What Happens After You Submit](#what-happens-after-you-submit)

<!-- ## Code of Conduct

This project and everyone participating in it is governed by the [Code of Conduct](CODE_OF_CONDUCT.md). By participating, you are expected to uphold it. Report unacceptable behaviour to TODO_CONTACT_EMAIL.

Before uncommenting: add CODE_OF_CONDUCT.md to this repository and replace TODO_CONTACT_EMAIL. -->


## I Have a Question

Start with the [documentation site](https://rc.sunbird.org/) and the [installation guide](https://rc.sunbird.org/use/getting-started/installation). Then search existing [Issues](https://github.com/Sunbird-RC/sunbird-rc-core/issues) and the [community discussions](https://github.com/Sunbird-RC/community/discussions).

If you still need help, open a thread in [Discussions](https://github.com/Sunbird-RC/community/discussions). Include your JDK version, Docker and Docker Compose versions, which services you were running, and the exact error.

## I Want To Contribute

> When contributing to this project, you must agree that you have authored 100% of the content, that you have the necessary rights to the content, and that the content you contribute may be provided under the project licence.

### Before You Start

Create a copy of, and fill in, the [**Sunbird Contribution Checklist**](https://docs.google.com/spreadsheets/d/1k0x3NEBvQAAEAm6WZzG3RqmzNnX9U8ywABjc4pKfttg/edit?usp=sharing). Share the filled-in copy with the maintainer so you're aligned before you write code.

**When it's needed:** for features, bug fixes with code changes, and anything touching APIs, schemas, credential formats or configuration. Small documentation edits and typo fixes don't need one — open the PR and describe what you changed.

### Reporting Bugs

Before reporting, check the [documentation](https://rc.sunbird.org/) and search the [issue tracker](https://github.com/Sunbird-RC/sunbird-rc-core/issues?q=label%3Abug).

**Report it in the right place.** This repository is the registry core and the services in this tree. Problems in a separate Sunbird RC repository belong in that repository, and problems in a third-party component — Keycloak, PostgreSQL, Elasticsearch, Kafka, ClickHouse — belong upstream unless the fault is in how we configure or call it.

> Never report security issues publicly. Use the **Security → Report a vulnerability** tab, and give maintainers a reasonable window to fix and release before disclosing.

[Open a bug report](https://github.com/Sunbird-RC/sunbird-rc-core/issues/new/choose). Say which service was involved, which Docker Compose configuration and environment file you used, and paste container logs — with credentials, tokens and keys redacted.

### Suggesting Enhancements

Sunbird RC is used to build registries in a range of contexts, so changes that work for adopters generally land more easily than ones serving a single deployment. Anything touching credential formats or attestation flows also has to respect the standards the project implements — say which specification your proposal follows.

[Open a feature request](https://github.com/Sunbird-RC/sunbird-rc-core/issues/new/choose) describing the problem it solves, which services it affects, and alternatives you've considered. **Agree the approach on the issue before writing significant code.**

### Your First Code Contribution

**1. Pick something and claim it.** Filter issues by `good first issue` and `help wanted`, then comment to say you're taking it.

**2. Fork, clone and build it.**

You'll need **JDK 11**, Docker and Docker Compose, `make`, and `curl`. Maven comes from the wrapper (`./mvnw`) — don't install it separately. A full build produces around ten container images, so allow plenty of disk space.

```bash
# Fork on GitHub, then:
git clone https://github.com/<your-username>/sunbird-rc-core.git
cd sunbird-rc-core
git remote add upstream https://github.com/Sunbird-RC/sunbird-rc-core.git

cp .env.example .env    # fill in values — .env is gitignored, never commit it

sh configure-dependencies.sh
make build
```

`make build` compiles the registry jar with `./mvnw clean install`, then builds images for the core and every service under `services/`. If you're only changing one service, building that service's image directly is far quicker than a full `make build`.

To run the stack, use `docker-compose-v1.yml` with one of the environment files under `test_environments/`, which switch on different combinations — native search, distributed definition manager, async create with Kafka, events and notifications. `make test` exercises two of them; a FusionAuth variant also exists but isn't covered by the test suite.

**There are two compose files, and they are not interchangeable.** `docker-compose-v1.yml` is the one `make test` and CI use, with the certificate-signer and certificate-api services. The plain `docker-compose.yml` is a different, Vault-based arrangement — it runs a Vault service, is driven by `setup_vault.sh` and the `compose-init` target, and wires the identity, credential-schema, credentials and OID4VC services to Vault instead. Unless you're specifically working on the Vault setup, use `docker-compose-v1.yml`.

Modern `docker compose` (v2) works with these files — you'll see a deprecation warning about the top-level `version:` key, which is harmless. CI still installs the end-of-life v1 binary.

If setup fails, the usual causes are a JDK other than 11, Docker running out of disk, a port already in use (8080 Keycloak, 8081 registry, 8070 notifications), or `.env` not filled in. **If the installation guide didn't work as written, open an issue** with your versions and the exact error — then consider fixing it.

**3. Branch.** Branch from the current working branch — `v2.1.1` at the time of writing; check the branch list, since it rolls each release. Don't branch from `main`. Keep the change to one logical unit.

**4. Code sanity.** Run the tests before you push:

```bash
make test
```

Be aware of what this does: it builds everything, then starts the full stack twice — once with distributed definition manager and native search, once with Kafka, events and notifications — and runs the end-to-end API suite against each. It needs Docker, free ports and real time. CI itself has to delete unused toolchains to make room for it.

Also:

- Tests for anything whose behaviour you changed. The end-to-end suite lives in `java/apitest`; services have their own tests under `services/<name>`.
- Match the surrounding code. The Java modules and the service modules have different conventions — follow whichever tree you're in rather than importing the other's style.
- **Never commit `.env`, credentials, tokens, private keys or certificates.** The compose files read secrets from `.env`, which is gitignored — keep it that way.
- Don't hardcode deployment-specific values such as hostnames, realm names or ports. They belong in environment configuration.
- If you add or change an API, update the schema and the documentation in the same change.

**Before opening the PR:**

- [ ] `make test` passes locally, or you've said in the PR which parts you couldn't run and why
- [ ] Issue linked, and you commented on it to say you're taking it
- [ ] Tests added or updated for the behaviour you changed
- [ ] Documentation updated — anything your change made wrong, plus API and configuration docs for anything you added
- [ ] Schema and architecture documentation added or updated using the [template](https://docs.google.com/document/d/1YqUzR09a5t_ebkMsCaW7juf1gZgXLudQlkYJF0jl4hY/edit?usp=sharing), if your change affects system design
- [ ] No `.env`, credentials, tokens, keys or certificates in the diff
- [ ] If you changed a service's API or its image, checked whether the compose files, environment files or dependent services need the same change
- [ ] Your filled-in copy of the Sunbird Contribution Checklist is complete, with details rather than just ticks, and ready to attach

### Improving The Documentation

Documentation fixes are real contributions and an ideal first one — whatever tripped you up during setup is a genuine bug.

- **Where:** most user-facing documentation lives on the [documentation site](https://rc.sunbird.org/) rather than in this repository. In-repository Markdown covers the build, the services and the compose setup.
- **Voice:** plain, direct, active. Write for someone competent who has never built a registry before.
- **Be exact about versions and commands.** A wrong JDK or Compose version here costs someone an afternoon.
- **Accessibility:** descriptive link text, alt text on images, real heading levels.
- Update the docs in the same change that made them wrong.

## Contribution Standards

Sunbird RC is a digital public good, used as national-scale infrastructure. That shapes what good code means here:

1. **Serve the public-good mission** — benefit adopters broadly, not one implementation's immediate need. Refer to the [DPG standard](https://www.digitalpublicgoods.net/standard).
2. **Uphold platform independence** — keep changes modular, and make any licensed component swappable by adopters.
3. **Protect privacy as policy, not just code** — registries hold personal data about real people. Never commit, hardcode or expose it, and think about what a change logs as well as what it stores.
4. **Do no harm by design** — an entry in a registry can determine whether someone receives a service. Check for bias, exclusion or barriers to access.
5. **Write for people outside your team** — Sunbird's value comes from adoption.
6. **Treat documentation as part of the contribution.**
7. **Follow the repository for mechanics** — the documentation site carries the detail; this repository's README is deliberately brief.

### Using AI Tools

Welcome, with conditions:

- **Understand what you submit.** If you can't explain and debug it in review, don't open the PR.
- **Attribute it.** Add `Assisted-by: <tool name>` for substantially AI-generated code — not `Co-authored-by:`, which implies a human contributor with authorship rights.
- **Licence hygiene applies.** Output must comply with the provider's terms, infringe nobody's IP, and must not include code under licences incompatible with MIT.
- **Tests and documentation are still required.**

## Styleguides

**Branches:** `<type>/<short-description>` — `feat/oid4vc-credential-offer`, `fix/keycloak-realm-import`, `docs/compose-prereqs`.

**Commits:** [Conventional Commits](https://www.conventionalcommits.org/) — `feat:`, `fix:`, `docs:`, `test:`, `refactor:`, `chore:`, `perf:`.

```bash
git fetch upstream
git checkout -b fix/keycloak-realm-import upstream/v2.1.1   # use the current working branch
git commit -m "fix: import realm before registry starts"
```

Keep commits small and atomic, and explain **why**, not just what.

## Submitting a Pull Request

Push your branch and open the PR against the current working branch of `Sunbird-RC/sunbird-rc-core` — `v2.1.1` at the time of writing. GitHub defaults the base branch to `main`, so you will need to change it.

The pull request template asks for what changed, why, how you tested it, and anything a reviewer should look at closely. Say which services you built and which test configurations you ran. Tag the reviewer or repo owner, and link the GitHub issue if applicable.

If you touched authentication, Keycloak configuration, encryption, signing, credential issuance or verification, or anything handling personal data in the registry, say so and ask for a security-focused review.

## What Happens After You Submit

**1. Automated checks.** [Java CI with Maven](.github/workflows/maven.yml) runs on every PR: it sets up JDK 11, runs `configure-dependencies.sh`, frees disk space, then runs `make test` — the full build plus both end-to-end configurations. On failure it dumps container logs, which are usually where the real cause is. [CodeQL](.github/workflows/codeql-analysis.yml) also runs on every PR, and weekly. Expect this to take a while; fix anything red before asking for review.

**2. Triage.** A maintainer labels the PR and assigns a reviewer. If you haven't heard anything within a week, nudge on the PR or in [Discussions](https://github.com/Sunbird-RC/community/discussions) — a reminder is welcome, not annoying.

**3. Review.** Expect comments, and expect a few rounds. Push follow-up commits to the same branch rather than opening a replacement PR, and reply to each comment. Disagreeing is fine; say why. If the branch falls behind, `git fetch upstream && git rebase upstream/v2.1.1`.

**4. Approval and merge.** At least one maintainer approval is required, with all comments resolved and CI green. A maintainer merges — contributors don't merge their own PRs.

**After merge.** Your change sits on the working branch until that version is released, at which point it lands on `main` and is tagged, and a new working branch opens for the next release. Releases are published as container images under `ghcr.io/sunbird-rc/`.

> [!NOTE]
> **If your PR is closed without merging,** it's usually scope, direction, or inactivity. The maintainer should say which — ask if it isn't clear.

<!-- omit in toc -->
## Licensing

This repository is licensed under MIT, in line with the DPG code licence. By contributing, you agree your contribution is licensed under the repository's [LICENSE](LICENSE).