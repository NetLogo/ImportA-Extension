package org.nlogo.extension.importa

import java.io.File
import org.nlogo.headless.TestLanguage

object Tests {
  val testFileNames = Seq("tests.txt")
  val testFiles     = testFileNames.map((f) => (new File(f)).getCanonicalFile)
}

class Tests extends TestLanguage(Tests.testFiles) {
  System.setProperty("org.nlogo.preferHeadless", "true")
}
