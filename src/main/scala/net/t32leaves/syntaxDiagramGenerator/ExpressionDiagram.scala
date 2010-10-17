package net.t32leaves.syntaxDiagramGenerator

import java.io.FileOutputStream
import java.awt.{Font, Graphics2D, Dimension}
import java.awt.geom._

import org.apache.batik.svggen.SVGGraphics2D
import org.apache.batik.dom.svg.SVGDOMImplementation

import org.apache.batik.transcoder.{TranscoderInput, TranscoderOutput}
import org.apache.batik.transcoder.image.PNGTranscoder
import org.apache.fop.svg.PDFTranscoder

object ExpressionDiagram {
  private val PADDING = 10
  private val PADDING_HALF = 5

  private abstract class Graph {
    def apply

    def w: Int

    def h: Int
  }

  private class OrGraph(children: List[Graph], g: Graphics2D) extends Graph {
    def apply: Unit = {
      val prev = g.getTransform

      val x = w
      children.foreach(c => {
        val dw = (x - c.w) / 2
        val ch = c.h
        val dh = ch / 2

        val path = new GeneralPath()
        if (children.head == c) {
          path.moveTo(dw, dh + PADDING)
          path.lineTo(PADDING, dh + PADDING)
          path.quadTo(0, dh + PADDING, 0, dh + 2 * PADDING)
          path.lineTo(0, ch + PADDING)

          path.moveTo(x - dw, dh + PADDING)
          path.lineTo(x - PADDING, dh + PADDING)
          path.quadTo(x, dh + PADDING, x, dh + 2 * PADDING)
          path.lineTo(x, ch + PADDING)
        } else {
          path.moveTo(0, 0)
          path.lineTo(0, dh)
          path.quadTo(0, PADDING + dh, PADDING, PADDING + dh)
          path.lineTo(dw, PADDING + dh)
          path.moveTo(x - dw, PADDING + dh)
          path.lineTo(x - PADDING, PADDING + dh)
          path.quadTo(x, PADDING + dh, x, dh)
          path.lineTo(x, 0)

          if (children.last != c) {
            path.moveTo(0, dh)
            path.lineTo(0, ch + PADDING)
            path.moveTo(x, dh)
            path.lineTo(x, ch + PADDING)
          }
        }

        g.draw(path)

        g.translate(dw, PADDING)
        c.apply
        g.translate(-dw, ch)
      })

      g.setTransform(prev)
      //val y = h - children.last.h
      //g.drawLine(0, 0, 0, y)
      //g.drawLine(x, 0, x, y)
    }

    def w = (0 /: children)((m, e) => if (m < e.w) e.w else m) + (3 * PADDING)

    def h = (children.length * PADDING) + (0 /: children)((m, e) => m + e.h)
  }

  private class SeqGraph(children: List[Graph], g: Graphics2D) extends Graph {
    def apply: Unit = {
      val prev = g.getTransform

      val y = h / 2
      children.foreach(c => {
        val dh = y - (c.h / 2)

        g.drawLine(0, y, PADDING, y)
        g.translate(PADDING, dh)
        c.apply
        g.translate(c.w, -dh)
      })
      g.drawLine(0, y, PADDING, y)

      g.setTransform(prev)
    }

    def w = (children.length * PADDING) + (0 /: children)((m, e) => m + e.w) + PADDING

    def h = (0 /: children)((m, e) => if (m < e.h) e.h else m)
  }

  private abstract class CrossingGraph(child: Graph, g: Graphics2D) extends Graph {
    protected def drawDirection(offsetX: Int, offsetY: Int)

    def apply {
      val ch = child.h
      val cw = child.w

      g.translate(PADDING, 0)
      child.apply
      g.translate(-PADDING, 0)

      val path = new GeneralPath
      path.moveTo(0, 0)
      path.moveTo(0, h / 2)
      path.quadTo(0, ch / 2, PADDING, ch / 2)

      path.moveTo(cw + PADDING, ch / 2)
      path.lineTo(cw + 2 * PADDING, ch / 2)
      path.quadTo(cw + 3 * PADDING, ch / 2, cw + 3 * PADDING, h / 2)

      path.moveTo(0, h / 2)
      path.lineTo(0, ch)
      path.quadTo(0, ch + PADDING, PADDING, ch + PADDING)
      path.lineTo(cw + 2 * PADDING, ch + PADDING)
      path.quadTo(cw + 3 * PADDING, ch + PADDING, cw + 3 * PADDING, ch)
      path.lineTo(cw + 3 * PADDING, h / 2)
      g.draw(path)

      drawDirection((cw + PADDING) / 2, ch + PADDING_HALF)
    }

