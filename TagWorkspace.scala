// import com.github.pathikrit.dijon._
import scala.sys.process._
import play.api.libs.json._
import play.api.libs.functional._

object TagWorkspace{

	val getWorkspacesCmd = Seq("i3-msg","-t","get_workspaces")
	val get_tree = Seq("i3-msg", "-t", "get_tree")
	val get_marks = Seq("i3-msg", "-t", "get_marks")

	def main(arg: Array[String]) = {
		implicit val fmt = Json.format[Node]
		
	    val treeAsJson = Json.parse(get_tree!!)

	    val nodes = Json.fromJson[Node](treeAsJson)
	    val nodeSeq = nodes.map(getNodes) 

	    val selections = 
	    	for(node <- nodeSeq.get if !node.name.contains("scratch");
	    		selection <- toSelections(node))
	    	yield  selection 

	    showSelections(selections)
	}

	def showSelections(selections:Seq[Selection]){
		val names = selections.map(_.displayName)
		val namesArg = names.mkString("\n");
		val echo = Seq("echo",namesArg)
		val dmenuResult = echo #| "dmenu" !

		val selection = selections.find(_.displayName == dmenuResult)
		selection.map(_.focus)
	}

	def getNodes(node: Node):Seq[Node] = {
		Seq(node) ++ node.nodes.flatMap(getNodes)
	}

	def toSelections(node: Node) = {
		node match {
			case Node(name,"con",_,None,Some(_)) => Some(new WindowSelection(name))
			case Node(name,"con",_,Some(mark),_) => Some(new MarkSelection(name,mark))
			case Node(name,"workspace",_,None,_) => Some(new WorkspaceSelection(name))
			case _ => None
		}	
	}
}

trait Selection{
	def displayName : String
	def focus() : Unit
}
class MarkSelection(name:String,mark:String) extends Selection{
	lazy val cmd = Seq("i3-msg","[con_mark=$name]","focus")
	def displayName =mark + ": " + name
	def focus() = cmd!!

}

class WindowSelection(name:String) extends Selection{
	lazy val cmd = Seq("i3-msg","[con=$name]","focus")
	def displayName = name
	def focus() = cmd!!
}

class WorkspaceSelection(name:String) extends Selection{

	lazy val cmd = Seq("i3-msg","workspace $name","focus")
	def displayName = "workspace: " + name
	def focus() = cmd!! 
}

case class Node(name:String, `type`:String, nodes:Seq[Node], mark:Option[String],window:Option[Int])


