# Releasing TesselFX to Maven Central

The POMs are set up for the [Central Portal](https://central.sonatype.com). Only the `tessel-fx` library (and the parent POM
it needs) is published; the demo app `tessel-gallery` is skipped.

Coordinates: `io.github.exlirixsoftware:tessel-fx:<version>`

## One-time setup

1. **Account.** Sign in at <https://central.sonatype.com> (signing in with the GitHub account `exlirixsoftware` is simplest).
2. **Namespace.** In the Portal open *Namespaces* and add `io.github.exlirixsoftware`. The Portal shows a verification key;
   create a temporary **public** GitHub repository under the `exlirixsoftware` account whose name is that key, press *Verify*,
   then delete the repository again. (The Portal's page for the namespace has the exact current steps.)
3. **GitHub repository.** The POM points to `https://github.com/exlirixsoftware/TesselFX` for `url`, `scm` and the issue tracker.
   Create that repository and push this project to it; Central only checks that the URLs are filled in, but they should work.
4. **User token.** In the Portal: *Account* > *Generate User Token*. Put it in `~/.m2/settings.xml` (Windows:
   `C:\Users\<you>\.m2\settings.xml`) under the server id `central`:

   ```xml
   <settings>
     <servers>
       <server>
         <id>central</id>
         <username>TOKEN_USERNAME</username>
         <password>TOKEN_PASSWORD</password>
       </server>
     </servers>
   </settings>
   ```

   This is the generated token, not your account password. Never commit this file.
5. **GPG key.** Central requires every file to be signed.

   ```bash
   gpg --full-generate-key                      # RSA 4096, no expiry is fine; use the same email as in the POM
   gpg --list-secret-keys --keyid-format long   # note the key id
   gpg --keyserver keyserver.ubuntu.com --send-keys <KEYID>
   ```

   Keep a backup of the secret key (`gpg --export-secret-keys --armor <KEYID>`) in a safe place: you need the same key for
   every later release.

## Checking a release without publishing

```bash
mvn -Prelease -Dgpg.skip=true clean verify
```

builds `tessel-fx-<version>.jar`, `-sources.jar` and `-javadoc.jar` in `tessel-fx/target/` and fails when the javadoc is
broken. Nothing is signed or uploaded.

## Releasing a version

1. Set the version in all three POMs (the parent and the two modules), for example with
   `mvn versions:set -DnewVersion=1.0.1` (then `mvn versions:commit`), and update `CHANGELOG.md` and the `Tessel.VERSION`
   constant in `Tessel.java`.
2. Commit and tag: `git tag v1.0.1`.
3. Deploy. The passphrase of the GPG key can be passed through the environment so it is not stored anywhere:

   ```bash
   export MAVEN_GPG_PASSPHRASE='...'            # PowerShell: $env:MAVEN_GPG_PASSPHRASE = '...'
   mvn -Prelease clean deploy
   ```

   This signs the artifacts and uploads one bundle to the Portal. Because `autoPublish` is `false` in the POM, the bundle is
   validated and then waits in the Portal under *Deployments*.
4. Open *Deployments* in the Portal, check that validation passed, and press **Publish**. After a few minutes the release is on
   Maven Central and appears at `https://central.sonatype.com/artifact/io.github.exlirixsoftware/tessel-fx`.
   (Set `autoPublish` to `true` in the `release` profile of `pom.xml` to skip this last step.)

A published version can never be changed or deleted, so run the check above first. Central does not accept `-SNAPSHOT`
versions unless snapshots are enabled for the namespace in the Portal.

## Using the released library

```xml
<dependency>
    <groupId>io.github.exlirixsoftware</groupId>
    <artifactId>tessel-fx</artifactId>
    <version>1.0.0</version>
</dependency>
```

```kotlin
dependencies { implementation("io.github.exlirixsoftware:tessel-fx:1.0.0") }
```

## Things to decide before the first release

- **Java level.** `java.release` in the parent POM is currently `26`, so the published jar contains Java 26 bytecode and only
  JDK 26 can use it. JavaFX 26 itself needs JDK 24+, but a library that targets Java 17 also serves projects on JavaFX 21 to 25.
  To publish for Java 17, set `<java.release>17</java.release>` in `tessel-fx/pom.xml` (the module's own properties override the
  parent) - the build is verified with it - and keep 26 in the gallery. Note that editors such as VS Code may then show errors
  in the library module, because JavaFX 26's class files are newer than Java 17; Maven builds are not affected.
- **JavaFX version.** The published POM depends on `javafx-controls` `26.0.2`. Consumers on another JavaFX version declare their
  own `javafx-controls` version, which wins.
