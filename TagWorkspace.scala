import com.github.pathikrit.dijon._
import scala.sys.process._


object TagWorkspace{

	val getWorkspacesCmd = Seq("i3-msg","-t","get_workspaces")
	val get_tree = Seq("i3-msg", "-t", "get_tree")
	val get_marks = Seq("i3-msg", "-t", "get_tree")
	def main(arg: Array[String]) = {

		val workspaces = parse(getWorkspacesCmd!!).toSeq


	    for(workspace <- workspaces;
	    	focused <- workspace.focused.as[Boolean] if focused;
	    	num <- workspace.num.as[Double]){
	    	println(num)
	    	println(workspace)	
	    }
	    
	    println("------")

	    workspaces.foreach(println)
	}
}
