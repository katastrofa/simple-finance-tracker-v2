val Http4sVersion = "1.0.0-M29"
val CirceVersion = "0.14.1"
val MunitVersion = "0.7.29"
val LogbackVersion = "1.2.6"
val MunitCatsEffectVersion = "1.0.6"
val MyScalaVersion = "2.13.6"
val DoobieVersion = "1.0.0-RC1"
val MyProjectName = "simple-finance-tracker-v2"


lazy val basicSettings = Seq(
  organization := "org.big.pete",
  version := "0.1-SNAPSHOT",
  scalaVersion := MyScalaVersion,
  credentials += Credentials(Path.userHome / ".sbt" / ".credentials-github-repo"),
  resolvers += ("scala-toolz-github" at "https://maven.pkg.github.com/katastrofa/scala-toolz/"),
)

lazy val db = (project in file("db"))
  .settings(basicSettings)
  .settings(
    name := s"$MyProjectName-db",
    libraryDependencies += "org.big.pete" %% "scala-toolz-macros" % "0.1.0",
    libraryDependencies += "org.tpolecat" %% "doobie-core" % DoobieVersion,
    libraryDependencies += "io.circe" %% "circe-generic" % CirceVersion,
    libraryDependencies += "io.circe" %% "circe-jawn" % CirceVersion,
    libraryDependencies += "org.wvlet.airframe" %% "airframe-log" % "21.10.0"
  )

lazy val backend = (project in file("backend"))
  .dependsOn(db)
  .settings(basicSettings)
  .settings(
    name := s"$MyProjectName-backend",
    libraryDependencies += "com.softwaremill.sttp.client3" %% "async-http-client-backend-cats" % "3.3.16",
    libraryDependencies += "org.tpolecat" %% "doobie-hikari" % DoobieVersion,
    libraryDependencies += "org.http4s" %% "http4s-ember-server" % Http4sVersion,
//    libraryDependencies += "org.http4s" %% "http4s-ember-client" % Http4sVersion,
    libraryDependencies += "org.http4s" %% "http4s-circe" % Http4sVersion,
    libraryDependencies += "org.http4s" %% "http4s-dsl" % Http4sVersion,
    libraryDependencies += "ch.qos.logback" % "logback-classic" % LogbackVersion,
    libraryDependencies += "org.scalameta" %% "svm-subs" % "20.2.0",
    libraryDependencies += "com.typesafe" % "config" % "1.4.1",
    addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.13.2" cross CrossVersion.full),
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),

    scalacOptions += "-Ymacro-annotations"
  )

lazy val root = (project in file("."))
  .settings(basicSettings)
  .settings(
    name := s"$MyProjectName-root",
    publish := {}
//      "org.scalameta"   %% "munit"               % MunitVersion           % Test,
//      "org.typelevel"   %% "munit-cats-effect-3" % MunitCatsEffectVersion % Test,
//    testFrameworks += new TestFramework("munit.Framework")
  )
  .aggregate(db, backend)
