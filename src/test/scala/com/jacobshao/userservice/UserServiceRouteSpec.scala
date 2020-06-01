package com.jacobshao.userservice

import java.sql.SQLException

import com.jacobshao.userservice.config.ServerConfig
import com.jacobshao.userservice.repo.UserRepo
import io.circe.config.parser
import io.circe.generic.auto._
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import org.http4s._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe._
import org.http4s.client.Client
import org.http4s.implicits._
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{GivenWhenThen, OneInstancePerTest}

import scala.concurrent.duration._

class UserServiceRouteSpec extends AnyFlatSpec
  with OneInstancePerTest
  with GivenWhenThen
  with Matchers
  with MockitoSugar
  with ArgumentMatchersSugar
  with TestFixture {

  val serverConf: Task[ServerConfig] = parser.decodePathF[Task, ServerConfig]("server")
  val reqresBaseUri: Uri = serverConf.runSyncUnsafe().reqresBaseUri

  private val mockClient = mock[Client[Task]]
  private val mockRepo = mock[UserRepo]

  "POST /users" should "return 204 No Content when user creation is successful" in {
    when(mockClient.expectOption[ReqResUserResponse](*[Request[Task]])(*[EntityDecoder[Task, ReqResUserResponse]]))
      .thenReturn(Task.now(Some(someValidReqResUserResponse)))

    when(mockRepo.createUser(someUser)).thenReturn(Task.now(Right(1)))

    val userAlg = UserService.impl(mockRepo, mockClient, reqresBaseUri)
    val response: Task[Response[Task]] = UserServiceRoute(userAlg)
      .orNotFound
      .run(
        Request[Task](
          method = Method.POST,
          uri = uri"/users",
          body = fs2.Stream.emits(someValidUserCreationBody.getBytes())
        )
      )

    check[String](response, Status.NoContent) shouldBe true
  }

  it should "return 400 Bad Request when create user with malformed request body" in {

    val userAlg = UserService.impl(mockRepo, mockClient, reqresBaseUri)
    val response: Task[Response[Task]] = UserServiceRoute(userAlg)
      .orNotFound
      .run(
        Request[Task](
          method = Method.POST,
          uri = uri"/users",
          body = fs2.Stream.emits(someMalformedUserCreationBody.getBytes())
        )
      )

    check[String](response, Status.BadRequest, checkBody = false) shouldBe true
  }

  it should "return 400 Bad Request when create user with invalid email" in {

    val userAlg = UserService.impl(mockRepo, mockClient, reqresBaseUri)
    val response: Task[Response[Task]] = UserServiceRoute(userAlg)
      .orNotFound
      .run(
        Request[Task](
          method = Method.POST,
          uri = uri"/users",
          body = fs2.Stream.emits(someInValidEmailUserCreationBody.getBytes())
        )
      )

    val expectedResponseError = generateExpectedResponseError("-Invalid email address.")
    check[ResponseError](response, Status.BadRequest, checkBody = true, Some(expectedResponseError)) shouldBe true
  }

  it should "return 404 Not Found when user data is not available for the user id" in {
    when(mockClient.expectOption[ReqResUserResponse](*[Request[Task]])(*[EntityDecoder[Task, ReqResUserResponse]]))
      .thenReturn(Task.now(None))

    val userAlg = UserService.impl(mockRepo, mockClient, reqresBaseUri)
    val response: Task[Response[Task]] = UserServiceRoute(userAlg)
      .orNotFound
      .run(
        Request[Task](
          method = Method.POST,
          uri = uri"/users",
          body = fs2.Stream.emits(someInvalidUserIdUserCreationBody.getBytes())
        )
      )

    val expectedResponseError = generateExpectedResponseError(UserDataNotAvailableFailure.message)
    check[ResponseError](response, Status.NotFound, checkBody = true, Some(expectedResponseError)) shouldBe true
  }

  it should "return 409 Conflict when create user with an existing email" in {
    when(mockClient.expectOption[ReqResUserResponse](*[Request[Task]])(*[EntityDecoder[Task, ReqResUserResponse]]))
      .thenReturn(Task.now(Some(someValidReqResUserResponse)))

    when(mockRepo.createUser(someUser)).thenReturn(Task.now(Left(UserAlreadyExistsFailure)))

    val userAlg = UserService.impl(mockRepo, mockClient, reqresBaseUri)
    val response: Task[Response[Task]] = UserServiceRoute(userAlg)
      .orNotFound
      .run(
        Request[Task](
          method = Method.POST,
          uri = uri"/users",
          body = fs2.Stream.emits(someValidUserCreationBody.getBytes())
        )
      )

    val expectedResponseError = generateExpectedResponseError(UserAlreadyExistsFailure.message)
    check[ResponseError](response, Status.Conflict, checkBody = true, Some(expectedResponseError)) shouldBe true
  }

  it should "return 500 Internal Server Error when encounter database exception during user creation" in {
    when(mockClient.expectOption[ReqResUserResponse](*[Request[Task]])(*[EntityDecoder[Task, ReqResUserResponse]]))
      .thenReturn(Task.now(Some(someValidReqResUserResponse)))

    when(mockRepo.createUser(someUser)).thenReturn(Task.raiseError(new SQLException))

    val userAlg = UserService.impl(mockRepo, mockClient, reqresBaseUri)
    val response: Task[Response[Task]] = UserServiceRoute(userAlg)
      .orNotFound
      .run(
        Request[Task](
          method = Method.POST,
          uri = uri"/users",
          body = fs2.Stream.emits(someValidUserCreationBody.getBytes())
        )
      )

    check[ResponseError](response, Status.InternalServerError, checkBody = false) shouldBe true
  }

  it should "return 503 Service Unavailable when encounter exception during user creation" in {
    when(mockClient.expect[ReqResUserResponse](s"$reqResBaseUrl${someValidUserId.value}")).thenReturn(Task.now(someValidReqResUserResponse))

    when(mockRepo.createUser(someUser)).thenReturn(Task.raiseError(new Exception))

    val userAlg = UserService.impl(mockRepo, mockClient, reqresBaseUri)
    val response: Task[Response[Task]] = UserServiceRoute(userAlg)
      .orNotFound
      .run(
        Request[Task](
          method = Method.POST,
          uri = uri"/users",
          body = fs2.Stream.emits(someValidUserCreationBody.getBytes())
        )
      )

    check[ResponseError](response, Status.ServiceUnavailable, checkBody = false) shouldBe true
  }

  "GET /users/{email}" should "return 200 OK when user data is successfully retrieved" in {
    when(mockRepo.getUser(someValidUserEmail)).thenReturn(Task.now(Right(someUser)))

    val userAlg = UserService.impl(mockRepo, mockClient, reqresBaseUri)
    val response: Task[Response[Task]] = UserServiceRoute(userAlg)
      .orNotFound
      .run(
        Request[Task](
          method = Method.GET,
          uri = uri"/users" / s"${someValidUserEmail.value}"
        )
      )

    check[User](response, Status.Ok, checkBody = true, Some(someUser)) shouldBe true
  }

  it should "return 400 Bad Request when receiving an invalid email" in {

    val userAlg = UserService.impl(mockRepo, mockClient, reqresBaseUri)
    val response: Task[Response[Task]] = UserServiceRoute(userAlg)
      .orNotFound
      .run(
        Request[Task](
          method = Method.GET,
          uri = uri"/users" / s"${someInValidUserEmail.value}"
        )
      )

    val expectedResponseError = generateExpectedResponseError(InvalidEmailFormatFailure.message)
    check[ResponseError](response, Status.BadRequest, checkBody = true, Some(expectedResponseError)) shouldBe true
  }

  it should "return 404 Not Found when user does not exist" in {
    when(mockRepo.getUser(someValidUserEmail)).thenReturn(Task.raiseError(UserNotExistsFailure))

    val userAlg = UserService.impl(mockRepo, mockClient, reqresBaseUri)
    val response: Task[Response[Task]] = UserServiceRoute(userAlg)
      .orNotFound
      .run(
        Request[Task](
          method = Method.GET,
          uri = uri"/users" / s"${someValidUserEmail.value}"
        )
      )

    val expectedResponseError = generateExpectedResponseError(UserNotExistsFailure.message)
    check[ResponseError](response, Status.NotFound, checkBody = true, Some(expectedResponseError)) shouldBe true
  }

  it should "return 500 Internal Server Error when encounter database exception during user data retrieval" in {
    when(mockRepo.getUser(someValidUserEmail)).thenReturn(Task.raiseError(new SQLException))

    val userAlg = UserService.impl(mockRepo, mockClient, reqresBaseUri)
    val response: Task[Response[Task]] = UserServiceRoute(userAlg)
      .orNotFound
      .run(
        Request[Task](
          method = Method.GET,
          uri = uri"/users" / s"${someValidUserEmail.value}"
        )
      )

    check[ResponseError](response, Status.InternalServerError, checkBody = false) shouldBe true
  }

  it should "return 503 Service Unavailable when encounter exception during user data retrieval" in {
    when(mockRepo.getUser(someValidUserEmail)).thenReturn(Task.raiseError(new Exception))

    val userAlg = UserService.impl(mockRepo, mockClient, reqresBaseUri)
    val response: Task[Response[Task]] = UserServiceRoute(userAlg)
      .orNotFound
      .run(
        Request[Task](
          method = Method.GET,
          uri = uri"/users" / s"${someValidUserEmail.value}"
        )
      )

    check[ResponseError](response, Status.ServiceUnavailable, checkBody = false) shouldBe true
  }

  "DELETE /users/{email}" should "return 204 No Content when user is deleted" in {
    when(mockRepo.deleteUser(someValidUserEmail)).thenReturn(Task.now(1))

    val userAlg = UserService.impl(mockRepo, mockClient, reqresBaseUri)
    val response: Task[Response[Task]] = UserServiceRoute(userAlg)
      .orNotFound
      .run(
        Request[Task](
          method = Method.DELETE,
          uri = uri"/users" / s"${someValidUserEmail.value}"
        )
      )

    check[User](response, Status.NoContent) shouldBe true
  }

  it should "return 400 Bad Request when receiving an invalid email" in {

    val userAlg = UserService.impl(mockRepo, mockClient, reqresBaseUri)
    val response: Task[Response[Task]] = UserServiceRoute(userAlg)
      .orNotFound
      .run(
        Request[Task](
          method = Method.DELETE,
          uri = uri"/users" / s"${someInValidUserEmail.value}"
        )
      )

    val expectedResponseError = generateExpectedResponseError(InvalidEmailFormatFailure.message)
    check[ResponseError](response, Status.BadRequest, checkBody = true, Some(expectedResponseError)) shouldBe true
  }

  it should "return 500 Internal Server Error when encounter database exception during user deletion" in {
    when(mockRepo.deleteUser(someValidUserEmail)).thenReturn(Task.raiseError(new SQLException))

    val userAlg = UserService.impl(mockRepo, mockClient, reqresBaseUri)
    val response: Task[Response[Task]] = UserServiceRoute(userAlg)
      .orNotFound
      .run(
        Request[Task](
          method = Method.DELETE,
          uri = uri"/users" / s"${someValidUserEmail.value}"
        )
      )

    check[ResponseError](response, Status.InternalServerError, checkBody = false) shouldBe true
  }

  it should "return 503 Service Unavailable when encounter exception during user deletion" in {
    when(mockRepo.deleteUser(someValidUserEmail)).thenReturn(Task.raiseError(new Exception))

    val userAlg = UserService.impl(mockRepo, mockClient, reqresBaseUri)
    val response: Task[Response[Task]] = UserServiceRoute(userAlg)
      .orNotFound
      .run(
        Request[Task](
          method = Method.DELETE,
          uri = uri"/users" / s"${someValidUserEmail.value}"
        )
      )

    check[ResponseError](response, Status.ServiceUnavailable, checkBody = false) shouldBe true
  }

  def check[A](actual: Task[Response[Task]],
               expectedStatus: Status,
               checkBody: Boolean = true,
               expectedBody: Option[A] = None)(
                  implicit ev: EntityDecoder[Task, A]
              ): Boolean = {
    val actualResp = actual.runSyncUnsafe(5.seconds)
    val statusCheck = actualResp.status == expectedStatus
    val bodyCheck = if (checkBody) {
      expectedBody.fold[Boolean](
        actualResp.body.compile.toVector.runSyncUnsafe(5.seconds).isEmpty)(
        expected => actualResp.as[A].runSyncUnsafe(5.seconds) == expected
      )
    } else true
    statusCheck && bodyCheck
  }
}
