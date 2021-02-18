package com.jacobshao.userservice

import cats.data.Validated.{Invalid, Valid}
import cats.implicits._
import com.jacobshao.userservice.repo.UserRepo
import monix.eval.Task
import org.http4s.client.Client
import org.http4s.{Method, Request, Uri}

trait UserService {
  def reqResBaseUri: Uri

  def create(userCreationRequest: UserCreationRequest): Task[Unit]

  def get(email: EmailAddress): Task[User]

  def delete(email: EmailAddress): Task[Unit]
}

object UserService {
  def apply(implicit ev: UserService): UserService = ev
}

class UserServiceIO(userRepo: UserRepo, client: Client[Task], baseUri: Uri) extends UserService {
  override def reqResBaseUri: Uri = baseUri

  override def create(userReq: UserCreationRequest): Task[Unit] =
    for {
      validEmail <- CreateRequestValidatorNec
        .validateRequest(userReq) match {
        case Valid(email) => Task.now(email)
        case Invalid(nec) =>
          Task.raiseError(
            UserCreationRequestInvalidFailure(
              nec.foldLeft("")((b, dv) => b + "-" + dv.errorMessage)
            )
          )
      }
      request = Request[Task](Method.GET, reqResBaseUri / s"${userReq.user_id.value}")
      userData <- client
        .expectOption[ReqResUserResponse](request)
        .flatMap {
          case Some(user) => Task.now(user)
          case None       => Task.raiseError(UserDataNotAvailableFailure)
        }
      user = User(userReq.user_id, validEmail, userData.data.first_name, userData.data.last_name)
      _ <- userRepo.createUser(user).flatMap {
        case Right(_) => Task.unit
        case Left(e)  => Task.raiseError(e)
      }
    } yield ()

  override def get(email: EmailAddress): Task[User] =
    for {
      validEmail <- CreateRequestValidatorNec
        .validateEmail(email) match {
        case Valid(email) => Task.now(email)
        case Invalid(_)   => Task.raiseError(InvalidEmailFormatFailure)
      }
      user <- userRepo.getUser(validEmail).flatMap {
        case Right(user) => Task(user)
        case Left(e)     => Task.raiseError(e)
      }
    } yield user

  override def delete(email: EmailAddress): Task[Unit] =
    for {
      validEmail <- CreateRequestValidatorNec
        .validateEmail(email) match {
        case Valid(email) => Task.now(email)
        case Invalid(_)   => Task.raiseError(InvalidEmailFormatFailure)
      }
      _ <- userRepo.deleteUser(validEmail)
    } yield ()
}
