ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.13.14"  // Changed from 2.13.14 as that version doesn't exist

// Version definitions
val akkaVersion = "2.8.6"
val akkaHttpVersion = "10.5.3"
val circeVersion = "0.14.9"
val awsSdkVersion = "2.21.45"
val protobufVersion = "3.24.0"

lazy val root = (project in file("."))
  .settings(
    name := "CS441_Fall2024_Assignment_3",

    assemblyMergeStrategy in assembly := {
      case PathList("META-INF", "services", xs @ _*) => MergeStrategy.concat
      case PathList("META-INF", xs @ _*)             => MergeStrategy.discard
      case PathList("module-info.class")             => MergeStrategy.discard
      case PathList("META-INF", "native-image", xs @ _*) => MergeStrategy.discard
      case PathList("google", "protobuf", xs @ _*)   => MergeStrategy.first
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    },

    assembly / mainClass := Some("Main"),

      // Protobuf and ScalaPB settings
    Compile / PB.protoSources := Seq(sourceDirectory.value / "main" / "protobuf"),
    Compile / PB.targets := Seq(
      scalapb.gen(grpc = true) -> (Compile / sourceManaged).value / "scalapb"
    ),

    libraryDependencies ++= Seq(
      // Protobuf dependencies
      "com.google.protobuf" % "protobuf-java" % protobufVersion % "protobuf",
      "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf",

      // gRPC dependencies
      "io.grpc" % "grpc-netty" % "1.53.0",
      "io.grpc" % "grpc-protobuf" % "1.53.0",
      "io.grpc" % "grpc-stub" % "1.53.0",
      "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion,

      // Akka dependencies
      "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "de.heikoseeberger" %% "akka-http-circe" % "1.39.2",

      // Circe dependencies
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,

      // AWS SDK dependencies
      "software.amazon.awssdk" % "bedrock" % awsSdkVersion exclude("org.scala-lang.modules", "scala-java8-compat_2.13"),
      "software.amazon.awssdk" % "bedrockruntime" % awsSdkVersion exclude("org.scala-lang.modules", "scala-java8-compat_2.13"),
      "software.amazon.awssdk" % "core" % awsSdkVersion exclude("org.scala-lang.modules", "scala-java8-compat_2.13"),
      "software.amazon.awssdk" % "auth" % awsSdkVersion exclude("org.scala-lang.modules", "scala-java8-compat_2.13"),
      "software.amazon.awssdk" % "apache-client" % awsSdkVersion exclude("org.scala-lang.modules", "scala-java8-compat_2.13"),
      "software.amazon.awssdk" % "netty-nio-client" % awsSdkVersion exclude("org.scala-lang.modules", "scala-java8-compat_2.13"),

      // ollama dependency
      "io.github.ollama4j" % "ollama4j" % "1.0.79",

      // Scala Java 8 compatibility
      "org.scala-lang.modules" %% "scala-java8-compat" % "1.0.0",

      // Logging dependencies
      "org.slf4j" % "slf4j-api" % "2.0.13",
      "ch.qos.logback" % "logback-classic" % "1.4.14",

      // Configuration
      "com.typesafe" % "config" % "1.4.3",

      // Testing
      "org.scalatest" %% "scalatest" % "3.2.19" % Test,
      "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test
    ),

    scalacOptions ++= Seq(
      "-deprecation",
      "-feature",
      "-unchecked",
      "-encoding", "utf8"
    )
  )