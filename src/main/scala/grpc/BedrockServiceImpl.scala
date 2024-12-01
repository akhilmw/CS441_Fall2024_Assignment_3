package grpc

import scala.concurrent.{ExecutionContext, Future}
import bedrock.bedrock.BedrockServiceGrpc.BedrockService
import bedrock.bedrock.{QueryRequest, QueryResponse}
import org.slf4j.LoggerFactory
import io.circe.parser._
import io.circe.generic.auto._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.actor.ActorSystem
import scala.concurrent.duration._
import io.circe.syntax._
import com.typesafe.config.ConfigFactory

class BedrockServiceImpl(implicit ec: ExecutionContext, system: ActorSystem) extends BedrockService {
  private val logger = LoggerFactory.getLogger(getClass)
  private val config = ConfigFactory.load()
  private val apiGatewayUrl = config.getString("apiGateway.url")

  case class LambdaResponse(statusCode: Int, body: String)
  case class BedrockResponse(response: String, model: String)
  case class LambdaError(errorMessage: String)

  override def processQuery(request: QueryRequest): Future[QueryResponse] = {
    logger.info(s"Processing gRPC query: ${request.query}")

    val payload = Map(
      "query" -> request.query
    ).asJson.noSpaces

    val httpRequest = HttpRequest(
      method = HttpMethods.POST,
      uri = apiGatewayUrl,
      entity = HttpEntity(ContentTypes.`application/json`, payload)
    )

    Http()
      .singleRequest(httpRequest)
      .flatMap { response =>
        response.entity.toStrict(10.seconds).map(_.data.utf8String)
      }
      .flatMap { responseBody =>
        logger.debug(s"Response from Lambda: $responseBody")

        Future.fromTry {
          if (responseBody.contains("errorMessage")) {
            decode[LambdaError](responseBody) match {
              case Right(error) =>
                if (error.errorMessage.contains("Task timed out")) {
                  logger.error(s"Lambda timeout: ${error.errorMessage}")
                  scala.util.Success(QueryResponse(
                    response = "The request took too long to process. Please try again.",
                    model = "error"
                  ))
                } else {
                  logger.error(s"Lambda error: ${error.errorMessage}")
                  scala.util.Success(QueryResponse(
                    response = "There was an error processing your request. Please try again later.",
                    model = "error"
                  ))
                }
              case Left(decodingError) => {
                logger.error(s"Failed to decode error response: ${decodingError.getMessage}")
                scala.util.Success(QueryResponse(
                  response = "Unexpected error format received. Please try again.",
                  model = "error"
                ))
              }
            }
          } else {
            for {
              lambdaResp <- parse(responseBody).flatMap(_.as[LambdaResponse]).toTry
              _ = logger.debug(s"Parsed Lambda response: $lambdaResp")
              result <- if (lambdaResp.statusCode == 200) {
                parse(lambdaResp.body).flatMap(_.as[BedrockResponse]).map { bedrockResp =>
                  QueryResponse(
                    response = bedrockResp.response,
                    model = bedrockResp.model
                  )
                }.toTry
              } else {
                scala.util.Success(
                  QueryResponse(
                    response = s"Request failed with status code: ${lambdaResp.statusCode}. Please try again.",
                    model = "error"
                  )
                )
              }
            } yield result
          }
        }
      }
      .recover {
        case ex: akka.stream.StreamTcpException =>
          logger.error(s"Network error: ${ex.getMessage}", ex)
          QueryResponse(
            response = "Network connection error. Please check your connection and try again.",
            model = "error"
          )
        case ex: java.util.concurrent.TimeoutException =>
          logger.error(s"Request timed out: ${ex.getMessage}", ex)
          QueryResponse(
            response = "The request timed out. Please try again.",
            model = "error"
          )
        case ex =>
          logger.error(s"Unexpected error: ${ex.getMessage}", ex)
          QueryResponse(
            response = "An unexpected error occurred. Please try again later.",
            model = "error"
          )
      }
  }
}