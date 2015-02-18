# vault-api
Vault Orchestration Service

## IntelliJ Setup
* Configure the SBT plugin
* Open this source directory with File -> Open
* In the Import Project dialog, check "Create directories for empty content roots automatically" and set your Project SDK to 1.8 (TODO: do we actually require 1.8?)

## Plugins
* spray-routing
* spray-json
* sbt-revolver
* spray-swagger

## TODO
* Decouple config (application.conf) from so it can be deployed via chef
