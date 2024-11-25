// build.sbt

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.14"

lazy val root = (project in file("."))
  .settings(
    name := "CS441_Fall2024_Assignment_3",

    // Enable ScalaPB Plugin
    PB.targets := Seq(
      scalapb.gen(grpc = true) -> (Compile / sourceManaged).value / "scalapb"
    ),

      // Additional settings if needed
    scalacOptions ++= Seq(
      "-deprecation",
      "-feature",
      "-unchecked",
      "-encoding", "utf8"
    )
  )

// Add library dependencies
libraryDependencies ++= Seq(
  // Akka HTTP and related dependencies
  "com.typesafe.akka"       %% "akka-actor-typed"    % "2.8.6",
  "com.typesafe.akka"       %% "akka-stream"         % "2.8.6",
  "com.typesafe.akka"       %% "akka-http"           % "10.5.3",
  "de.heikoseeberger"       %% "akka-http-circe"     % "1.39.2",
  "io.circe"                %% "circe-generic"       % "0.14.9",

  // AWS SDK for Bedrock
  // Excluding scala-java8-compat to avoid conflicts
  "software.amazon.awssdk"   % "bedrock"             % "2.21.45" exclude("org.scala-lang.modules", "scala-java8-compat_2.13"),
  "software.amazon.awssdk"   % "bedrockruntime"      % "2.21.45" exclude("org.scala-lang.modules", "scala-java8-compat_2.13"),
  "software.amazon.awssdk"   % "core"                % "2.21.45" exclude("org.scala-lang.modules", "scala-java8-compat_2.13"),
  "software.amazon.awssdk"   % "auth"                % "2.21.45" exclude("org.scala-lang.modules", "scala-java8-compat_2.13"),
  "software.amazon.awssdk"   % "apache-client"       % "2.21.45" exclude("org.scala-lang.modules", "scala-java8-compat_2.13"),
  "software.amazon.awssdk"   % "netty-nio-client"    % "2.21.45" exclude("org.scala-lang.modules", "scala-java8-compat_2.13"),

  // Ensuring scala-java8-compat is at version 1.0.0
  "org.scala-lang.modules"  %% "scala-java8-compat"  % "1.0.0",

  // Logging
  "org.slf4j"                % "slf4j-simple"        % "2.0.13",

  // Configuration
  "com.typesafe"             % "config"              % "1.4.3",

  // Testing
  "org.scalatest"           %% "scalatest"           % "3.2.19" % Test,

  // gRPC and ScalaPB
  "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % "0.11.10",
  "io.grpc" % "grpc-netty" % "1.53.0",
  "io.grpc" % "grpc-protobuf" % "1.53.0",
  "io.grpc" % "grpc-stub" % "1.53.0"
)
