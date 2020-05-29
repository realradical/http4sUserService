package com.jacobshao.userservice.database

import cats.effect.{Blocker, Resource}
import com.jacobshao.userservice.cli.ArgParser.CliArgs
import com.jacobshao.userservice.config.DbConfig
import com.zaxxer.hikari.HikariConfig
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import monix.eval.Task

object Database {

  private def parseConnectionParams(cliArgs: CliArgs, dbConfig: DbConfig): ConnectionParams = {
    ConnectionParams(
      DbJdbcUrl(cliArgs.dbUrl.getOrElse(dbConfig.url)),
      DbUsername(cliArgs.dbUser.getOrElse(dbConfig.username)),
      DbPassword(cliArgs.dbPassword.getOrElse(dbConfig.password)),
      DbMaxPoolSize(dbConfig.poolSize)
    )
  }

  def transactor(cliArgs: CliArgs, dbConfig: DbConfig): Resource[Task, HikariTransactor[Task]] = {
    val connectionParams = parseConnectionParams(cliArgs, dbConfig)

    val config = new HikariConfig()

    config.setJdbcUrl(connectionParams.jdbcUrl.value)
    config.setUsername(connectionParams.userName.value)
    config.setPassword(connectionParams.password.value)
    config.setMaximumPoolSize(connectionParams.maxPoolSize.value)

    for {
      ce <- ExecutionContexts.fixedThreadPool[Task](8)
      be <- Blocker[Task]
      xa <- HikariTransactor.fromHikariConfig[Task](config, ce, be)
    } yield xa
  }

}
