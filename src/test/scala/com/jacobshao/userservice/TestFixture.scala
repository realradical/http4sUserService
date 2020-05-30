package com.jacobshao.userservice

import java.sql.SQLException

import com.jacobshao.userservice.repo.UserRepo
import monix.eval.Task

trait TestFixture {

  val someValidUserId: UserId = UserId(1)
  val someInValidUserId: UserId = UserId(99)
  val someValidUserEmail: EmailAddress = EmailAddress("someuser@test.com")
  val someInValidUserEmail: EmailAddress = EmailAddress("someusertest.com")
  val someValidUserFirstName = FirstName("Bob")
  val someValidUserLastName = LastName("McGrill")
  val someValidReqResUserResponse: ReqResUserResponse =
    ReqResUserResponse(UserData(someValidUserFirstName, someValidUserLastName))

  val someUser = User(someValidUserId, someValidUserEmail, someValidUserFirstName, someValidUserLastName)

  val someValidUserCreationBody = s"""{"email": "${someValidUserEmail.value}","user_id": "${someValidUserId.value}"}"""
  val someMalformedUserCreationBody = s"""{"email "${someValidUserEmail.value}","user_id": "${someValidUserId.value}"}"""
  val someInValidEmailUserCreationBody = s"""{"email": "${someInValidUserEmail.value}","user_id": "${someValidUserId.value}"}"""
  val someInvalidUserIdUserCreationBody = s"""{"email": "${someValidUserEmail.value}","user_id": "${someInValidUserId.value}"}"""

  val someUserSuccessRepo: UserRepo = new UserRepo {
    override def createUser(
                               user: User
                           ): Task[Either[UserAlreadyExistsFailure.type, Int]] = Task.now(Right(1))

    override def getUser(email: EmailAddress): Task[Either[UserNotExistsFailure.type, User]] =
      Task.now(Right(someUser))
  }

  val someUserConflictFailRepo: UserRepo = new UserRepo {
    override def createUser(
                               user: User
                           ): Task[Either[UserAlreadyExistsFailure.type, Int]] = Task.now(Left(UserAlreadyExistsFailure))

    override def getUser(email: EmailAddress): Task[Either[UserNotExistsFailure.type, User]] =
      Task.now(Left(UserNotExistsFailure))
  }

  val someUserSQLFailRepo: UserRepo = new UserRepo {
    override def createUser(
                               user: User
                           ): Task[Either[UserAlreadyExistsFailure.type, Int]] = Task.raiseError(new SQLException)

    override def getUser(email: EmailAddress): Task[Either[UserNotExistsFailure.type, User]] = Task.raiseError(new SQLException)
  }

  val someUserExceptionFailRepo: UserRepo = new UserRepo {
    override def createUser(
                               user: User
                           ): Task[Either[UserAlreadyExistsFailure.type, Int]] = Task.raiseError(new Exception)

    override def getUser(email: EmailAddress): Task[Either[UserNotExistsFailure.type, User]] = Task.raiseError(new Exception)
  }

  def generateExpectedResponseError(message: String): ResponseError = ResponseError(s"$message")
}
