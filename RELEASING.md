# Releasing

## Prerequisites

- `~/.ssh/chain.crt` — plugin signing certificate chain
- `~/.ssh/private.pem` — plugin signing private key
- JetBrains Marketplace [personal access token](https://plugins.jetbrains.com/author/me/tokens)

## Build

```bash
./gradlew buildPlugin
```

The unsigned ZIP will be in `build/distributions/`.

## Sign

Pass the private key password via `-P` or an environment variable:

```bash
# Option 1: Gradle property (use read -s to avoid shell history)
read -s PASS && ./gradlew signPlugin -Psigning.password="$PASS"; unset PASS

# Option 2: environment variable
export PLUGIN_SIGNING_PASSWORD='your-password'
./gradlew signPlugin
```

The signed ZIP will be in `build/distributions/`.

## Publish

```bash
export ORG_GRADLE_PROJECT_intellijPlatformPublishingToken='your-token'
./gradlew publishPlugin
```

The first publication must be done manually by uploading the signed ZIP at [plugins.jetbrains.com/author/me](https://plugins.jetbrains.com/author/me). Subsequent releases can use `publishPlugin`.

## Version bump

Update the version in `build.gradle.kts`:

```kotlin
version = "0.2.0"
```

This value is used for both the Gradle project and the plugin configuration.
