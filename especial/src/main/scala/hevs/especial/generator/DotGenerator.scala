package hevs.especial.generator

import java.io.File

import hevs.especial.dsl.components.ComponentManager.Wire
import hevs.especial.dsl.components._
import hevs.especial.utils._

import scala.collection.mutable
import scala.language.existentials
import scalax.collection.Graph
import scalax.collection.edge.LDiEdge
import scalax.collection.io.dot._

/**
 * Generate a diagram of the current program. Export a `dot` and a `pdf` file.
 *
 * Export the component graph to a `dot` file using the Dot extension of the ScalaGraph library. Can be disabled
 * using the general [[hevs.especial.utils.Settings]].
 *
 * The DOT diagram and the PDF are exported to the `output/<progName>/dot` folder.
 * The name of the generated file is the program name (available in the context). The user can add a suffix to the
 * generated file name so different diagrams of the same program can be exported..
 *
 * @version 2.0
 */
class DotGenerator extends Pipeline[Option[String], Unit] {

  /**
   * Create the DOT and the PDF file that correspond to a DSL program.
   * Files written directly in the output folder (pipeline output not used).
   *
   * @param ctx the context of the program with the logger
   * @param fileNameSuffix the suffix to append to the file name, if necessary
   * @return nothing (not used). Update the program graph directly.
   */
  def run(ctx: Context)(fileNameSuffix: Option[String]): Unit = {
    // First block of the pipeline. Force to clean the output folder.
    // FIXME. clear the output folder before starting the test task
    // val folder: RichFile = new File("output/")
    // folder.createEmptyFolder()

    if (!Settings.PIPELINE_RUN_DOT) {
      ctx.log.info(s"$currentName is disabled.")
      return
    }

    // Append the suffix to the filename, is defined
    val suffix = fileNameSuffix match {
      case Some(s) => s"_$s"
      case None => ""
    }

    // Generate the DOT file
    val res = DotGenerator.generateDotFile(ctx, suffix)
    if (!res)
      ctx.log.error("Unable to generate the DOT file !")
    else
      ctx.log.info("DOT file generated and saved.")

    // Generate the PDF file if necessary
    if (Settings.PIPELINE_EXPORT_PDF) {
      // Check the if dot is installed and print the installed version
      val valid = OSUtils.runWithCodeResult("dot -V")
      if (valid._1 == 0)
        ctx.log.info(s"Running '${valid._2}'.")
      else {
        ctx.log.error(s"Unable to run DOT. Must be installed and in the PATH !\n> ${valid._2}")
        return
      }

      val res = DotGenerator.convertDotToPdf(ctx.progName, suffix)
      if (res._1 == 0)
        ctx.log.info("PDF file generated.")
      else
        ctx.log.error(s"Unable to generate the PDF file !\n> ${res._2}")
    }
  }
}

/**
 * Helper object to generate the DOT file, format it correctly and finally convert it to a PDF file.
 */
object DotGenerator {

  /** DOT output path */
  private final val OUTPUT_PATH = "output/%s/dot/"

  /** General dot diagrams settings */
  private final val dotSettings =
    """
      |	// Diagram settings
      |	graph [rankdir=LR labelloc=b, fontname=Arial, fontsize=14];
      |	node [ fontname=serif, fontsize=11, shape=Mrecord];
      |	edge [ fontname=Courier, color=dimgrey fontsize=12 ];
      |
      |	// Exported nodes from the components graph
    """.stripMargin

  private final val dotHeader =
    """// This file was auto-generated by DotGenerator version %s
      |// Visualisation of the '%s' program.""".stripMargin

  /**
   * Generate the dot file and save it.
   * @param ctx the program context
   * @return `true` if the dot file has been saved, `false` if an error occurred
   */
  def generateDotFile(ctx: Context, suffix: String): Boolean = {
    // Generate the dot source
    val fileName = s"${ctx.progName}$suffix.dot"
    val dot = generateDot(ctx.progName, fileName)

    // Create the folder if it not exist
    val folder: RichFile = new File(String.format(OUTPUT_PATH, ctx.progName))
    if (!folder.createFolder())
      return false // Error: unable to create the folder

    // Save the dot file to the disk
    val path = String.format(OUTPUT_PATH, ctx.progName) + fileName
    val f: RichFile = new File(path)
    f.write(dot) // Write succeed or not
  }

  /**
   * Generate the dot file and return it as a String.
   *
   * @param graphName the title to display on the graph
   * @param fileName the name of the generated file
   * @return the dot file as a String
   */
  def generateDot(graphName: String, fileName: String): String = {
    val dot = new GraphDot(graphName, fileName).generateDot()
    // Add static settings by hand after the first line
    val dotLines = dot.split("\\r?\\n\\t", 2)
    dotLines(0) += dotSettings
    // Add the file header
    val header = String.format(dotHeader, Version.getVersion, graphName)
    header + "\n" + dotLines.mkString
  }

  /**
   * Convert the generated dot file to a PDF file (with the same name).
   * @param progName the program name
   * @param suffix the suffix to append to the file name
   * @return the conversion command result
   */
  def convertDotToPdf(progName: String, suffix: String): (Int, String) = {
    val path = String.format(OUTPUT_PATH, progName)
    val fileName = s"${progName}$suffix"
    val dotFile = path + fileName + ".dot"
    val pdfFile = path + fileName + ".pdf"
    OSUtils.runWithCodeResult(s"dot $dotFile -Tpdf -o $pdfFile")
  }
}

