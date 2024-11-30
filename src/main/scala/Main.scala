import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import grpc.GrpcServer
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Await}
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}
import org.slf4j.LoggerFactory

object Main extends App {
  private val logger = LoggerFactory.getLogger(getClass)

  implicit val system: ActorSystem = ActorSystem("bedrock-system")
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  // Start gRPC Server
  val grpcServer = new GrpcServer()
  grpcServer.start(50051)
  logger.info("gRPC Server started on port 50051")

  // Start HTTP Server
  val interface = "localhost"
  val port = 8080

  val routes = new Routes {}
  val bindingFuture = Http().newServerAt(interface, port).bind(routes.route)

  bindingFuture.onComplete {
    case Success(binding) =>
      logger.info(s"HTTP Server online at http://${binding.localAddress.getHostString}:${binding.localAddress.getPort}/")
    case Failure(ex) =>
      logger.error(s"Failed to bind HTTP server: ${ex.getMessage}", ex)
      system.terminate()
  }

  sys.addShutdownHook {
    logger.info("Shutting down servers...")
    grpcServer.stop()
    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate())
  }

  // Keep the main thread alive until the system terminates
  Await.result(system.whenTerminated, Duration.Inf)
}