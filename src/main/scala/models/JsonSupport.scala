package models

import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, FromRequestUnmarshaller, Unmarshaller}
import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import io.circe.{Decoder, Encoder}
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.util.FastFuture
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait JsonSupport {
  // Response case classes
  case class ApiResponse(response: String, nextQuery: Option[String])
  case class ErrorResponse(error: String)

  // Implicit encoders/decoders
  implicit val apiResponseEncoder: Encoder[ApiResponse] = deriveEncoder[ApiResponse]
  implicit val errorResponseEncoder: Encoder[ErrorResponse] = deriveEncoder[ErrorResponse]
  implicit val queryRequestDecoder: Decoder[QueryRequest] = deriveDecoder[QueryRequest]
  implicit val queryResponseEncoder: Encoder[QueryResponse] = deriveEncoder[QueryResponse]

  // Basic entity unmarshaller
  implicit def circeUnmarshaller[A: Decoder]: FromEntityUnmarshaller[A] = {
    Unmarshaller
      .stringUnmarshaller
      .forContentTypes(`application/json`)
      .map { json =>
        decode[A](json) match {
          case Right(value) => value
          case Left(error) => throw error
        }
      }
  }

  // Request unmarshaller
  implicit val queryRequestFromRequestUnmarshaller: FromRequestUnmarshaller[QueryRequest] = {
    Unmarshaller.withMaterializer[HttpRequest, QueryRequest](_ => implicit mat => {
      request: HttpRequest =>
        request.entity.dataBytes.runFold(new StringBuilder)(_ append _.utf8String).map(_.toString()).flatMap { body =>
          decode[QueryRequest](body) match {
            case Right(value) => FastFuture.successful(value)
            case Left(error) => FastFuture.failed(error)
          }
        }
    })
  }

  // Entity marshaller
  implicit def circeMarshaller[A: Encoder]: ToEntityMarshaller[A] = {
    Marshaller.withFixedContentType(`application/json`) { obj =>
      HttpEntity(`application/json`, obj.asJson.noSpaces)
    }
  }
}