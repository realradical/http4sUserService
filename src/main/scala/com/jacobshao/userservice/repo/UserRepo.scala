package com.jacobshao.userservice.repo

import com.jacobshao.userservice.database.UserQuery
import com.jacobshao.userservice.{
  EmailAddress,
  User,
  UserAlreadyExistsFailure,
  UserNotExistsFailure
}
import doobie.hikari.HikariTransactor
import doobie.implicits._
import doobie.postgres.sqlstate
import doobie.util.invariant
import monix.eval.Task

trait UserRepo {
  def createUser(user: User): Task[Either[UserAlreadyExistsFailure.type, Int]]

  def getUser(email: EmailAddress): Task[Either[UserNotExistsFailure.type, User]]

  def deleteUser(email: EmailAddress): Task[Int]
}

object UserRepo {
  def apply(xa: HikariTransactor[Task]): UserRepo = new UserRepo {

    override def createUser(user: User): Task[Either[UserAlreadyExistsFailure.type, Int]] =
      UserQuery
        .insert(user)
        .run
        .attemptSomeSqlState { case sqlstate.class23.UNIQUE_VIOLATION =>
          UserAlreadyExistsFailure
        }
        .transact(xa)

    override def getUser(email: EmailAddress): Task[Either[UserNotExistsFailure.type, User]] =
      UserQuery.select(email).unique.transact(xa).map(Right(_)).onErrorHandle {
        case invariant.UnexpectedEnd => Left(UserNotExistsFailure)
      }

    override def deleteUser(email: EmailAddress): Task[Int] =
      UserQuery.delete(email).run.transact(xa)
  }
}
