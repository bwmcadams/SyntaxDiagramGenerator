package net.t32leaves.syntaxDiagramGenerator

import java.io.{OutputStreamWriter, FileOutputStream}
import org.w3c.dom.Document
import scala.tools.nsc
import nsc.Global
import nsc.Phase
import nsc.ast.TreeBrowsers
import nsc.plugins.Plugin
import nsc.plugins.PluginComponent

import scala.util.matching.Regex

import scala.util.parsing.combinator.Parsers

/*
 * Assumptions and conventions
 * - The name of the class which contains the production rules must equal the filename - ".scala"
 * - Production rules must not be named: "stringWrapper", "literal", "regex", "Predef", "r" or "accept"
 * - def production rules must not have parameters, otherwise they're not considered to be production rules
 * - val precedes def
 */

class DiagramGenerator(val global: Global) extends Plugin {
  import global._
  val repsepRE = new Regex("^[0-9a-zA-Z]*\\.this\\.repsep")
  val pluginArgs: List[String] = List[String]()
  val name = "generateDiagrams"
  val description = "Generates syntax diagrams out of parser combinator"
  val components = List[PluginComponent](Component)
  
  var virtualRules = 
    (List[String]()/:pluginArgs.filter(_.startsWith("-P:virtual:")).map(_.substring("-P:virtual:".length).split(",").map(_.trim)))(_ ++ _)
  var depth = {
    var t = pluginArgs.filter(_.startsWith("-P:depth:"))
    if(t.isEmpty) 1
    else t.head.substring("-P:depth:".length).toInt
  }
  
  
  private object Component extends PluginComponent {
    val global: DiagramGenerator.this.global.type = DiagramGenerator.this.global
    val runsAfter = List[String]("liftcode")
    // Using the Scala Compiler 2.8.x the runsAfter should be written as below
    // val runsAfter = List[String]("refchecks");
    val phaseName = DiagramGenerator.this.name
    def newPhase(_prev: Phase) = new DiagramGeneratorPhase(_prev)    
    
    class DiagramGeneratorPhase(prev: Phase) extends StdPhase(prev) {
      override def name = DiagramGenerator.this.name
      def apply(unit: CompilationUnit) {
        val name = unit.source.toString.split("\\.").head
        val expressions = DiagramGenerator.this.rules(unit.body, name)
        println("Expressions... %s".format(expressions))
        val cleanedExpressions = ExpressionUtils.cleanRules(expressions, virtualRules, depth)
        println(cleanedExpressions mkString("\n"))
        cleanedExpressions.foreach(e => {
          try {
            val diag = ExpressionDiagram(e.expr, e.name)
            diag.stream(new OutputStreamWriter(new FileOutputStream(e.name + ".svg"), "UTF-8"), true)
            ExpressionDiagram.transcode(e.name, "png")
            ExpressionDiagram.transcode(e.name, "pdf")
          } catch {
            /*case ne: java.lang.NoClassDefFoundError => println("ERROR: %s".format(ne.getCause.getCause))*/
            case e => throw e
          }
        })
      }
    }
  }

  private def rules(t: Tree, containerName: String): List[ProductionRule] = t match {
    case PackageDef(_, children) => (List[ProductionRule]()/:children.map(rules(_, containerName)))(_ ++ _)
    case ClassDef(_, name, _, Template(_, _, children)) if name.toString == containerName => (List[ProductionRule]()/:children.map(rules(_, containerName)))(_ ++ _)
    case ValDef(_, name, _, rhs) => List(new ProductionRule(name.toString, buildExpression(rhs)))
    case DefDef(_, name, _, vp, _, rhs) if vp.isEmpty => List(new ProductionRule(name.toString, buildExpression(rhs)))
    case _ => List()
  }
  
  private def buildExpression(t: Tree): Expression = t match {
    case Select(qualifier, name) => name.toString match {
      case "$plus" => OneToManyExpression(buildExpression(qualifier))
      case "$bar" => OrExpression(List(buildExpression(qualifier)))
      case "$tilde" | "$less$tilde" | "$tilde$greater" => SeqExpression(List(buildExpression(qualifier)))
      case "$qmark" => OptionExpression(buildExpression(qualifier))
      case "$times" => ZeroToManyExpression(buildExpression(qualifier))
      case "$up$up" => buildExpression(qualifier)
      case "stringWrapper" => buildExpression(qualifier)
      case "literal" => buildExpression(qualifier)
      case "accept" => NilExpression
      case "regex" => buildExpression(qualifier)
      case "Predef" => buildExpression(qualifier)
      case "r" => buildExpression(qualifier)
      case nm => SeqExpression(List(RuleRefExpression(nm), buildExpression(qualifier)))
    }
    case Apply(fun, args) => {
      if (repsepRE.findFirstIn(fun.toString).isDefined) {
        println("Repsep: %s".format(args(0)))
        val rs = OneToManyExpression(SeqExpression(args.map(buildExpression(_))))
        println("RS: %s".format(rs))
        rs
      } else buildExpression(fun) match {
        case OrExpression(c) => OrExpression(c ++ args.map(buildExpression(_)))
        case SeqExpression(c) => SeqExpression(c ++ args.map(buildExpression(_)))
        case NilExpression if args.length == 1 => buildExpression(args.head)
        case r => r
      }
    }
    case TypeApply(fun, args) => buildExpression(fun)
    case Literal(Constant(l)) => LiteralExpression(l.toString)
    case This(_) => NilExpression
    case TypeTree() => NilExpression
    case Function(_, _) => NilExpression
    case _ => NilExpression
  }
}
