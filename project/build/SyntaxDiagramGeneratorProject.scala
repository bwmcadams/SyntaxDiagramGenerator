import sbt._

class SynaxDiagramGeneratorProject(info: ProjectInfo) extends DefaultProject(info) with AutoCompilerPlugins {


  val batikParser = "org.apache.xmlgraphics" % "batik-parser" % "1.7"
  val batikExtension = "org.apache.xmlgraphics" % "batik-extension" % "1.7"
  val batikSwing = "org.apache.xmlgraphics" % "batik-swing" % "1.7"
  val batikSVGGen = "org.apache.xmlgraphics" % "batik-svggen" % "1.7"
  val batikSVGDOM = "org.apache.xmlgraphics" % "batik-svg-dom" % "1.7"
  val batikTranscode = "org.apache.xmlgraphics" % "batik-transcoder" % "1.7"
    // Repositories
  val mavenOrgRepo = "Maven.Org Repository" at "http://repo1.maven.org/maven2/org/"

  override def mainClass = Some("net.t32leaves.syntaxDiagramGenerator.Main")

  def removeDupEntries(paths: PathFinder) =
   Path.lazyPathFinder {
     val mapped = paths.get map { p => (p.relativePath, p) }
     (Map() ++ mapped).values.toList
   }

  def allArtifacts = {
    Path.fromFile(buildScalaInstance.libraryJar) +++
    (removeDupEntries(runClasspath filter ClasspathUtilities.isArchive) +++
    ((outputPath ##) / defaultJarName) +++
    mainResources +++
    mainDependencies.scalaJars +++
    descendents(info.projectPath / "scripts", "run_akka.sh") +++
    descendents(info.projectPath / "scripts", "akka-init-script.sh") +++
    descendents(info.projectPath / "dist", "*.jar") +++
    descendents(info.projectPath / "deploy", "*.jar") +++
    descendents(path("lib") ##, "*.jar") +++
    descendents(configurationPath(Configurations.Compile) ##, "*.jar"))
    .filter(jar => // remove redundant libs
      !jar.toString.endsWith("stax-api-1.0.1.jar") ||
      !jar.toString.endsWith("scala-library-2.7.7.jar")
    )
  }

 override def manifestClassPath = Some(
    "scala-library.jar scala-compiler.jar " +
    allArtifacts.getFiles
    .filter(_.getName.endsWith(".jar"))
    .filter(!_.getName.contains("servlet_2.4"))
    .filter(!_.getName.contains("scala-library"))
    .map("lib_managed/scala_%s/compile/".format(buildScalaVersion) + _.getName)
    .mkString(" ") 
    )
  
  
}

// vim: set ts=2 sw=2 sts=2 et:
