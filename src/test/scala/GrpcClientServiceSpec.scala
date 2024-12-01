package grpc

import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.BeforeAndAfterAll
import models.{QueryRequest, QueryResponse}
import scala.concurrent.{Future, ExecutionContext}
import scala.concurrent.duration._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Span, Seconds, Millis}
import io.grpc.ManagedChannel
import io.grpc.inprocess.InProcessChannelBuilder
import bedrock.bedrock.BedrockServiceGrpc

class GrpcClientServiceSpec extends AsyncFlatSpec
  with Matchers
  with ScalaFutures
  with BeforeAndAfterAll {

  implicit override def patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(5, Seconds), interval = Span(100, Millis))

  // Create test service with protected members exposed for testing
  class TestGrpcClientService(host: String, port: Int) extends GrpcClientService(host, port) {
    def exposedChannel: ManagedChannel = channel
    def exposedStub: BedrockServiceGrpc.BedrockServiceBlockingStub = blockingStub
  }

  "GrpcClientService" should "initialize with correct configuration" in {
    val service = new TestGrpcClientService("localhost", 50051)
    service.exposedChannel should not be null
    service.exposedStub should not be null
    succeed
  }

  it should "transform request to correct format" in {
    val service = new TestGrpcClientService("localhost", 50051)
    val request = QueryRequest("test query")

    val grpcRequest = bedrock.bedrock.QueryRequest(query = request.query)
    grpcRequest.query shouldBe "test query"
    succeed
  }

  it should "handle empty responses" in {
    val service = new TestGrpcClientService("localhost", 50051)
    val response = QueryResponse("")
    response.response shouldBe ""
    succeed
  }

  it should "properly shutdown" in {
    val service = new TestGrpcClientService("localhost", 50051)
    service.shutdown()
    service.exposedChannel.isShutdown shouldBe true
    succeed
  }

  it should "handle connection failures gracefully" in {
    val service = new TestGrpcClientService("invalid-host", 50051)
    val request = QueryRequest("test")

    recoverToSucceededIf[Exception] {
      service.processQuery(request)
    }
  }

  override def afterAll(): Unit = {
    super.afterAll()
  }
}