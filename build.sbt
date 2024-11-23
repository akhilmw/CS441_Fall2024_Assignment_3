ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.14"

lazy val root = (project in file("."))
  .settings(
    name := "CS441_Fall2024_Assignment_3"
  )


libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed"   % "2.8.6",
  "com.typesafe.akka" %% "akka-stream"        % "2.8.6",
  "com.typesafe.akka" %% "akka-http"          % "10.5.3",
  "de.heikoseeberger" %% "akka-http-circe"    % "1.39.2",   // For JSON support
  "io.circe"          %% "circe-generic"      % "0.14.9",
  "software.amazon.awssdk" % "bedrock"        % "2.26.22",   // AWS SDK for Bedrock,
)