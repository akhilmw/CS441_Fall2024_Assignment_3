// src/main/scala/Server.scala
import akka.actor.ActorSystem
import akka.http.scaladsl.Http

import scala.concurrent.{Await, ExecutionContextExecutor}
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

object Server extends App with Routes {

  /**
   * Main HTTP server entry point that sets up and manages the server lifecycle.
   * Handles server binding, startup, and graceful shutdown.
   */

  val interface = "localhost"
  val port = 8080
  // Bind server to configured host and port
  val bindingFuture = Http().newServerAt(interface, port).bind(route)

  bindingFuture.onComplete {
    case Success(binding) =>
      println(s"Server online at http://${binding.localAddress.getHostString}:${binding.localAddress.getPort}/")
    case Failure(exception) =>
      println(s"Failed to bind server: ${exception.getMessage}")
      system.terminate()
  }

  // Ensure graceful shutdown when JVM terminate
  sys.addShutdownHook {
    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate())
  }

  Await.result(system.whenTerminated, Duration.Inf)
}
