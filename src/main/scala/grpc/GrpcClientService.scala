package grpc

import io.grpc.netty.NettyChannelBuilder
import scala.concurrent.{ExecutionContext, Future, Promise}
import bedrock.bedrock.BedrockServiceGrpc
import bedrock.bedrock.{QueryRequest => GrpcQueryRequest}
import models.{QueryRequest, QueryResponse}
import org.slf4j.LoggerFactory
import scala.concurrent.duration._
import com.google.common.util.concurrent.{FutureCallback, Futures, ListenableFuture, MoreExecutors}

class GrpcClientService(host: String, port: Int)(implicit ec: ExecutionContext) {
  private val logger = LoggerFactory.getLogger(getClass)

  val channel = NettyChannelBuilder
    .forAddress(host, port)
    .usePlaintext()
    .build()

  val blockingStub = BedrockServiceGrpc.blockingStub(channel)

  private def toScalaFuture[A](guavaFuture: ListenableFuture[A]): Future[A] = {
    val promise = Promise[A]()

    val callback = new FutureCallback[A] {
      override def onSuccess(result: A): Unit = {
        promise.success(result)
      }
      override def onFailure(t: Throwable): Unit = {
        promise.failure(t)
      }
    }

    Futures.addCallback(
      guavaFuture,
      callback,
      MoreExecutors.directExecutor()
    )

    promise.future
  }

  def processQuery(request: QueryRequest): Future[QueryResponse] = {
    try {
      logger.info(s"Starting gRPC query process: ${request.query}")
      val grpcRequest = GrpcQueryRequest(query = request.query)

      val response = blockingStub
        .withDeadlineAfter(30, SECONDS)
        .processQuery(grpcRequest)

      logger.info(s"Received gRPC response: $response")

      if (response == null) {
        Future.failed(new RuntimeException("Received null response from gRPC server"))
      } else {
        val cleanResponse = Option(response.response).getOrElse("").trim
        Future.successful(QueryResponse(response = cleanResponse))
      }
    } catch {
      case ex: Exception =>
        logger.error(s"Error in gRPC call: ${ex.getMessage}", ex)
        Future.failed(ex)
    }
  }

  def shutdown(): Unit = {
    if (channel != null && !channel.isShutdown) {
      logger.info("Shutting down gRPC channel")
      channel.shutdown()
    }
  }
}