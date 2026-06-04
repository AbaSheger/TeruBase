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
   com.terubase
   ```

   If `com.terubase` is not available or cannot be verified, change the Maven
   `groupId` before publishing. Common fallback coordinates are based on a
   verified domain or GitHub namespace, for example `io.github.abasheger`.

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

Maven Central releases must not use a `-SNAPSHOT` version. Set the parent,
starter, plugin, and example references to the release version first, for
example `0.1.0`.

Then deploy with the release profile:

```bash
mvn -B -ntp -Pcentral-release clean deploy
```

The Central Portal upload is configured with `autoPublish=false`, so the
deployment should be reviewed and published manually at:

```text
https://central.sonatype.com/publishing/deployments
```

After publishing, bump the project back to the next development version, for
example `0.1.1-SNAPSHOT`.
