import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ExceptionHandler, Route}
import akka.http.scaladsl.model._
import models._
import grpc.GrpcClientService

import scala.concurrent.{ExecutionContext, Future}
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success}
import io.circe.generic.auto._
import io.circe.syntax._

import scala.concurrent.duration._

trait Routes extends JsonSupport {
  private val logger = LoggerFactory.getLogger(getClass)

  implicit val system: ActorSystem
  implicit val executionContext: ExecutionContext

  private val grpcClient = new GrpcClientService(
    host = "localhost",
    port = 50051
  )

  implicit def myExceptionHandler: ExceptionHandler =
    ExceptionHandler {
      case ex: Exception =>
        extractUri { uri =>
          logger.error(s"Request to $uri could not be handled normally", ex)
          complete(
            HttpResponse(
              status = StatusCodes.InternalServerError,
              entity = HttpEntity(
                ContentTypes.`application/json`,
                s"""{"error":"Internal server error: ${ex.getClass.getSimpleName}"}"""
              )
            )
          )
        }
    }

  val route: Route = pathPrefix("query") {
    post {
      handleExceptions(myExceptionHandler) {
        entity(as[QueryRequest]) { queryRequest =>
          logger.info(s"Received request: $queryRequest")

          val resultFuture = grpcClient.processQuery(queryRequest)
            .map { response =>
              HttpResponse(
                status = StatusCodes.OK,
                entity = HttpEntity(
                  ContentTypes.`application/json`,
                  s"""{"response":"${response.response.replace("\"", "\\\"")}"}"""
                )
              )
            }
            .recover { case ex =>
              HttpResponse(
                status = StatusCodes.InternalServerError,
                entity = HttpEntity(
                  ContentTypes.`application/json`,
                  s"""{"error":"${ex.getMessage.replace("\"", "\\\"")}"}"""
                )
              )
            }

          complete(resultFuture)
        }
      }
    }
  }

  sys.addShutdownHook {
    logger.info("Shutting down gRPC client")
    grpcClient.shutdown()
  }
}