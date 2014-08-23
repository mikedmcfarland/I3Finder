import scala.sys.process._
import play.api.libs.json._
import play.api.libs.functional._
import scopt._

object I3Finder {

  case class Config(move: Boolean, dmenu: String, workspacePrefix: String)

  val argParser = new OptionParser[Config]("I3Finder") {
    head("I3Finder", "0.9")

    (opt[Unit]('m', "move")
      optional ()
      action { (x, c) => c.copy(move = true) }
      text ("grab element and move it to current workspace"))
    (opt[String]('d', "dmenu")
      optional ()
      action { (x, c) => c.copy(dmenu = x) }
      text ("arguments to pass along to demnu"))
    (opt[String]('w', "workspace-prefix")
      optional ()
      action { (x, c) => c.copy(workspacePrefix = x) }
      text ("workspace displayname prefix"))

    note("Calling I3Finder with no arguments results in a focus action")
    help("help") hidden ()
  }

  def main(args: Array[String]): Unit = {

    argParser.parse(args, Config(false, "", "workspace: ")) map { config =>

      implicit val format = Json.format[Node]

      val get_tree = Seq("i3-msg", "-t", "get_tree")
      val treeAsJson = Json.parse(get_tree!!)

      val root = Json.fromJson[Node](treeAsJson)
      val nodeSeq = root.map(nodeAndChildren)

      val selections =
        for (
          node <- nodeSeq.get if !node.name.contains("scratch");
          selection <- toSelections(node, config)
        ) yield selection

      val selection = makeSelection(selections, config.dmenu)

      selection.map(s => if (config.move) s.move else s.focus)

    } getOrElse {
      //arguments are bad
    }
  }

  def nodeAndChildren(node: Node): Seq[Node] = Seq(node) ++ node.nodes.flatMap(nodeAndChildren)

  def toSelections(node: Node, config: Config) = {
    node match {
      case Node(name, "con", id, _, None, Some(_)) => Some(new WindowSelection(id, name))
      case Node(name, "con", id, _, Some(mark), _) => Some(new MarkSelection(id, name, mark))
      case Node(name, "workspace", id, _, None, _) => Some(new WorkspaceSelection(id, config.workspacePrefix + name))
      case _ => None
    }
  }

  def makeSelection(selections: Seq[Selection], dmenuArgs: String): Option[Selection] = {
    val names = selections.map(_.displayName).mkString("\n");

    import java.io._
    import java.nio.charset.StandardCharsets
    //pipe names to dmenu over command line
    val input = new ByteArrayInputStream(names.getBytes(StandardCharsets.UTF_8))
    var dmenuResult = ""
    try {
      dmenuResult = s"dmenu $dmenuArgs" #< input !!
    } catch {
      //We exited dmenu, return no selection
      case e: RuntimeException => return None
    }
    val selectedDisplayName = dmenuResult.trim

    //find the selection made through dmenu
    selections.find(_.displayName == selectedDisplayName)
  }
}

//Reperesents nodes in the I3 Tree
case class Node(name: String, `type`: String, id: Int, nodes: Seq[Node], mark: Option[String], window: Option[Int])

trait Selection {
  def id: Int
  def displayName: String

  val cmd = Seq("i3-msg", s"[con_id=$id]")
  def focus(): Unit = cmd ++ Seq("focus") !!
  def move(): Unit = cmd ++ Seq("move", "workspace", "current") !!
}

class MarkSelection(val id: Int, name: String, mark: String) extends Selection {
  def displayName = mark + ": " + name
}

class WindowSelection(val id: Int, val displayName: String) extends Selection
class WorkspaceSelection(val id: Int, val displayName: String) extends Selection