    def w = child.w + (3 * PADDING)

    def h = child.h + (2 * PADDING)
  }

  private class OptionalGraph(child: Graph, g: Graphics2D) extends CrossingGraph(child, g) {
    protected def drawDirection(offsetX: Int, offsetY: Int) {
      val path = new GeneralPath
      path.moveTo(offsetX, offsetY)
      path.lineTo(offsetX + PADDING, offsetY + PADDING_HALF)
      path.lineTo(offsetX, offsetY + PADDING)
      path.lineTo(offsetX, offsetY)
      g.fill(path)
    }
  }

  private class OneToManyGraph(child: Graph, g: Graphics2D) extends CrossingGraph(child, g) {
    protected def drawDirection(offsetX: Int, offsetY: Int) {
      val path = new GeneralPath
      path.moveTo(offsetX + PADDING, offsetY)
      path.lineTo(offsetX, offsetY + PADDING_HALF)
      path.lineTo(offsetX + PADDING, offsetY + PADDING)
      path.lineTo(offsetX + PADDING, offsetY)
      g.fill(path)
    }
  }

  private class LiteralGraph(literal: String, g: Graphics2D) extends Graph {
    def apply {
      g.drawRect(0, 0, w, h)
      g.drawLine(PADDING / 2, 0, PADDING / 2, h)
      g.drawString(literal, PADDING, h - PADDING_HALF)
    }

    def w = g.getFontMetrics.stringWidth(literal) + (2 * PADDING)

    def h = g.getFontMetrics.getHeight + PADDING
  }

  private class RuleRefGraph(ruleName: String, g: Graphics2D) extends Graph {
    def apply {
      g.drawRect(0, 0, w, h)
      g.drawString(ruleName, PADDING_HALF, h - PADDING_HALF)
    }

    def w = g.getFontMetrics.stringWidth(ruleName) + PADDING

    def h = g.getFontMetrics.getHeight + PADDING
  }


  def apply(expr: Expression, name: String) = {
    val impl = SVGDOMImplementation.getDOMImplementation()
    val doc = impl.createDocument(SVGDOMImplementation.SVG_NAMESPACE_URI, "svg", null)

    val g = new SVGGraphics2D(doc)
    g.setFont(new Font("Helvetica", Font.BOLD, 10))
    val titleFontHeight = g.asInstanceOf[Graphics2D].getFontMetrics.getHeight
    g.drawString(name, PADDING: Float, PADDING + titleFontHeight: Float)
    g.translate(PADDING, PADDING + titleFontHeight)
    g.setFont(new Font("Courier", Font.PLAIN, 10))
    val subgr = subgraph(expr, g)
    subgr.apply

    g.setSVGCanvasSize(new Dimension(subgr.w + 2 * PADDING, subgr.h + 3 * PADDING))
    g
  }

  private def subgraph(expr: Expression, g: Graphics2D): Graph = expr match {
    case OrExpression(c) => new OrGraph(c.map(subgraph(_, g)), g)
    case SeqExpression(c) => new SeqGraph(c.map(subgraph(_, g)), g)
    case OptionExpression(c) => new OptionalGraph(subgraph(c, g), g)
    case ZeroToManyExpression(c) => new OptionalGraph(new OneToManyGraph(subgraph(c, g), g), g)
    case OneToManyExpression(c) => new OneToManyGraph(subgraph(c, g), g)
    case LiteralExpression(l) => new LiteralGraph(l, g)
    case RuleRefExpression(n) => new RuleRefGraph(n, g)
  }

  def transcode(name: String, format: String) {
    val t = format match {
      case "png" => new PNGTranscoder()
      case "pdf" => new PDFTranscoder()
    }
    val input = new TranscoderInput(new java.io.File(name + ".svg").toURI.toURL.toString)

    // Create the transcoder output.
    val ostream = new FileOutputStream(name + "." + format)
    val output = new TranscoderOutput(ostream)

    // Save the image.
    t.transcode(input, output)

    // Flush and close the stream.
    ostream.flush
    ostream.close
  }
}