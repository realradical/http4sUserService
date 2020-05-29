package com.jacobshao.userservice

import com.jacobshao.userservice.repo.UserRepo
import monix.eval.Task

trait TestFixture {

  val someValidUserId: UserId = UserId(1)
  val someValidUserEmail: EmailAddress = EmailAddress("someuser@test.com")
  val someInValidUserEmail: EmailAddress = EmailAddress("someusertest.com")
  val someValidReqResUserResponse: ReqResUserResponse = ReqResUserResponse(UserData(FirstName("Bob"), LastName("McGrill")))
  val someValidUserCreationBody = s"""{"email": "${someValidUserEmail.value}","user_id": "${someValidUserId.value}"}"""

  val someMalformedUserCreationBody = s"""{"email "${someValidUserEmail.value}","user_id": "${someValidUserId.value}"}"""

  val someInValidUserCreationBody = s"""{"email": "${someInValidUserEmail.value}","user_id": "${someValidUserId.value}"}"""

  val someUserSuccessRepo: UserRepo = new UserRepo {
    override def createUser(
        user: User
    ): Task[Either[UserAlreadyExistsFailure.type, Int]] = Task.now(Right(1))
  }

  val someUserAlreadyExistsFailRepo: UserRepo = new UserRepo {
    override def createUser(
        user: User
    ): Task[Either[UserAlreadyExistsFailure.type, Int]] = Task.now(Left(UserAlreadyExistsFailure))
  }

  def generateExpectedResponseError(message: String): ResponseError = ResponseError(s"$message")
}
