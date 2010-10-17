package net.t32leaves.syntaxDiagramGenerator
import org.{ scalatest, junit }
@junit.runner.RunWith(classOf[scalatest.junit.JUnitRunner])
class SyntaxDiagramGeneratorSpec extends scalatest.FlatSpec with scalatest.matchers.ShouldMatchers {
  "SyntaxDiagramGenerator" should "be able to process the sample grammar" in {
    Main.main(Array("src/main/scala/ebnf/EBNFParser.scala"))
  }
  it should "create image files in the output directory for every rule" in {
    val rules = List("definitions_list", "grouped_sequence", "meta_identifier", "optional_sequence",
      "primary", "repeated_sequence", "single_definition", "start", "syntax_rule", "terminal_string")
    val files = new java.io.File(DiagramGenerator.OutputLocation).list
    for (name <- rules; suffix <- List("pdf", "svg", "png")) files should contain(name + "." + suffix)
  }
}