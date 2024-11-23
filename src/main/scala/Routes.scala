import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.model.StatusCodes
import models._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

// AWS SDK Imports
import software.amazon.awssdk.services.bedrock.BedrockClient
import software.amazon.awssdk.services.bedrock.model._
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.core.SdkBytes

// JSON Parsing
import io.circe.parser._
import io.circe.generic.auto._

trait Routes extends JsonSupport {

  val route: Route = path("query") {
    post {
      entity(as[QueryRequest]) { queryRequest =>
        val responseFuture: Future[QueryResponse] = handleQuery(queryRequest)
        onSuccess(responseFuture) { queryResponse =>
          complete(queryResponse)
        }
      }
    }
  }

  def handleQuery(queryRequest: QueryRequest): Future[QueryResponse] = Future {
    // Create a Bedrock client
    val bedrockClient = BedrockClient.builder()
      .region(Region.US_EAST_1) // Replace with your AWS region
      .credentialsProvider(DefaultCredentialsProvider.create())
      .build()

    try {
      // Build the request to the model
      val prompt = queryRequest.query

      // Build the InvokeModelRequest
      val invokeModelRequest = InvokeModelRequest.builder()
        .modelId("amazon.titan-tg1-large") // Replace with your model ID
        .contentType("text/plain")
        .accept("application/json")
        .body(SdkBytes.fromUtf8String(prompt))
        .build()

      // Call the model
      val invokeModelResponse = bedrockClient.invokeModel(invokeModelRequest)

      // Read the response
      val responseBodyBytes = invokeModelResponse.body().asByteArray()
      val responseBodyString = new String(responseBodyBytes, "UTF-8")

      // Parse the JSON response to extract the generated text
      val generatedText = parse(responseBodyString) match {
        case Right(json) =>
          json.hcursor.get[String]("generated_text").getOrElse("Could not extract generated text")
        case Left(failure) =>
          s"Failed to parse response JSON: $failure"
      }

      // Return the QueryResponse
      QueryResponse(generatedText)
    } catch {
      case ex: BedrockException =>
        println(s"BedrockException: ${ex.getMessage}")
        QueryResponse("An error occurred while processing your request.")
      case ex: Exception =>
        println(s"Exception: ${ex.getMessage}")
        QueryResponse("An unexpected error occurred.")
    } finally {
      bedrockClient.close()
    }
  }
}

