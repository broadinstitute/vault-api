name          := "Vault-Orchestration"

version       := "0.1"

scalaVersion  := "2.11.2"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies ++= {
  val akkaV = "2.3.6"
  val sprayV = "1.3.2"
  Seq(
    "io.spray"             %%  "spray-can"     % sprayV
    ,"io.spray"            %%  "spray-routing" % sprayV
    ,"io.spray"            %%  "spray-json"    % "1.3.1"
    ,"io.spray"            %%  "spray-client"  % sprayV
    ,"io.spray"            %%  "spray-testkit" % sprayV  % "test"
    ,"com.typesafe.akka"   %%  "akka-actor"    % akkaV
    ,"org.scalatest"       %%  "scalatest"     % "2.2.1" % "test"
    ,"com.gettyimages"     %%  "spray-swagger" % "0.5.0"
    ,"org.webjars"         %   "swagger-ui"    % "2.1.8-M1"
    // -- Logging --
    ,"ch.qos.logback" % "logback-classic" % "1.1.2"
    ,"com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2"
  )
}

Revolver.settings

javaOptions in Revolver.reStart += "-Dconfig.file=application.conf"
