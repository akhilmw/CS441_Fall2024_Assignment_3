// src/main/scala/Server.scala
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.Materializer

object Server extends App with Routes {
  implicit val system: ActorSystem = ActorSystem("bedrock-system")
  implicit val materializer: Materializer = Materializer(system)
  implicit val executionContext = system.dispatcher

  val interface = "0.0.0.0"
  val port = 8080

  val bindingFuture = Http().newServerAt(interface, port).bind(route)

  println(s"Server online at http://$interface:$port/")
}
