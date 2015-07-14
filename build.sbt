name := "pants"

version := "0.1"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
    "io.netty" % "netty-all" % "4.0.29.Final",
    "com.google.protobuf" % "protobuf-java" % "2.6.1",
    "org.slf4j" % "slf4j-log4j12" % "1.7.12"
)

compileOrder := CompileOrder.JavaThenScala

unmanagedSourceDirectories in Compile += baseDirectory.value / "generated-src"