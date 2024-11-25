

import Server.getClass
import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.model._
import akka.http.scaladsl.Http
import akka.util.ByteString
import models._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import scala.jdk.OptionConverters._
import org.slf4j.{Logger, LoggerFactory}


// AWS SDK Imports
import software.amazon.awssdk.auth.credentials.{AwsCredentialsProvider, DefaultCredentialsProvider}
import software.amazon.awssdk.auth.signer.Aws4Signer
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.http.{SdkHttpFullRequest, SdkHttpMethod}

// JSON Parsing
import io.circe.parser._
import io.circe.generic.auto._

import java.net.URI
import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters._

import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient
import software.amazon.awssdk.services.bedrockruntime.model.{InvokeModelRequest, InvokeModelResponse}
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.regions.Region


trait Routes extends JsonSupport {
  private val logger = LoggerFactory.getLogger(getClass)


  implicit val system: ActorSystem
  implicit val executionContext: ExecutionContext

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
    logger.info(s"Received query: ${queryRequest.query}")

    val region = Region.US_EAST_1 // Update to the region where Bedrock is available
    val modelId = "amazon.titan-text-lite-v1" // Update to the correct model ID

    // Build the payload
    val payload = s"""{
    "inputText": "${queryRequest.query}",
    "textGenerationConfig": {
      "maxTokenCount": 4096,
      "stopSequences": [],
      "temperature": 0.7,
      "topP": 0.9
    }
  }"""

    // Create the BedrockRuntimeClient
    val credentialsProvider = DefaultCredentialsProvider.create()
    val bedrockClient = BedrockRuntimeClient.builder()
      .region(region)
      .credentialsProvider(credentialsProvider)
      .build()

    try {
      // Create the InvokeModelRequest
      val invokeModelRequest = InvokeModelRequest.builder()
        .modelId(modelId)
        .contentType("application/json")
        .accept("application/json")
        .body(SdkBytes.fromUtf8String(payload))
        .build()

      // Invoke the model
      val response: InvokeModelResponse = bedrockClient.invokeModel(invokeModelRequest)
      val responseBody = response.body().asUtf8String()
      logger.debug(s"Response from Bedrock: $responseBody")

      // Parse the JSON response
      parse(responseBody) match {
        case Right(json) =>
          json.as[TextGenerationResponse] match {
            case Right(textGenResponse) =>
              textGenResponse.results.headOption match {
                case Some(result) =>
                  QueryResponse(result.outputText)
                case None =>
                  logger.error("No output text generated")
                  QueryResponse("No output text generated")
              }
            case Left(error) =>
              logger.error(s"Failed to parse response: ${error.getMessage}")
              QueryResponse("Failed to parse the response from Bedrock.")
          }
        case Left(error) =>
          logger.error(s"JSON parsing error: $error")
          QueryResponse("Failed to parse the response from Bedrock.")
      }
    } catch {
      case ex: Exception =>
        logger.error(s"Request failed: ${ex.getMessage}", ex)
        QueryResponse("An error occurred while processing your request.")
    } finally {
      bedrockClient.close()
    }
  }

}
