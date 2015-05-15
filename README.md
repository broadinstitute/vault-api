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

Create a local .sbtopts file with the following properties.
See DevOps or any Vault team member for the correct values.

```
-Dconfig.file=application.conf
-Dopenam.deploymentUri=<replace_me>
-Dopenam.username=<replace_me>
-Dopenam.password=<replace_me>
-Dopenam.commonName=<replace_me>
-Dopenam.testUser=<replace_me>
-Dopenam.testUserPassword=<replace_me>
-Dopenam.tokenUrl=<replace_me>
-Dboss.bossUser=<replace_me>
-Dboss.bossUserPassword=<replace_me>
```

To test against local services, set the additional options:
```
-Ddm.server="http://localhost:8081"
-Dboss.server="http://localhost:8180"
```
