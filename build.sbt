lazy val root = (project in file(".")).
  enablePlugins(BuildInfoPlugin).
  settings (
    name := "strace-analyzer",
    version := "0.1.0",
    organization := "com.github.wookietreiber",
    scalaVersion := "2.11.7",
    libraryDependencies += "com.github.scopt" %% "scopt" % "3.3.0",
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "strace"
  )
