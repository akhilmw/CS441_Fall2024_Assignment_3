package models

import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshaller}
import akka.http.scaladsl.model.MediaTypes.`application/json`
import io.circe.{Decoder, Encoder}
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import akka.http.scaladsl.model.HttpEntity

trait JsonSupport {
  implicit def circeUnmarshaller[A: Decoder]: FromEntityUnmarshaller[A] = {
    Unmarshaller.stringUnmarshaller
      .forContentTypes(`application/json`)
      .map { json =>
        decode[A](json) match {
          case Right(value) => value
          case Left(error) => throw error
        }
      }
  }

  implicit def circeMarshaller[A: Encoder]: ToEntityMarshaller[A] = {
    Marshaller.stringMarshaller(`application/json`)
      .compose[A](_.asJson.noSpaces)
  }
}