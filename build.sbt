ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.14"

lazy val root = (project in file("."))
  .settings(
    name := "CS441_Fall2024_Assignment_3"
  )


libraryDependencies ++= Seq(
  "com.typesafe.akka"       %% "akka-actor-typed"    % "2.8.6",
  "com.typesafe.akka"       %% "akka-stream"         % "2.8.6",
  "com.typesafe.akka"       %% "akka-http"           % "10.5.3",
  "de.heikoseeberger"       %% "akka-http-circe"     % "1.39.2",
  "io.circe"                %% "circe-generic"       % "0.14.9",
  // Exclude scala-java8-compat from AWS SDK dependencies
  "software.amazon.awssdk"   % "bedrock"             % "2.21.45" exclude("org.scala-lang.modules", "scala-java8-compat_2.13"),
  "software.amazon.awssdk"   % "bedrockruntime"      % "2.21.45" exclude("org.scala-lang.modules", "scala-java8-compat_2.13"),
  "software.amazon.awssdk"   % "core"                % "2.21.45" exclude("org.scala-lang.modules", "scala-java8-compat_2.13"),
  "software.amazon.awssdk"   % "auth"                % "2.21.45" exclude("org.scala-lang.modules", "scala-java8-compat_2.13"),
  "software.amazon.awssdk"   % "apache-client"       % "2.21.45" exclude("org.scala-lang.modules", "scala-java8-compat_2.13"),
  "software.amazon.awssdk"   % "netty-nio-client"    % "2.21.45" exclude("org.scala-lang.modules", "scala-java8-compat_2.13"),
  // Ensure scala-java8-compat is at version 1.0.0
  "org.scala-lang.modules"  %% "scala-java8-compat"  % "1.0.0",
  "org.slf4j"                % "slf4j-simple"        % "2.0.13",
  "com.typesafe"             % "config"              % "1.4.3",
  "org.scalatest"           %% "scalatest"           % "3.2.19" % Test
)
