package models

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import io.circe.syntax._
import io.circe.parser._
import io.circe.{Encoder, Decoder}
import io.circe.generic.auto._
import io.circe.generic.semiauto._

class JsonSupportSpec extends AnyFlatSpec with Matchers with JsonSupport with ScalatestRouteTest {

  // Explicit encoders and decoders
  implicit val queryRequestEncoder: Encoder[QueryRequest] = deriveEncoder[QueryRequest]
  override implicit val queryRequestDecoder: Decoder[QueryRequest] = deriveDecoder[QueryRequest]

  override implicit val queryResponseEncoder: Encoder[QueryResponse] = deriveEncoder[QueryResponse]
  implicit val queryResponseDecoder: Decoder[QueryResponse] = deriveDecoder[QueryResponse]

  override implicit val apiResponseEncoder: Encoder[ApiResponse] = deriveEncoder[ApiResponse]
  implicit val apiResponseDecoder: Decoder[ApiResponse] = deriveDecoder[ApiResponse]

  override implicit val errorResponseEncoder: Encoder[ErrorResponse] = deriveEncoder[ErrorResponse]
  implicit val errorResponseDecoder: Decoder[ErrorResponse] = deriveDecoder[ErrorResponse]

  "JsonSupport" should "marshal and unmarshal QueryRequest" in {
    val request = QueryRequest("test query")
    val json = request.asJson.noSpaces

    decode[QueryRequest](json) match {
      case Right(decoded) => decoded shouldBe request
      case Left(error) => fail(s"Failed to decode: $error")
    }
  }

  it should "marshal and unmarshal QueryResponse" in {
    val response = QueryResponse("test response")
    val json = response.asJson.noSpaces

    decode[QueryResponse](json) match {
      case Right(decoded) => decoded shouldBe response
      case Left(error) => fail(s"Failed to decode: $error")
    }
  }

  it should "marshal and unmarshal ApiResponse" in {
    val response = ApiResponse("test", Some("next"))
    val json = response.asJson.noSpaces

    decode[ApiResponse](json) match {
      case Right(decoded) =>
        decoded.response shouldBe "test"
        decoded.nextQuery shouldBe Some("next")
      case Left(error) => fail(s"Failed to decode: $error")
    }
  }

  it should "marshal and unmarshal ErrorResponse" in {
    val error = ErrorResponse("test error")
    val json = error.asJson.noSpaces

    decode[ErrorResponse](json) match {
      case Right(decoded) => decoded shouldBe error
      case Left(error) => fail(s"Failed to decode: $error")
    }
  }

  it should "handle malformed JSON" in {
    val malformedJson = """{"query": invalid}"""

    decode[QueryRequest](malformedJson) match {
      case Right(_) => fail("Should not parse invalid JSON")
      case Left(_) => succeed
    }
  }

  it should "handle missing fields" in {
    val incompleteJson = "{}"

    decode[QueryRequest](incompleteJson) match {
      case Right(_) => fail("Should not parse incomplete JSON")
      case Left(_) => succeed
    }
  }

  it should "handle null values in optional fields" in {
    val jsonWithNull = """{"response":"test","nextQuery":null}"""

    decode[ApiResponse](jsonWithNull) match {
      case Right(decoded) =>
        decoded.response shouldBe "test"
        decoded.nextQuery shouldBe None
      case Left(error) => fail(s"Failed to decode: $error")
    }
  }
}