package grpc

// Update these imports
import akka.actor.ActorSystem
import io.grpc.Server

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import org.slf4j.LoggerFactory
import io.grpc.netty.NettyServerBuilder

class GrpcServer(implicit executionContext: ExecutionContext, system: ActorSystem) {
  private val logger = LoggerFactory.getLogger(getClass)
  private[this] var server: Server = null

  def start(port: Int): Unit = {
    server = NettyServerBuilder
      .forPort(port)
      .addService(
        bedrock.bedrock.BedrockServiceGrpc.bindService(
          new BedrockServiceImpl(),
          executionContext
        )
      )
      .build()
      .start()

    logger.info(s"gRPC Server started on port $port")

    sys.addShutdownHook {
      logger.info("*** Shutting down gRPC server since JVM is shutting down")
      stop()
      logger.info("*** Server shut down")
    }
  }

  def stop(): Unit = {
    if (server != null) {
      server.shutdown()
    }
  }

  def blockUntilShutdown(): Unit = {
    if (server != null) {
      server.awaitTermination()
    }
  }
}

object GrpcServer extends App {
  implicit val system: ActorSystem = ActorSystem("grpc-server")
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  val server = new GrpcServer()
  server.start(50051)
  server.blockUntilShutdown()
}