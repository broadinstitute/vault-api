# vault-api
Vault Orchestration Service

## IntelliJ Setup
* Configure the SBT plugin
* Open this source directory with File -> Open
* In the Import Project dialog, check "Create directories for empty content roots automatically" and set your Project SDK to 1.8 (TODO: do we actually require 1.8?)

## Plugins
* spray-routing
* spray-json
* sbt-assembly
* sbt-revolver
* spray-swagger

## Development Notes
* Configuration is excluded from the build package:
    - When running via sbt, start sbt with the config file ```sbt -Dconfig.file=application.conf``` and the run command will pick up your local configuration.
    - When running via sbt/revolver (i.e. using the re-start command), you can just run in sbt normally - the config is preset for you in build.sbt.

## Building and Running

Run the assembly task to build a fat jar:
```
sbt
assembly
```

Execute the jar with the path to the jar and path fo the config file:
```
java -Dconfig.file=application.conf -jar Vault-Orchestration-assembly-0.1.jar
```

## Testing

Replace the integration components of application.conf to reflect correct values.
See DevOps or any Vault team member for details. Make sure sbt is run with the config file option.

    vault {
      ubamsRedirectUrl="https://vault-ci.vault.broadinstitute.org/api/ubams/%s/%s"
    }
    dm {
      ubamsUrl="https://dm-ci.vault.broadinstitute.org/api/ubams"
      ubamsResolveUrl="https://dm-ci.vault.broadinstitute.org/api/ubams/%s"
    }
    boss {
      objectsUrl="... point to boss instance ..."
      ...
      bossUser="... real vault system user name for boss ..."
      bossUserPassword="... real vault user password for boss ..."
    }
    openam {
      testUser="... real openam testing user name ..."
      testUserPassword="... openam testing user password ..."
      tokenUrl="... point to open am instance ..."
    }

