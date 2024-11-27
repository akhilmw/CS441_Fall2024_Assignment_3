import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ExceptionHandler, Route}
import akka.http.scaladsl.model._
import models._
import grpc.GrpcClientService
import services.{ConversationManager, ConversationRecorder}
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import org.slf4j.LoggerFactory
import scala.util.{Failure, Success}
import io.circe.generic.auto._
import io.circe.syntax._

trait Routes extends JsonSupport {
  private val logger = LoggerFactory.getLogger(getClass)

  implicit val system: ActorSystem = ActorSystem("MyActorSystem")
  implicit val executionContext: ExecutionContext = system.dispatcher

  private val grpcClient = new GrpcClientService(
    host = "localhost",
    port = 50051
  )

  private val conversationManager = new ConversationManager()
  private val recorder = new ConversationRecorder()

  case class ConversationResponse(
                                   response: String,
                                   nextQuery: Option[String],
                                   sessionId: String,
                                   turnCount: Int
                                 )

  val route: Route = pathPrefix("query") {
    post {
      entity(as[QueryRequest]) { queryRequest =>
        val sessionId = UUID.randomUUID().toString
        logger.info(s"Starting conversation $sessionId with query: ${queryRequest.query}")

        conversationManager.initializeConversation(sessionId, queryRequest.query)

        def continueConversation(currentQuery: String, turnCount: Int): Future[List[ConversationResponse]] = {
          if (turnCount > 5) { // Max turns
            Future.successful(Nil)
          } else {
            for {
              // Get response from Bedrock via gRPC
              response <- grpcClient.processQuery(QueryRequest(currentQuery))
              _ = recorder.appendToConversation(sessionId, (currentQuery, response.response))

              // Get next query from Ollama
              nextQuery <- conversationManager.generateNextQuery(sessionId, response.response)

              // Create current turn response
              currentResponse = ConversationResponse(
                response = response.response,
                nextQuery = nextQuery,
                sessionId = sessionId,
                turnCount = turnCount
              )

              // Continue conversation if we have a next query
              remainingResponses <- nextQuery match {
                case Some(query) => continueConversation(query, turnCount + 1)
                case None => Future.successful(Nil)
              }

            } yield currentResponse :: remainingResponses
          }
        }

        onComplete(continueConversation(queryRequest.query, 1)) {
          case Success(responses) =>
            // Save final conversation
            val history = conversationManager.getConversationHistory(sessionId)
            history.foreach(recorder.saveConversation(sessionId, _))

            // Return the entire conversation
            complete(HttpResponse(
              status = StatusCodes.OK,
              entity = HttpEntity(
                ContentTypes.`application/json`,
                responses.asJson.noSpaces
              )
            ))

          case Failure(error) =>
            logger.error(s"Conversation failed: ${error.getMessage}", error)
            complete(HttpResponse(
              status = StatusCodes.InternalServerError,
              entity = HttpEntity(
                ContentTypes.`application/json`,
                s"""{"error":"Conversation failed: ${error.getMessage}"}"""
              )
            ))
        }
      }
    }
  }

  sys.addShutdownHook {
    logger.info("Shutting down services")
    grpcClient.shutdown()
  }
}