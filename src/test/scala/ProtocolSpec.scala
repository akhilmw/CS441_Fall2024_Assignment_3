package models

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ProtocolSpec extends AnyFlatSpec with Matchers {

  "QueryRequest" should "be created with query string" in {
    val request = QueryRequest("test query")
    request.query shouldBe "test query"
  }

  "QueryResponse" should "clean response text" in {
    val response = QueryResponse("\u0000test response\n")
    response.cleanResponse shouldBe "test response"
  }

  it should "handle empty response" in {
    val response = QueryResponse("")
    response.cleanResponse shouldBe ""
  }

  "TextGenerationConfig" should "be created with correct parameters" in {
    val config = TextGenerationConfig(
      maxTokenCount = 100,
      temperature = 0.7,
      topP = 0.9,
      stopSequences = Seq("stop")
    )

    config.maxTokenCount shouldBe 100
    config.temperature shouldBe 0.7
    config.topP shouldBe 0.9
    config.stopSequences should contain("stop")
  }

  "TextGenerationResult" should "store output text" in {
    val result = TextGenerationResult("test output")
    result.outputText shouldBe "test output"
  }

  "TextGenerationResponse" should "contain results" in {
    val results = Seq(TextGenerationResult("output1"), TextGenerationResult("output2"))
    val response = TextGenerationResponse(results)
    response.results should have length 2
    response.results.head.outputText shouldBe "output1"
  }
}