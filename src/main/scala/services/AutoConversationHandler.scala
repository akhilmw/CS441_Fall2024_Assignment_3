package services

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import org.slf4j.LoggerFactory
import akka.actor.ActorSystem
import akka.pattern.after
import models.{QueryRequest, QueryResponse}

import scala.util.{Failure, Success, Try}

class AutoConversationHandler(
                               grpcClient: grpc.GrpcClientService,
                               conversationManager: ConversationManager,
                               recorder: ConversationRecorder
                             )(implicit ec: ExecutionContext, system: ActorSystem) {

  private val logger = LoggerFactory.getLogger(getClass)
  private val MaxTurns = 10

  def startAutonomousConversation(initialQuery: String): Future[String] = {
    val sessionId = java.util.UUID.randomUUID().toString
    logger.info(s"Starting autonomous conversation $sessionId with query: $initialQuery")

    conversationManager.initializeConversation(sessionId, initialQuery)
    processTurns(sessionId, initialQuery, 1)
  }

  private def processTurns(sessionId: String, query: String, turnCount: Int): Future[String] = {
    if (turnCount > MaxTurns) {
      logger.info(s"Reached max turns ($MaxTurns) for session $sessionId")
      saveAndFinish(sessionId)
    } else {
      logger.info(s"Processing turn $turnCount for session $sessionId")

      grpcClient.processQuery(QueryRequest(query)).flatMap { response =>
        recorder.appendToConversation(sessionId, (query, response.response))

        // Introduce a non-blocking delay
        akka.pattern.after(2.seconds, system.scheduler)(Future.unit).flatMap { _ =>
          conversationManager.generateNextQuery(sessionId, response.response).flatMap {
            case Some(nextQuery) =>
              logger.info(s"Generated next query for turn ${turnCount + 1}")
              // Recursively call processTurns with the new query and incremented turn count
              processTurns(sessionId, nextQuery, turnCount + 1)
            case None =>
              logger.info(s"No more queries to process for session $sessionId")
              saveAndFinish(sessionId)
          }
        }
      }.recoverWith { case ex =>
        logger.error(s"Error in process turn: ${ex.getMessage}", ex)
        saveAndFinish(sessionId)
      }
    }
  }


  private def saveAndFinish(sessionId: String): Future[String] = Future {
    try {
      conversationManager.getConversationHistory(sessionId).foreach { history =>
        recorder.saveConversation(sessionId, history)
      }
      sessionId
    } catch {
      case ex: Exception =>
        logger.error(s"Error saving conversation: ${ex.getMessage}")
        sessionId
    }
  }
}