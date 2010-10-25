import sbt._

class SyntaxDiagramGeneratorProject(info: ProjectInfo) extends DefaultProject(info) {
  val mavenLocal = "Local Maven Repository" at "file://" + Path.userHome + "/.m2/repository"

  override def mainClass = Some("net.t32leaves.syntaxDiagramGenerator.Main")
}
