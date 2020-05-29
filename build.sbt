val Http4sVersion = "0.21.1"
val CirceVersion = "0.13.0"
val LogbackVersion = "1.2.3"
val MonixVersion = "3.1.0"
val doobieVersion = "0.8.8"
val ScalaTestVersion = "3.1.1"
val SLoggingVersion = "3.9.2"
val CirceConfigVersion = "0.8.0"
val fuuidVersion = "0.3.0"
val Metrics4Scala = "4.1.5"
val Scopt = "4.0.0-RC2"
val Mockito = "1.14.3"

lazy val root = (project in file("."))
  .settings(
    organization := "com.jacobshao",
    name := "http4sUserService",
    version := "0.1",
    scalaVersion := "2.13.2",
    libraryDependencies ++= Seq(
      "com.github.scopt" %% "scopt" % Scopt,
      "org.http4s" %% "http4s-blaze-server" % Http4sVersion,
      "org.http4s" %% "http4s-blaze-client" % Http4sVersion,
      "org.http4s" %% "http4s-circe" % Http4sVersion,
      "org.http4s" %% "http4s-dsl" % Http4sVersion,
      "io.circe" %% "circe-generic" % CirceVersion,
      "io.circe" %% "circe-generic-extras" % CirceVersion,
      "io.circe" %% "circe-config" % CirceConfigVersion,
      "io.monix" %% "monix" % MonixVersion,
      "org.tpolecat" %% "doobie-core" % doobieVersion,
      "org.tpolecat" %% "doobie-postgres" % doobieVersion,
      "org.tpolecat" %% "doobie-hikari" % doobieVersion,
      "com.typesafe.scala-logging" %% "scala-logging" % SLoggingVersion,
      "io.chrisdavenport" %% "fuuid" % fuuidVersion,
      "io.chrisdavenport" %% "fuuid-doobie" % fuuidVersion,
      "io.chrisdavenport" %% "fuuid-circe" % fuuidVersion,
      "ch.qos.logback" % "logback-classic" % LogbackVersion,
      "nl.grons" %% "metrics4-scala" % Metrics4Scala,
      "org.scalatest" %% "scalatest" % ScalaTestVersion % Test,
      "org.mockito" %% "mockito-scala" % Mockito % Test
    ),
    addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3"),
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.0")
  )

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-language:higherKinds",
  "-language:postfixOps",
  "-feature",
  "-unchecked",
  "-Xfatal-warnings",
  "-Ywarn-numeric-widen",
  "-Xlint:inaccessible",
  "-Xlint:infer-any",
  "-Xlint:stars-align",
  "-Xlint:constant",
  "-Xlint:adapted-args"
)

assemblyOption in assembly := (assemblyOption in assembly).value.copy(cacheOutput = false)
assemblyJarName in assembly := "http4sUserService.jar"