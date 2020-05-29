package com.jacobshao.userservice.cli

import cats.effect.Sync
import scopt.OParser

object ArgParser {

  private val zero = CliArgs(None, None, None)

  case class CliArgs(
      dbUrl: Option[String],
      dbUser: Option[String],
      dbPassword: Option[String]
  )

  private val builder = OParser.builder[CliArgs]

  private val parser = {
    import builder._
    OParser.sequence(
      programName("scopt"),
      head("User Service"),
      help("help").text(
        "User service stores and retrieves user information"
      ),
      opt[String]("dbUrl")
        .text("The connection url for the database connection.")
        .action((x, c) => c.copy(dbUrl = Some(x))),
      opt[String]("dbUser")
        .text("The user name for the database connection.")
        .action((x, c) => c.copy(dbUser = Some(x))),
      opt[String]("dbPassword")
        .text("The password for the database connection.")
        .action((x, c) => c.copy(dbPassword = Some(x)))
    )
  }

  def parse[F[_] : Sync](args: List[String]): F[CliArgs] =
    Sync[F].defer(
      OParser.parse(parser, args, zero) match {
        case Some(cliArgs) => Sync[F].pure(cliArgs)
        case _ => Sync[F].raiseError(new IllegalArgumentException("Illegal arguments"))
      }
    )
}
