package com.jacobshao.userservice

import java.util.concurrent.Executors

import cats.effect.{Blocker, ExitCode}
import com.codahale.metrics.MetricFilter
import com.jacobshao.userservice.database.UserQuery
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import doobie.implicits._
import doobie.util.transactor.Transactor
import doobie.util.{ExecutionContexts, invariant}
import monix.eval.Task
import monix.execution.CancelableFuture
import monix.execution.Scheduler.Implicits.global
import okhttp3.mockwebserver.{MockResponse, MockWebServer}
import org.http4s.client.{Client, JavaNetClientBuilder}
import org.http4s.implicits._
import org.http4s.{Method, Request, Status}
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, GivenWhenThen, OptionValues}

import scala.jdk.CollectionConverters._

class UserServiceServerIntegrationSpec
  extends AnyFeatureSpec
    with GivenWhenThen
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with TestFixture
    with OptionValues
    with Matchers {

  private var postgres: EmbeddedPostgres = _
  private var jdbcUrl: String = _
  private var httpClient: Client[Task] = _
  private var userService: CancelableFuture[ExitCode] = _
  private var transactor: Transactor[Task] = _

  override protected def beforeAll(): Unit = {
    super.beforeAll()

    val blockingPool = Executors.newFixedThreadPool(1)
    val blocker = Blocker.liftExecutorService(blockingPool)
    httpClient = JavaNetClientBuilder[Task](blocker).create

    postgres = EmbeddedPostgres.builder().start()
    jdbcUrl = postgres.getJdbcUrl("postgres", "postgres")

    transactor = Transactor.fromDriverManager[Task](
      "org.postgresql.Driver",
      jdbcUrl,
      "postgres",
      "postgres",
      Blocker.liftExecutionContext(ExecutionContexts.synchronous)
    )

    sql"CREATE TABLE users (user_id int NOT NULL, email VARCHAR PRIMARY KEY, first_name VARCHAR, last_name VARCHAR)".update.run
      .transact(transactor)
      .runSyncUnsafe()

    userService = startUserServiceServer(jdbcUrl).runToFuture
  }

  override protected def afterAll(): Unit = {
    postgres.close()
    userService.cancel()
    super.afterAll()
  }

  override protected def beforeEach(): Unit = {
    UserServiceServer.metricRegistry.removeMatching(MetricFilter.ALL)

    sql"DELETE FROM users".update.run
      .transact(transactor)
      .runSyncUnsafe()
  }

  Feature("User Service creates user, get user data and delete user") {
    Scenario("should get user data from ReqRes endpoint and create user in our database") {
      withMockServer { server =>
        Given("ReqRes endpoint is online")
        server.enqueue(
          new MockResponse().setBody(someValidReqResResponse).setResponseCode(200)
        )

        When("A valid user creation POST request is received")
        val request = Request[Task](Method.POST, uri"http://localhost:8080/users")
          .withEntity(someValidUserCreationBody)
        val responseStatus = httpClient.status(request).runSyncUnsafe()

        Then("User is created in our database")
        server.getRequestCount shouldBe 1
        responseStatus shouldBe Status.NoContent

        val actualUser =
          UserQuery.select(someValidUserEmail).unique.transact(transactor).runSyncUnsafe()

        actualUser shouldBe someUser

        withClue("Total server POST request count recorded in the metrics registry") {
          getTimer("server.default.post-requests").getCount shouldBe 1
        }

        withClue("Total client GET request count recorded in the metrics registry") {
          getTimer("client.default.get-requests").getCount shouldBe 1
        }
      }
    }

    Scenario("should retry to get user data from ReqRes endpoint") {
      withMockServer { server =>
        Given("ReqRes endpoint is offline for the first two requests and back online after")
        server.enqueue(
          new MockResponse().setResponseCode(500)
        )
        server.enqueue(
          new MockResponse().setResponseCode(500)
        )
        server.enqueue(
          new MockResponse().setBody(someValidReqResResponse).setResponseCode(200)
        )

        When("A valid user creation POST request is received")
        val request = Request[Task](Method.POST, uri"http://localhost:8080/users")
          .withEntity(someValidUserCreationBody)
        val responseStatus = httpClient.status(request).runSyncUnsafe()

        Then("User is created in our database")
        responseStatus shouldBe Status.NoContent

        withClue("Retried 2 times") {
          server.getRequestCount shouldBe 3
        }

        val actualUser =
          UserQuery.select(someValidUserEmail).unique.transact(transactor).runSyncUnsafe()

        actualUser shouldBe someUser

        withClue("Total server POST request count recorded in the metrics registry") {
          getTimer("server.default.post-requests").getCount shouldBe 1
        }

        withClue("Total client GET request count recorded in the metrics registry") {
          getTimer("client.default.get-requests").getCount shouldBe 1
        }
      }
    }

    Scenario("should retrieve user data from our database") {
      withMockServer { server =>
        Given("ReqRes endpoint is online")
        server.enqueue(
          new MockResponse().setBody(someValidReqResResponse).setResponseCode(200)
        )

        Given("A valid user creation POST request is received")
        val postRequest = Request[Task](Method.POST, uri"http://localhost:8080/users")
          .withEntity(someValidUserCreationBody)
        httpClient.status(postRequest).runSyncUnsafe()
        server.getRequestCount shouldBe 1

        When("A valid user GET request is received")
        val getRequest = Request[Task](
          Method.GET,
          uri"http://localhost:8080/users" / s"${someValidUserEmail.value}"
        )
        val actualUser = httpClient.expect[User](getRequest).runSyncUnsafe()

        Then("User data is retrieved")
        actualUser shouldBe someUser

        withClue("Total server POST request count recorded in the metrics registry") {
          getTimer("server.default.post-requests").getCount shouldBe 1
        }

        withClue("Total server GET request count recorded in the metrics registry") {
          getTimer("server.default.get-requests").getCount shouldBe 1
        }

        withClue("Total client GET request count recorded in the metrics registry") {
          getTimer("client.default.get-requests").getCount shouldBe 1
        }
      }
    }

    Scenario("should delete user from our database") {
      withMockServer { server =>
        Given("ReqRes endpoint is online")
        server.enqueue(
          new MockResponse().setBody(someValidReqResResponse).setResponseCode(200)
        )

        Given("A valid user creation POST request is received")
        val postRequest = Request[Task](Method.POST, uri"http://localhost:8080/users")
          .withEntity(someValidUserCreationBody)
        httpClient.status(postRequest).runSyncUnsafe()
        server.getRequestCount shouldBe 1

        When("A valid user DELETE request is received")
        val deleteRequest = Request[Task](
          Method.DELETE,
          uri"http://localhost:8080/users" / s"${someValidUserEmail.value}"
        )
        val responseStatus = httpClient.status(deleteRequest).runSyncUnsafe()

        responseStatus shouldBe Status.NoContent

        assertThrows[invariant.UnexpectedEnd.type] {
          UserQuery.select(someValidUserEmail).unique.transact(transactor).runSyncUnsafe()
        }

        withClue("Total server POST request count recorded in the metrics registry") {
          getTimer("server.default.post-requests").getCount shouldBe 1
        }

        withClue("Total server GET request count recorded in the metrics registry") {
          getTimer("server.default.delete-requests").getCount shouldBe 1
        }

        withClue("Total client GET request count recorded in the metrics registry") {
          getTimer("client.default.get-requests").getCount shouldBe 1
        }
      }
    }
  }

  private def withMockServer(testCode: MockWebServer => Any) = {
    val server = new MockWebServer
    server.start(54472)
    try {
      testCode(server)
    } finally {
      server.shutdown()
    }
  }

  private def getTimer(meterName: String) = {
    val (_, consumedTimer) = UserServiceServer.metricRegistry
      .getTimers(MetricFilter.contains(meterName))
      .asScala
      .headOption
      .value
    consumedTimer
  }

  private def startUserServiceServer(dbUrl: String): Task[ExitCode] = UserServiceServer.run(
    List(
      "--dbUrl",
      jdbcUrl,
      "--dbUser",
      "postgres",
      "--dbPassword",
      "postgres"
    )
  )
}
