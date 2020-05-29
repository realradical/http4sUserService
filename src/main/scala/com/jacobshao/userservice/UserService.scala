package com.jacobshao.userservice

import cats.data.Validated.{Invalid, Valid}
import cats.implicits._
import com.jacobshao.userservice.repo.UserRepo
import monix.eval.Task
import org.http4s.client.Client

trait UserService {
  def create(userCreationRequest: UserCreationRequest): Task[Unit]
}

object UserService {
  def apply(implicit ev: UserService): UserService = ev

  def impl(userRepo: UserRepo, client: Client[Task]): UserService = new UserService {

    override def create(userRegister: UserCreationRequest): Task[Unit] =
      for {
        validEmail <- CreateRequestValidatorNec
          .validateRequest(userRegister.email) match {
          case Valid(email) => Task.now(email)
          case Invalid(nec) =>
            Task.raiseError(UserCreationRequestInvalidFailure(nec.foldLeft("")((b, dv) => b + "-" + dv.errorMessage)))
        }
        userData <- client.expect[ReqResUserResponse](s"$ReqResBaseUrl${userRegister.user_id.value}")
        user = User(userRegister.user_id, validEmail, userData.data.first_name, userData.data.last_name)
        _ <- userRepo.createUser(user).flatMap {
          case Right(_) => Task.unit
          case Left(e) => Task.raiseError(e)
        }
      } yield ()
  }
}
