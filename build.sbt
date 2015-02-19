name          := "Vault-Orchestration"

version       := "0.1"

scalaVersion  := "2.11.2"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies ++= {
  val akkaV = "2.3.6"
  val sprayV = "1.3.2"
  Seq(
    "io.spray"            %%  "spray-can"     % sprayV
    ,"io.spray"            %%  "spray-routing" % sprayV
    ,"io.spray"            %%  "spray-json"    % "1.3.1"
    ,"io.spray"            %%  "spray-testkit" % sprayV  % "test"
    ,"com.typesafe.akka"   %%  "akka-actor"    % akkaV
    ,"com.typesafe.akka"   %%  "akka-testkit"  % akkaV   % "test"
    ,"org.specs2"          %%  "specs2-core"   % "2.3.11" % "test"
    ,"com.gettyimages"     %%  "spray-swagger" % "0.5.0"
    // -- Logging --
    ,"ch.qos.logback" % "logback-classic" % "1.1.2"
    ,"com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2"
  )
}

// Try to resolve slf4j logging errors
// SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder".
// SLF4J: Defaulting to no-operation (NOP) logger implementation
// SLF4J: See http://www.slf4j.org/codes.html#StaticLoggerBinder for further details.
//libraryDependencies ++= Seq(
//  "com.typesafe" % "scalalogging-slf4j" % "1.1.0",
//  "org.slf4j"    % "slf4j-api"    % "1.7.10",
//  "org.slf4j"    % "log4j-over-slf4j"  % "1.7.10",
//  "ch.qos.logback"   % "logback-classic"  % "1.1.2"
//)

Revolver.settings

javaOptions in Revolver.reStart += "-Dconfig.file=application.conf"