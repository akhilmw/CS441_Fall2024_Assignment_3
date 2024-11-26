import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ExceptionHandler, Route}
import akka.http.scaladsl.model._
import models._
import grpc.GrpcClientService
import services.ConversationManager
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import org.slf4j.LoggerFactory
import scala.util.{Failure, Success}
import io.circe.generic.auto._
import io.circe.syntax._

trait Routes extends JsonSupport {
  private val logger = LoggerFactory.getLogger(getClass)

  implicit val system: ActorSystem
  implicit val executionContext: ExecutionContext

  private val grpcClient = new GrpcClientService(
    host = "localhost",
    port = 50051
  )

  private val conversationManager = new ConversationManager()

  implicit def myExceptionHandler: ExceptionHandler =
    ExceptionHandler {
      case ex: Exception =>
        extractUri { uri =>
          logger.error(s"Request to $uri could not be handled normally", ex)
          complete(
            StatusCodes.InternalServerError ->
              ErrorResponse(Option(ex.getMessage).getOrElse("Internal server error"))
          )
        }
    }

  val route: Route = pathPrefix("query") {
    post {
      handleExceptions(myExceptionHandler) {
        entity(as[QueryRequest]) { queryRequest =>
          extractRequest { request =>
            val sessionId = request.headers
              .find(_.name() == "X-Session-ID")
              .map(_.value())
              .getOrElse(UUID.randomUUID().toString)

            logger.info(s"Processing request for session $sessionId: $queryRequest")

            if (request.headers.exists(_.name() == "X-Session-ID")) {
              processQuery(sessionId, queryRequest)
            } else {
              conversationManager.initializeConversation(sessionId, queryRequest.query)
              respondWithHeader(headers.RawHeader("X-Session-ID", sessionId)) {
                processQuery(sessionId, queryRequest)
              }
            }
          }
        }
      }
    } ~
      path("history" / Segment) { sessionId =>
        get {
          complete {
            conversationManager.getConversationHistory(sessionId) match {
              case Some(history) =>
                StatusCodes.OK -> history.map { case (q, r) =>
                  ApiResponse(response = r, nextQuery = Some(q))
                }
              case None =>
                StatusCodes.NotFound -> ErrorResponse("Conversation not found")
            }
          }
        }
      }
  }

  private def processQuery(sessionId: String, queryRequest: QueryRequest): Route = {
    onComplete(
      for {
        grpcResponse <- grpcClient.processQuery(queryRequest)
        nextQuery <- conversationManager.generateNextQuery(sessionId, grpcResponse.response)
      } yield (grpcResponse, nextQuery)
    ) {
      case Success((response, maybeNextQuery)) =>
        complete(
          StatusCodes.OK ->
            ApiResponse(
              response = response.response,
              nextQuery = maybeNextQuery
            )
        )

      case Failure(error) =>
        logger.error(s"Error processing query: ${error.getMessage}", error)
        complete(
          StatusCodes.InternalServerError ->
            ErrorResponse(Option(error.getMessage).getOrElse("Unknown error occurred"))
        )
    }
  }

  sys.addShutdownHook {
    logger.info("Shutting down services")
    grpcClient.shutdown()
  }
}