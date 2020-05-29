package com.jacobshao.userservice

import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import org.http4s._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.client.Client
import org.http4s.implicits._
import org.mockito.MockitoSugar
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{GivenWhenThen, OneInstancePerTest}

import scala.concurrent.duration._

class UserServiceRouteSpec extends AnyFlatSpec
  with OneInstancePerTest
  with GivenWhenThen
  with Matchers
  with MockitoSugar
  with TestFixture {

  val mockClient = mock[Client[Task]]

  "UserServiceRoute" should "return 204 NoContent when user creation is successful" in {

    when(mockClient.expect[ReqResUserResponse](s"$ReqResBaseUrl${someValidUserId.value}")).thenReturn(Task.now(someValidReqResUserResponse))

    val userAlg = UserService.impl(someUserSuccessRepo, mockClient)
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

  it should "return 400 Bad Request when register with malformed request body" in {

    val userAlg = UserService.impl(someUserSuccessRepo, mockClient)
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

  it should "return 400 Bad Request when register with invalid email" in {

    val userAlg = UserService.impl(someUserSuccessRepo, mockClient)
    val response: Task[Response[Task]] = UserServiceRoute(userAlg)
      .orNotFound
      .run(
        Request[Task](
          method = Method.POST,
          uri = uri"/users",
          body = fs2.Stream.emits(someInValidUserCreationBody.getBytes())
        )
      )

    val expectedResponseError = generateExpectedResponseError("-Invalid email address.")
    check[ResponseError](response, Status.BadRequest, checkBody = true, Some(expectedResponseError)) shouldBe true
  }

  it should "return 409 Conflict when register with an existing email" in {
    when(mockClient.expect[ReqResUserResponse](s"$ReqResBaseUrl${someValidUserId.value}")).thenReturn(Task.now(someValidReqResUserResponse))

    val userAlg = UserService.impl(someUserAlreadyExistsFailRepo, mockClient)
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
