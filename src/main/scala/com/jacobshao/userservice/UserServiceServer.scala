package com.jacobshao.userservice

import cats.effect.ExitCode
import cats.implicits._
import com.jacobshao.userservice.cli.ArgParser
import com.jacobshao.userservice.cli.ArgParser.CliArgs
import com.jacobshao.userservice.config.Config
import com.jacobshao.userservice.database.Database
import com.jacobshao.userservice.repo.UserRepo
import com.typesafe.scalalogging.StrictLogging
import fs2.Stream
import monix.eval.{Task, TaskApp}
import nl.grons.metrics4.scala.DefaultInstrumented
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.client.middleware.{Retry, RetryPolicy, Logger => ClientLogger, Metrics => ClientMetrics}
import org.http4s.implicits._
import org.http4s.metrics.dropwizard.Dropwizard
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware._

import scala.concurrent.duration._

object UserServiceServer extends TaskApp with StrictLogging with DefaultInstrumented {

  def run(args: List[String]): Task[ExitCode] =
    for {
      cliArgs <- ArgParser.parse(args)
      _ <- Task(logger.info(
        s"""description="running with CLI args: ${
          cliArgs.copy(
            dbPassword = cliArgs.dbPassword.map(pw => s"***${pw.takeRight(4)})")
          )
        }" """))
      exitCode <- serverStream(cliArgs).compile.drain
        .as(ExitCode.Success)
        .onErrorHandleWith { t =>
          Task(logger.error("""description="Fatal failure" """, t)) *> Task.pure(ExitCode.Error)
        }
    } yield exitCode

  private def serverStream(cliArgs: CliArgs): Stream[Task, ExitCode] =
    for {
      Config(serverConfig, dbConfig) <- Stream.eval(Config.load())
      (tractor, client) <- (
        Stream.resource(Database.transactor(cliArgs, dbConfig)),
        Stream.resource(
          BlazeClientBuilder(scheduler).resource
            .map(client =>
              ClientMetrics(Dropwizard(metricRegistry, "client"))(
                ClientLogger(logHeaders = true, logBody = true)(
                  Retry[Task](RetryPolicy(RetryPolicy.exponentialBackoff(5.seconds, 5)))(client)
                )
              )
            )
        )
        ).mapN((_, _))
      implicit0(userService: UserService) = new UserServiceIO(
        UserRepo(tractor),
        client,
        serverConfig.reqresBaseUri
      )
      userRoute = UserServiceRoute.apply
      httpApp = Metrics(Dropwizard(metricRegistry, "server"))(
        Logger.httpRoutes(logHeaders = true, logBody = true)(userRoute)
      ).orNotFound
      exitCode <- BlazeServerBuilder[Task]
        .withBanner(Seq("http4s Server starts ****************************"))
        .bindHttp(serverConfig.port, serverConfig.host)
        .withHttpApp(httpApp)
        .withNio2(true)
        .serve
    } yield exitCode
}
