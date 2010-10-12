package net.t32leaves.syntaxDiagramGenerator
package ebnf

import java.io.{OutputStreamWriter, FileOutputStream}

object Main {
  def main(args: Array[String]) {
    val (pluginArgs, restArgs) = args.partition(_.startsWith("-P:"))
	if(restArgs.isEmpty) {
	  println("usage: sdg [options] inputFile.ebnf")
	  return()
	}
    var virtualRules = (List[String]()/:pluginArgs.filter(_.startsWith("-P:virtual:")).map(_.substring("-P:virtual:".length).split(",").map(_.trim)))(_ ++ _)
    var depth = {
      var t = pluginArgs.filter(_.startsWith("-P:depth:"))
      if(t.isEmpty) 1
      else t.first.substring("-P:depth:".length).toInt
    }
    
    val input = scala.io.Source.fromFile(restArgs.first).getLines.mkString
    val prodRules = EBNFParser.apply(input) match {
      case Some(c) => c
    }
    
    val cleanedExpressions = ExpressionUtils.cleanRules(prodRules, virtualRules, depth)
    println(cleanedExpressions mkString("\n"))
    cleanedExpressions.foreach(e => {
      val diag = ExpressionDiagram(e.expr, e.name)
      diag.stream(new OutputStreamWriter(new FileOutputStream(e.name + ".svg"), "UTF-8"), true)
      ExpressionDiagram.transcode(e.name, "png")
      ExpressionDiagram.transcode(e.name, "pdf")
    })
  }
}
