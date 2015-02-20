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
    ,"org.scalatest"       %   "scalatest_2.11" % "2.2.1" % "test"
    ,"com.gettyimages"     %%  "spray-swagger" % "0.5.0"
    // -- Logging --
    ,"ch.qos.logback" % "logback-classic" % "1.1.2"
    ,"com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2"
  )
}

Revolver.settings

javaOptions in Revolver.reStart += "-Dconfig.file=application.conf"