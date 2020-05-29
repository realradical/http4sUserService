package com.jacobshao.userservice.config

import io.circe.config.parser
import io.circe.generic.auto._
import monix.eval.Task

case class ServerConfig(
    port: Int,
    host: String,
)

case class DbConfig(url: String, username: String, password: String, poolSize: Int)

case class Config(serverConfig: ServerConfig, dbConfig: DbConfig)

object Config {

  def load(): Task[Config] = {
    for {
      dbConf <- parser.decodePathF[Task, DbConfig]("db")
      serverConf <- parser.decodePathF[Task, ServerConfig]("server")
    } yield Config(serverConf, dbConf)
  }
}
