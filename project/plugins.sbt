addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "2.1.1")
addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.4")
// ScalaPB compiler plugin for generating Scala code from .proto files
libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.11.10"