package grpc

import io.grpc.{ManagedChannel, ManagedChannelBuilder}
import io.grpc.netty.NettyChannelBuilder
import scala.concurrent.{ExecutionContext, Future}
import bedrock.bedrock.BedrockServiceGrpc
import bedrock.bedrock.{QueryRequest => GrpcQueryRequest, QueryResponse => GrpcQueryResponse}
import models.{QueryRequest, QueryResponse}
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
import scala.jdk.FutureConverters._
import scala.concurrent.duration._

class GrpcClientService(host: String, port: Int)(implicit ec: ExecutionContext) {
  private val logger = LoggerFactory.getLogger(getClass)

  logger.info(s"Creating gRPC channel to $host:$port")

  private val channel: ManagedChannel = NettyChannelBuilder
    .forAddress(host, port)
    .usePlaintext()
    .build()

  private val blockingStub = BedrockServiceGrpc.blockingStub(channel)

  def processQuery(request: QueryRequest): Future[QueryResponse] = {
    logger.info(s"Starting gRPC query process: ${request.query}")

    val completableFuture = new CompletableFuture[QueryResponse]()

    // Run in a separate thread
    new Thread(() => {
      try {
        val grpcRequest = GrpcQueryRequest(query = request.query)
        logger.info(s"Created gRPC request: $grpcRequest")

        val response = blockingStub
          .withDeadlineAfter(30, SECONDS)
          .processQuery(grpcRequest)

        logger.info(s"Received gRPC response: $response")

        if (response == null || response.response == null) {
          completableFuture.completeExceptionally(
            new RuntimeException("Received null response from gRPC server")
          )
        } else {
          completableFuture.complete(QueryResponse(response = response.response))
        }
      } catch {
        case ex: Exception =>
          logger.error(s"Error in gRPC call: ${ex.getMessage}", ex)
          completableFuture.completeExceptionally(ex)
      }
    }).start()

    // Convert Java CompletableFuture to Scala Future
    completableFuture.asScala
  }

  def shutdown(): Unit = {
    if (channel != null && !channel.isShutdown) {
      logger.info("Shutting down gRPC channel")
      channel.shutdown()
    }
  }
}