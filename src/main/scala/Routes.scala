// Routes.scala in bedrock-server
import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.model._
import models._
import grpc.GrpcClientService
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import io.circe.generic.auto._
import io.circe.syntax._

import scala.util.{Failure, Success}
import com.typesafe.config.ConfigFactory

trait Routes extends JsonSupport {
  private val logger = LoggerFactory.getLogger(getClass)
  private val config = ConfigFactory.load()
  private val grpcPort = config.getInt("grpc.server.port")
  private val grpcHost = config.getString("grpc.server.host")

  implicit val system: ActorSystem = ActorSystem("BedrockSystem")
  implicit val executionContext: ExecutionContext = system.dispatcher

  private val grpcClient = new GrpcClientService(
    host = grpcHost,
    port = grpcPort
  )

  case class BedrockResponse(
                              response: String
                            )

  val route: Route =
    pathPrefix("query") {
      post {
        entity(as[QueryRequest]) { queryRequest =>
          logger.info(s"Received query request: ${queryRequest.query}")

          onComplete(grpcClient.processQuery(queryRequest)) {
            case Success(response) =>
              complete(HttpResponse(
                status = StatusCodes.OK,
                entity = HttpEntity(
                  ContentTypes.`application/json`,
                  BedrockResponse(
                    response = response.response
                  ).asJson.noSpaces
                )
              ))

            case Failure(error) =>
              logger.error(s"Query processing failed: ${error.getMessage}", error)
              complete(HttpResponse(
                status = StatusCodes.InternalServerError,
                entity = HttpEntity(
                  ContentTypes.`application/json`,
                  s"""{"error":"Query processing failed: ${error.getMessage}"}"""
                )
              ))
          }
        }
      }
    } ~ // This ~ operator combines routes
      path("health") {
        get {
          complete(HttpResponse(
            status = StatusCodes.OK,
            entity = HttpEntity(
              ContentTypes.`application/json`,
              """{"status":"UP"}"""
            )
          ))
        }
      }

  sys.addShutdownHook {
    logger.info("Shutting down services")
    grpcClient.shutdown()
  }
}