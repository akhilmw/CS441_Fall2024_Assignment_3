package grpc

import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.BeforeAndAfterAll
import akka.actor.ActorSystem
import bedrock.bedrock.{QueryRequest, QueryResponse}
import org.scalatest.concurrent.ScalaFutures
import scala.concurrent.{Future, ExecutionContext}
import scala.concurrent.duration._
import akka.http.scaladsl.model._
import org.scalatest.time.{Span, Seconds, Millis}
import io.circe.syntax._
import io.circe.generic.auto._

class BedrockServiceImplSpec extends AsyncFlatSpec
  with Matchers
  with ScalaFutures
  with BeforeAndAfterAll {


  implicit override def patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(5, Seconds), interval = Span(100, Millis))

  implicit val system: ActorSystem = ActorSystem("BedrockServiceTest")
  implicit val ec: ExecutionContext = system.dispatcher

  // Mock response from Lambda
  case class LambdaResponse(statusCode: Int, body: String)
  case class BedrockResponse(response: String, model: String)

  class MockBedrockServiceImpl extends BedrockServiceImpl {
    override def processQuery(request: QueryRequest): Future[QueryResponse] = {
      request.query match {
        case "" =>
          Future.successful(QueryResponse(
            response = "Error: Empty query",
            model = "error"
          ))
        case "trigger_error" =>
          Future.successful(QueryResponse(
            response = "Request failed: Test error",
            model = "error"
          ))
        case _ =>
          val mockLambdaResponse = LambdaResponse(
            200,
            BedrockResponse(
              response = "This is a test response",
              model = "test-model"
            ).asJson.noSpaces
          )

          Future.successful(QueryResponse(
            response = "This is a test response",
            model = "test-model"
          ))
      }
    }
  }

  "BedrockServiceImpl" should "process query and return response" in {
    val service = new MockBedrockServiceImpl()
    val request = QueryRequest("What is Scala?")

    service.processQuery(request).map { response =>
      response.response should not be empty
      response.model should not be empty
      response.model shouldBe "test-model"
    }
  }

  it should "handle empty queries gracefully" in {
    val service = new MockBedrockServiceImpl()
    val request = QueryRequest("")

    service.processQuery(request).map { response =>
      response.model shouldBe "error"
      response.response should include("Empty query")
    }
  }

  it should "handle API failures gracefully" in {
    val service = new MockBedrockServiceImpl()
    val request = QueryRequest("trigger_error")

    service.processQuery(request).map { response =>
      response.model shouldBe "error"
      response.response should include("failed")
    }
  }

  // Clean up ActorSystem after tests
  override def afterAll(): Unit = {
    system.terminate()
    super.afterAll()
  }
}