package com.jacobshao.userservice.repo

import com.jacobshao.userservice.database.UserQuery
import com.jacobshao.userservice.{User, UserAlreadyExistsFailure}
import doobie.hikari.HikariTransactor
import doobie.implicits._
import doobie.postgres.sqlstate
import monix.eval.Task

trait UserRepo {
  def createUser(user: User): Task[Either[UserAlreadyExistsFailure.type, Int]]
}

object UserRepo {
  def apply(xa: HikariTransactor[Task]): UserRepo = new UserRepo {

    override def createUser(user: User): Task[Either[UserAlreadyExistsFailure.type, Int]] =
      UserQuery.insert(user).run.attemptSomeSqlState {
        case sqlstate.class23.UNIQUE_VIOLATION => UserAlreadyExistsFailure
      }.transact(xa)
  }
}
