package models

case class QueryRequest(query: String)
case class QueryResponse(response: String)

// Bedrock specific models
case class TextGenerationConfig(
                                 maxTokenCount: Int,
                                 temperature: Double,
                                 topP: Double,
                                 stopSequences: Seq[String]
                               )

case class TextGenerationResult(
                                 outputText: String
                               )

case class TextGenerationResponse(
                                   results: Seq[TextGenerationResult]
                                 )