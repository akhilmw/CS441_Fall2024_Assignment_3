package grpc

import scala.concurrent.{ExecutionContext, Future}
import bedrock.bedrock.BedrockServiceGrpc.BedrockService
import bedrock.bedrock.{QueryRequest, QueryResponse}
import org.slf4j.LoggerFactory
import io.circe.parser._
import io.circe.{Decoder, HCursor}
import io.circe.generic.auto._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.actor.ActorSystem
import scala.concurrent.duration._
import io.circe.syntax._

class BedrockServiceImpl(implicit ec: ExecutionContext, system: ActorSystem) extends BedrockService {
  private val logger = LoggerFactory.getLogger(getClass)
  private val apiGatewayUrl = "https://3schxcy90d.execute-api.us-east-1.amazonaws.com/prod/invokeBedrock"

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
        response.entity.toStrict(10.seconds)
      }
      .flatMap { strictEntity =>
        Future.successful(strictEntity.data.utf8String)
      }
      .flatMap { responseBody =>
        logger.debug(s"Response from Lambda: $responseBody")

        Future.fromTry {
          parse(responseBody).flatMap(_.as[LambdaResponse]) match {
            case Right(lambdaResponse) if lambdaResponse.statusCode == 200 =>
              parse(lambdaResponse.body).flatMap(_.as[BedrockResponse]) match {
                case Right(bedrockResponse) =>
                  scala.util.Success(
                    QueryResponse(
                      response = bedrockResponse.response,
                      model = bedrockResponse.model
                    )
                  )
                case Left(error) =>
                  scala.util.Success(
                    QueryResponse(
                      response = s"Error parsing Bedrock response: ${error.getMessage}",
                      model = "error"
                    )
                  )
              }
            case Right(lambdaResponse) =>
              scala.util.Success(
                QueryResponse(
                  response = s"Lambda returned error status: ${lambdaResponse.statusCode}",
                  model = "error"
                )
              )
            case Left(error) =>
              scala.util.Success(
                QueryResponse(
                  response = s"Failed to parse Lambda response: ${error.getMessage}",
                  model = "error"
                )
              )
          }
        }
      }
      .recover { case ex =>
        logger.error(s"Request failed: ${ex.getMessage}", ex)
        QueryResponse(
          response = s"Request failed: ${ex.getMessage}",
          model = "error"
        )
      }
  }
}