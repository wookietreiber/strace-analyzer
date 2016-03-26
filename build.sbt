enablePlugins(GitVersioning)
enablePlugins(JavaAppPackaging)
enablePlugins(UniversalPlugin)

organization in ThisBuild := "com.github.wookietreiber"

scalaVersion in ThisBuild := "2.11.7"

lazy val root = (project in file(".")).
  enablePlugins(BuildInfoPlugin).
  settings (
    name := "strace-analyzer",
    buildInfoKeys := Seq[BuildInfoKey](name, version),
    buildInfoPackage := "strace.analyze",
    mappings in Universal <++= name in Universal map { name =>
      val license = file("LICENSE")
      val notice = file("NOTICE.md")
      val manPage = file("strace-analyzer.1")
      Seq (
        license -> ("share/" + name + "/LICENSE"),
        notice -> ("share/" + name + "/NOTICE.md"),
        manPage -> ("share/man/man1/" + manPage)
      )
    }
  )
