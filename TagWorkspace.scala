import scala.sys.process._
import play.api.libs.json._
import play.api.libs.functional._


object TagWorkspace{

	def main(arg: Array[String]) = {
		implicit val fmt = Json.format[Node]

		val get_tree = Seq("i3-msg", "-t", "get_tree")
	    val treeAsJson = Json.parse(get_tree!!)

	    val root = Json.fromJson[Node](treeAsJson)
	    val nodeSeq = root.map(nodeAndChildren) 

	    val selections = 
	    	for(node <- nodeSeq.get if !node.name.contains("scratch");
	    		selection <- toSelections(node))
	    	yield  selection 

	    showSelections(selections)

	}

	def nodeAndChildren(node: Node):Seq[Node] = Seq(node) ++ node.nodes.flatMap(nodeAndChildren) 

	def toSelections(node: Node) = {
		node match {
			case Node(name,"con",id,_,None,Some(_)) => Some(new WindowSelection(id,name))
			case Node(name,"con",id,_,Some(mark),_) => Some(new MarkSelection(id,name,mark))
			case Node(name,"workspace",id,_,None,_) => Some(new WorkspaceSelection(id,name))
			case _ => None
		}	
	}

	def showSelections(selections:Seq[Selection]){
		val names = selections.map(_.displayName)
		val namesArg = names.mkString("\n");

		import java.io._
		import java.nio.charset.StandardCharsets

		val input = new ByteArrayInputStream(namesArg.getBytes(StandardCharsets.UTF_8))
		val dmenuResult = ( ("dmenu" #< input) !! ).trim

		val selection = selections.find(_.displayName == dmenuResult)
		
		selection.map(_.focus)
	}
}

case class Node(name:String, `type`:String, id:Int, nodes:Seq[Node], mark:Option[String],window:Option[Int])

trait Selection{
	def id:Int
	def displayName : String
	def focus() : Unit = {
		val cmd = Seq("i3-msg",s"[con_id=$id]","focus")
		cmd!!
	}
}

class MarkSelection(val id:Int,name:String,mark:String) extends Selection{
	def displayName = mark + ": " + name
	override def focus() ={
		val cmd = Seq("i3-msg",s"[con_mark=$mark]","focus")
		cmd!!
	} 

}

class WindowSelection(val id:Int,val displayName:String) extends Selection

class WorkspaceSelection(val id:Int,val name:String) extends Selection{
	def displayName = "workspace: " + name
}

