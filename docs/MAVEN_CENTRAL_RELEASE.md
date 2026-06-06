# Maven Central Release

TeruBase is configured for Maven Central publishing through the
`central-release` Maven profile.

## What Is Configured

- Required POM metadata: name, description, project URL, MIT license, developer,
  and SCM.
- Source jars through `maven-source-plugin`.
- Javadoc jars through `maven-javadoc-plugin`.
- Artifact signatures through `maven-gpg-plugin`.
- Upload to the Sonatype Central Portal through
  `org.sonatype.central:central-publishing-maven-plugin`.

## Before the First Release

1. Create or sign in to a Sonatype Central Portal account:

   ```text
   https://central.sonatype.com
   ```

2. Create and verify the namespace for the Maven group:

   ```text
   io.github.abasheger
   ```

   The Maven `groupId` must match this verified namespace.

3. Generate a Central Portal user token and add it to `~/.m2/settings.xml`:

   ```xml
   <settings>
     <servers>
       <server>
         <id>central</id>
         <username><!-- token username --></username>
         <password><!-- token password --></password>
       </server>
     </servers>
   </settings>
   ```

4. Configure GPG locally. The release profile signs artifacts during `verify`,
   so Maven must be able to use your signing key.

## GitHub Actions Secrets

The manual release workflow at `.github/workflows/release.yml` expects these
repository secrets:

```text
CENTRAL_USERNAME
CENTRAL_PASSWORD
GPG_PRIVATE_KEY_BASE64
GPG_PASSPHRASE
```

`CENTRAL_USERNAME` and `CENTRAL_PASSWORD` come from the Sonatype Central Portal
user token. `GPG_PRIVATE_KEY_BASE64` should be a base64-encoded copy of the
ASCII-armored private key used for artifact signing. `GPG_PASSPHRASE` is the
passphrase for that key.

The workflow decodes and imports the private key manually, then exposes the
passphrase to Maven as `MAVEN_GPG_PASSPHRASE`, which is the environment variable
expected by the Maven GPG Plugin in unattended CI builds.

On Windows PowerShell, generate the base64 secret value with:

```powershell
& 'C:\Program Files\GnuPG\bin\gpg.exe' --armor --export-secret-keys YOUR_KEY_ID | Set-Content -LiteralPath private.key -Encoding ascii
[Convert]::ToBase64String([IO.File]::ReadAllBytes((Resolve-Path private.key))) | Set-Clipboard
Remove-Item -LiteralPath private.key -Force
```

Then paste the clipboard value into the GitHub secret named
`GPG_PRIVATE_KEY_BASE64`.

## Validate Locally

Normal project verification should still pass without release signing:

```bash
mvn -B -ntp clean verify
```

Validate source and Javadoc artifact generation:

```bash
mvn -B -ntp -Pcentral-release -Dgpg.skip=true clean package
```

In PowerShell, quote the dotted property:

```powershell
mvn -B -ntp -Pcentral-release '-Dgpg.skip=true' clean package
```

## Publish

Maven Central releases must not use a `-SNAPSHOT` version. For a manual local
release, set the reactor parent and module versions to the release version, for
example `0.1.0`. The invoice example is not part of the published reactor.

Then deploy with the release profile:

```bash
mvn -B -ntp -Pcentral-release clean deploy
```

The Central Portal upload is configured with `autoPublish=false`, so the
deployment should be reviewed and published manually at:

```text
https://central.sonatype.com/publishing/deployments
```

After a manual local release, bump the project back to the next development
version, for example `0.1.1-SNAPSHOT`.

## Publish With GitHub Actions

The `Maven Central Release` workflow is manually triggered from GitHub Actions.

Use `publish=false` first to verify that the release version can build, create
source jars, create Javadoc jars, and sign artifacts.

Use `publish=true` only after the dry run passes and the namespace is verified
in the Central Portal. The workflow changes the Maven version only inside the
workflow checkout; it does not commit release-version changes back to `main`.
After validation succeeds, publish the deployment manually in the Central
Portal because the release profile uses `autoPublish=false`.
