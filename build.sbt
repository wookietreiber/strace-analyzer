enablePlugins(BuildInfoPlugin)
enablePlugins(GitVersioning)
enablePlugins(JavaAppPackaging)
enablePlugins(UniversalPlugin)

organization in ThisBuild := "com.github.wookietreiber"

scalaVersion in ThisBuild := "2.12.2"

crossScalaVersions in ThisBuild := Seq("2.11.11", "2.12.2")

name := "strace-analyzer"

libraryDependencies += "com.github.wookietreiber" %% "scala-chart" % "0.5.1"

buildInfoKeys := Seq[BuildInfoKey](name, version)
buildInfoPackage := "strace.analyze"

mappings in Universal ++= {
  val n = (name in Universal).value

  val license = file("LICENSE")
  val notice = file("NOTICE.md")
  val manPage = file("strace-analyzer.1")
  val completion = file("bash-completion.sh")

  Seq (
    license -> ("share/" + n + "/LICENSE"),
    notice -> ("share/" + n + "/NOTICE.md"),
    manPage -> ("share/man/man1/" + manPage),
    completion -> ("share/bash-completion/completions/" + n)
  )
}