/**
 * Display the graph generated by the [[ComponentManager]] in a `dot` diagram.
 *
 * Components are the nodes, connected together with ports. The label on the edge describe the type
 * of the connection - from an [[OutputPort]] to an [[InputPort]] of two components. Unconnected ports are labeled
 * as "NC". Components (nodes) with unconnected nodes are drawn in orange.
 * All components available in the graph are in the diagram.
 *
 * @param graphName the title of the program as graph legend
 * @param fileName the name of the file to display as graph legend
 */
private class GraphDot(graphName: String, fileName: String) {

  // General diagram settings
  private val name = "\"\\n\\nVisualisation of the '" + graphName + "' program.\\n" + fileName + "\""
  private val root = DotRootGraph(directed = true, id = Some("G"), kvList = Seq(DotAttr("label", name)))

  /**
   * Generate the DOT diagram of the `ComponentManager` graph.
   * @return the dot file as a String
   */
  def generateDot(): String = {
    // Generate the dot diagram and return it as String
    val g = ComponentManager.getDotGraph
    g.toDot(root, edgeTransformer,
      hEdgeTransformer = Option(edgeTransformer), /* hypergraph to the DOT language */
      iNodeTransformer = Option(nodeTrans), /* transform isolated nodes */
      cNodeTransformer = Option(nodeTrans)) /* transform connected nodes */
  }

  /* Helper methods to transform nodes and edges to dot. */

  /**
   * Transform all connected nodes.
   * @param innerNode graph nodes
   * @return the same transformation for all connected nodes of the graph
   */
  private def nodeTrans(innerNode: Graph[Component, LDiEdge]#NodeT):
  Option[(DotGraph, DotNodeStmt)] = {
    val n = innerNode.value.asInstanceOf[Component]

    // The label is something like: {{<in1>in1|<in2>in2}|Cmp[01]|{<out1>out1|<out2>out2}}
    val in = makeLabelList(n.getInputs.getOrElse(Nil))
    val out = makeLabelList(n.getOutputs.getOrElse(Nil))

    // Default node shape
    var shape = s"{{$in}|${nodeName(n)}|{$out}}" // Double '{' are necessary with rankdir=LR !
    val attrs = mutable.ArrayBuffer.empty[DotAttr]

    // Different shape if no input and no output. Draw a rectangle.
    if (in.isEmpty && out.isEmpty) {
      shape = s"${nodeName(n)}"
      attrs += DotAttr("color", "dimgrey")
      attrs += DotAttr("shape", "Record")
    }

    // Change the border color for unconnected nodes
    else if (!n.isConnected)
      attrs += DotAttr("color", "orange")

    Some(root, DotNodeStmt(nodeId(n), Seq(DotAttr("label", shape)) ++ attrs))
  }

  /**
   * Format the name of a Component to display it in a node.
   * @param c Component to display in a node
   * @return the node title value
   */
  private def nodeName(c: Component): String = {
    // Display the component id and description on two lines
    val title = c.name + s" [${c.getId}]"
    s"$title\\n${c.description}"
  }

  /**
   * Return the component ID as String.
   * @param c the Component
   * @return the ID as String
   */
  private def nodeId(c: Component): String = c.getId.toString // String ID to create a `DotNodeStmt`

  /**
   * Format a list of input or output of a component. Check if it is connected or not and display it.
   * @param l list of input or output of the component
   * @return list formatted for dot record structure
   */
  private def makeLabelList(l: Seq[Port[_]]) = {
    // Return the ID of the port with a label
    l.map(
      x => {
        val id = x.getId
        val nc = if (x.isNotConnected) " (NC)" else ""
        x match {
          case _: InputPort[_] => s"<$id>${x.name}\\n$nc" // In[$id]
          case _: OutputPort[_] => s"<$id>${x.name}\\n$nc" // In[$id]
        }
      }
    ).mkString("|")
  }

  /**
   * Transform all edges of the graph. Display the wire from an InputPort to an OutputPort of two components.
   * @param innerEdge graph edges
   * @return the same transformation for all edges of the graph
   */
  private def edgeTransformer(innerEdge: Graph[Component, LDiEdge]#EdgeT): Option[(DotGraph, DotEdgeStmt)] = {
    val edge = innerEdge.edge
    val label: Wire = edge.label.asInstanceOf[Wire]

    // Create the connection between two nodes. Example: "1:0 -> 2:0 [label = bool]"
    // Nodes are the components and identified by a unique ID, like ports.
    val nodeFrom = edge.from.value.asInstanceOf[Component].getId
    val nodeTo = edge.to.value.asInstanceOf[Component].getId
    val attrs = Seq(DotAttr("label", labelName(label)))
    Some(root, DotEdgeStmt(nodeFrom + ":" + label.from.getId, nodeTo + ":" + label.to.getId, attrs))
  }

  /**
   * Display the type of the connection.
   * @param w the wire to display as a edge label
   * @return connections types as a String, displayed as edge label
   */
  private def labelName(w: Wire): String = {
    // Display the type of the output port (from) -> the input port (to)
    // Example: "bool -> bool"
    s"${w.from.getTypeAsString}->${w.to.getTypeAsString}"
  }
}