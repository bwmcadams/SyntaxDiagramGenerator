package com.novus.analytics
package dsl
package portfolio
/*
 * src/main/scala/dsl/portfolio/ParsingDSL.scala
 * created: 05/25/10
 * 
 * Copyright (c) 2009, 2010 Novus Partners, Inc. <http://novus.com>
 * 
 * $Id: scalacommenter.vim 307 2010-04-09 01:07:43Z  $
 * 
 */
import scala.util.parsing.combinator._

class ParsingDSL extends JavaTokenParsers {
  def mapping: Parser[(Int, String)] =
    wholeNumber~"is"~ident ^^
  { case columnNum~_~fieldName => (columnNum.toInt - 1, fieldName) }

  def statement: Parser[Map[Int, String]] = 
    "parse file with "~>"("~> repsep(mapping, ",") <~")" ^^ (Map() ++ _)
}

// vim: set ts=2 sw=2 sts=2 et
