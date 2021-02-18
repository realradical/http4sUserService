package com.jacobshao.userservice

package object database {

  case class DbJdbcUrl(value: String) extends AnyVal

  case class DbUsername(value: String) extends AnyVal

  case class DbPassword(value: String) extends AnyVal

  case class DbMaxPoolSize(value: Int) extends AnyVal

  case class ConnectionParams(
      jdbcUrl: DbJdbcUrl,
      userName: DbUsername,
      password: DbPassword,
      maxPoolSize: DbMaxPoolSize
  )

}
