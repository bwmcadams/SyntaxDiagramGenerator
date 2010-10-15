package net.t32leaves.syntaxDiagramGenerator

case class ProductionRule(name: String, expr: Expression) {
  override def toString = name + " = " + expr + ";"
}

abstract class Expression
abstract case class ChildrenContainerExpression(children: List[Expression]) extends Expression
case class OrExpression(c: List[Expression]) extends ChildrenContainerExpression(c) {
  override def toString = "( " + c.map(_.toString).mkString(" | ") + " )"
}
case class SeqExpression(c: List[Expression]) extends ChildrenContainerExpression(c) {
  override def toString = "( " + c.map(_.toString).mkString(" ") + " )"
}
case class OptionExpression(child: Expression) extends Expression {
  override def toString = "[ " + child + " ]"
}
case class ZeroToManyExpression(child: Expression) extends Expression {
  override def toString = "{ " + child + " }"
}
case class OneToManyExpression(child: Expression) extends Expression {
  override def toString = SeqExpression(List(child, ZeroToManyExpression(child))).toString
}
case class LiteralExpression(literal: String) extends Expression {
  override def toString = "'" + literal + "'"
}
case class RuleRefExpression(name: String) extends Expression {
  override def toString = name
}
object NilExpression extends Expression

object ExpressionUtils {
  def compact(expr: Expression): Expression = expr match {
    case OrExpression(OrExpression(c) :: rest) => compact(OrExpression(c ++ rest))
    case SeqExpression(SeqExpression(c) :: rest) => compact(SeqExpression(c ++ rest))
    case OrExpression(c) if c.length == 1 => compact(c.head)
    case SeqExpression(c) if c.length == 1 => compact(c.head)
    case OrExpression(c) => OrExpression(c map (compact(_)))
    case SeqExpression(c) => SeqExpression(c map (compact(_)))
    case OptionExpression(c) => OptionExpression(compact(c))
    case OneToManyExpression(c) => OneToManyExpression(compact(c))
    case ZeroToManyExpression(c) => ZeroToManyExpression(compact(c))
    case _ => {
      println("Unable to compact unknown expression '%s' (%s)".format(expr, expr.getClass))
      expr
    }
  }

  def filterNil(l: List[Expression]) = l filter (_ match {
    case NilExpression => false
    case _ => true
  }) map (cleanFromNil(_))

  def cleanFromNil(expr: Expression): Expression = expr match {
    case OrExpression(children) => OrExpression(filterNil(children))
    case SeqExpression(children) => SeqExpression(filterNil(children))
    case OptionExpression(child) => OptionExpression(cleanFromNil(child))
    case OneToManyExpression(child) => OneToManyExpression(cleanFromNil(child))
    case ZeroToManyExpression(child) => ZeroToManyExpression(cleanFromNil(child))
    case _ => expr
  }

  def resolveVirtualRules(expr: Expression, symbols: Map[String, Expression], virtualRules: List[String], maxDepth: Int): Expression = {
    def f(expr: Expression, symbols: Map[String, Expression], d: Int): Expression = expr match {
      case OrExpression(children) => OrExpression(children map (f(_, symbols, d)))
      case SeqExpression(children) => SeqExpression(children map (f(_, symbols, d)))
      case OptionExpression(child) => OptionExpression(f(child, symbols, d))
      case OneToManyExpression(child) => OneToManyExpression(f(child, symbols, d))
      case ZeroToManyExpression(child) => ZeroToManyExpression(f(child, symbols, d - 1))
      case RuleRefExpression(name) if virtualRules.contains(name) && symbols.contains(name) => f(symbols(name), symbols - name, d)
      case RuleRefExpression(name) if d > 0 && symbols.contains(name) => f(symbols(name), symbols - name, d - 1)
      case _ => expr
    }
    f(expr, symbols, maxDepth)
  }

  def cleanRules(prodRules: List[ProductionRule], virtualRules: List[String], depth: Int) = {
    val compacted = prodRules.map(_ match {
      case ProductionRule(n, c) => ProductionRule(n, compact(cleanFromNil(c)))
    })
    val rules = (Map[String, Expression]() /: compacted)((m, e) => m update (e.name, e.expr))
    compacted.map(_ match {
      case ProductionRule(n, c) => ProductionRule(n, resolveVirtualRules(c, rules, virtualRules, depth))
    })
  }
}