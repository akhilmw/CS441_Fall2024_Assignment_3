// src/main/scala/Server.scala
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import scala.concurrent.{Await, ExecutionContextExecutor}
import scala.concurrent.duration.Duration

object Server extends App with Routes {
//  implicit val system: ActorSystem = ActorSystem("bedrock-system")
//  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  val interface = "0.0.0.0"
  val port = 8080

  val bindingFuture = Http().newServerAt(interface, port).bind(route)

  bindingFuture.onComplete {
    case scala.util.Success(binding) =>
      println(s"Server online at http://${binding.localAddress.getHostString}:${binding.localAddress.getPort}/")
    case scala.util.Failure(exception) =>
      println(s"Failed to bind server: ${exception.getMessage}")
      system.terminate()
  }

  // Keep the application running
  Await.result(system.whenTerminated, Duration.Inf)
}
