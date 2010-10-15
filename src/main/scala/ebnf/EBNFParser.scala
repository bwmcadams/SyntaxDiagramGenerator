package net.t32leaves.syntaxDiagramGenerator
package ebnf

import scala.util.parsing.combinator.RegexParsers
import scala.util.parsing.input._

object EBNFParser extends RegexParsers {
  def apply(x: CharSequence) = phrase(start)(new CharSequenceReader(x)) match {
    case Success(result, _) => Some(result)
    case e: NoSuccess => throw new Exception(e.toString)
  }

  def start: Parser[List[ProductionRule]] = syntax_rule *

  def syntax_rule = meta_identifier ~ '=' ~ definitions_list ~ ';' ^^ {
    case name ~ _ ~ expr ~ _ => ProductionRule(name, expr)
  }

  def meta_identifier = """[a-zA-Z0-9]+"""r

  def definitions_list: Parser[Expression] = (single_definition ~ (('|' ~ single_definition) *)) ^^ {
    case t ~ o => OrExpression(t :: o.map(_ match {case _ ~ x => x}))
  }

  def single_definition = (primary ~ ((("," ?) ~ primary) *)) ^^ {
    case t ~ o => SeqExpression(t :: o.map(_ match {case _ ~ x => x}))
  }

  def primary: Parser[Expression] = optional_sequence | repeated_sequence | grouped_sequence | terminal_string | meta_identifier ^^ {
    case s => RuleRefExpression(s)
  }

  def optional_sequence = "[" ~ definitions_list ~ "]" ^^ {
    case _ ~ os ~ _ => OptionExpression(os)
  }

  def repeated_sequence = "{" ~ definitions_list ~ "}" ^^ {
    case _ ~ rs ~ _ => ZeroToManyExpression(rs)
  }

  def grouped_sequence = "(" ~ definitions_list ~ ")" ^^ {
    case _ ~ gs ~ _ => gs
  }

  def terminal_string = (""""[^"\\\r\n]*(?:\\.[^"\\\r\n]*)*" """r) ^^ {
    case s => LiteralExpression(s)
  }
}