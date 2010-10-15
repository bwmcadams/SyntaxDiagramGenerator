package net.t32leaves.syntaxDiagramGenerator

import scala.tools.nsc.{Global, Settings}
import scala.tools.nsc.plugins.Plugin
import scala.tools.nsc.reporters.{ConsoleReporter, Reporter}

class PluginRunner(settings: Settings, reporter: Reporter) extends Global(settings, reporter) {
  def this(settings: Settings) = this(settings, new ConsoleReporter(settings))

  val diagramGen = new DiagramGenerator(PluginRunner.this);

  override def loadPlugins(): List[Plugin] = super.loadPlugins ++ List(diagramGen)
  /*
  override protected def computeInternalPhases() {
    for (phase <- DiagramGenerator.components(this)) {
      phasesSet += phase
    }

    println(phasesSet)
  }*/
}

