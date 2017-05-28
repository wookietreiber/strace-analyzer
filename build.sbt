enablePlugins(GitVersioning)
enablePlugins(JavaAppPackaging)
enablePlugins(LinuxPlugin)

organization in ThisBuild := "com.github.wookietreiber"

scalaVersion in ThisBuild := "2.11.7"

git.baseVersion in ThisBuild := "0.1.0"

lazy val root = (project in file(".")).
  enablePlugins(BuildInfoPlugin).
  settings (
    name := "strace-analyzer",
    buildInfoKeys := Seq[BuildInfoKey](name, version),
    buildInfoPackage := "strace.analyze"
  )
