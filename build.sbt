enablePlugins(org.nlogo.build.NetLogoExtension)

version    := "1.1.0"
isSnapshot := true

scalaVersion           := "3.7.0"
Compile / scalaSource  := baseDirectory.value / "src" / "main"
Test / scalaSource     := baseDirectory.value / "src" / "test"
scalacOptions          ++= Seq("-deprecation", "-unchecked", "-Xfatal-warnings", "-encoding", "us-ascii", "-release", "11")

netLogoExtName      := "import-a"
netLogoClassManager := "org.nlogo.extension.importa.ImportExtension"
netLogoVersion      := "7.0.0-beta1-c8d671e"
