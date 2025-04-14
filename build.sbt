enablePlugins(org.nlogo.build.NetLogoExtension)

version    := "1.0.8"
isSnapshot := true

scalaVersion           :=  "2.12.12"
Compile / scalaSource  := baseDirectory.value / "src" / "main"
Test / scalaSource     := baseDirectory.value / "src" / "test"
scalacOptions          ++= Seq("-deprecation", "-unchecked", "-Xfatal-warnings", "-encoding", "us-ascii", "-release", "11")

netLogoExtName      := "import-a"
netLogoClassManager := "org.nlogo.extension.importa.ImportExtension"
netLogoVersion      := "6.3.0"
