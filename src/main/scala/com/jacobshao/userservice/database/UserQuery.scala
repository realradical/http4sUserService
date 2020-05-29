package com.jacobshao.userservice.database

import com.jacobshao.userservice.{EmailAddress, User}
import doobie.implicits._

object UserQuery {

  def insert(user: User): doobie.Update0 = {
    sql"""
         |INSERT INTO users (
         |  user_id,
         |  email,
         |  first_name,
         |  last_name
         |)
         |VALUES (
         |  ${user.userId},
         |  ${user.email},
         |  ${user.firstName},
         |  ${user.lastName}
         |)
        """.stripMargin
      .update
  }

  def selectByEmail(email: EmailAddress): doobie.Query0[User] = {
    sql"""
         |SELECT
         |  user_id,
         |  email,
         |  first_name,
         |  last_name
         |FROM users
         |WHERE email = $email
        """.stripMargin
      .query[User]
  }
}
