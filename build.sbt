val Http4sVersion = "1.0.0-M29"
val CirceVersion = "0.14.1"
val MunitVersion = "0.7.29"
val LogbackVersion = "1.2.7"
val MunitCatsEffectVersion = "1.0.6"
val EnumeratumVersion = "1.7.0"
val MyScalaVersion = "2.13.6"
val DoobieVersion = "1.0.0-RC1"
val ScalaJsReactVersion = "2.0.0"
val MyProjectName = "simple-finance-tracker-v2"

scalaVersion := MyScalaVersion


lazy val basicSettings = Seq(
  organization := "org.big.pete",
  version := "0.1-SNAPSHOT",
  scalaVersion := MyScalaVersion,
  credentials += Credentials(Path.userHome / ".sbt" / ".credentials-github-repo"),
  resolvers += ("scala-toolz-github" at "https://maven.pkg.github.com/katastrofa/scala-toolz/")
)


lazy val shared = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("shared"))
  .settings(basicSettings)
  .settings(
    // https://mvnrepository.com/artifact/com.beachape/enumeratum
    libraryDependencies += "com.beachape" %%% "enumeratum" % EnumeratumVersion
  )
//  .jsConfigure(_.enablePlugins(ScalaJSWeb))

lazy val sharedJvm = shared.jvm
lazy val sharedJs = shared.js

lazy val db = (project in file("db"))
  .dependsOn(sharedJvm)
  .settings(basicSettings)
  .settings(
    name := s"$MyProjectName-db",
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
    libraryDependencies += "com.softwaremill.sttp.client3" %% "async-http-client-backend-cats" % "3.3.17",
    libraryDependencies += "org.tpolecat" %% "doobie-hikari" % DoobieVersion,
    libraryDependencies += "org.http4s" %% "http4s-ember-server" % Http4sVersion,
    libraryDependencies += "org.http4s" %% "http4s-circe" % Http4sVersion,
    libraryDependencies += "org.http4s" %% "http4s-dsl" % Http4sVersion,
    libraryDependencies += "ch.qos.logback" % "logback-classic" % LogbackVersion,
    libraryDependencies += "org.scalameta" %% "svm-subs" % "20.2.0",
    libraryDependencies += "com.typesafe" % "config" % "1.4.1",
    addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.13.2" cross CrossVersion.full),
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),

    scalacOptions += "-Ymacro-annotations"
  )

lazy val shapeFun = (project in file("shape-fun"))
  .settings(basicSettings)
  .settings(
    name := s"shape-fun",
    libraryDependencies += "com.chuusai" %% "shapeless" % "2.3.7"
  )

lazy val reactToolz = (project in file("react-toolz"))
  .enablePlugins(ScalaJSPlugin, ScalaJSWeb, ScalaJSBundlerPlugin)
  .settings(basicSettings)
  .settings(
    name := "react-toolz",
    scalaJSUseMainModuleInitializer := false,

    libraryDependencies += "com.beachape" %%% "enumeratum" % EnumeratumVersion,

    libraryDependencies +="com.github.japgolly.scalajs-react" %%% "core-ext-cats" % ScalaJsReactVersion,
    libraryDependencies +="com.github.japgolly.scalajs-react" %%% "core-ext-cats_effect" % ScalaJsReactVersion,
    libraryDependencies +="com.github.japgolly.scalajs-react" %%% "core-bundle-cats_effect" % ScalaJsReactVersion,

    libraryDependencies += "io.github.cquiroz" %%% "scala-java-time" % "2.3.0",
    libraryDependencies += "io.github.cquiroz" %%% "scala-java-time-tzdb" % "2.3.0",
    libraryDependencies += "com.chuusai" %%% "shapeless" % "2.3.7",

    Compile / npmDependencies ++= Seq(
      "react" -> "17.0.2",
      "react-dom" -> "17.0.2"
    )
  )

lazy val frontend = (project in file("frontend"))
  .dependsOn(sharedJs, reactToolz)
  .enablePlugins(ScalaJSPlugin, ScalaJSWeb, ScalaJSBundlerPlugin)
  .settings(basicSettings)
  .settings(
    name := s"$MyProjectName-frontend",
    scalaJSUseMainModuleInitializer := true,

    libraryDependencies +="com.github.japgolly.scalajs-react" %%% "core-ext-cats" % ScalaJsReactVersion,
    libraryDependencies +="com.github.japgolly.scalajs-react" %%% "core-ext-cats_effect" % ScalaJsReactVersion,
    libraryDependencies +="com.github.japgolly.scalajs-react" %%% "core-bundle-cats_effect" % ScalaJsReactVersion,

    libraryDependencies += "com.github.japgolly.scalajs-react" %%% "extra" % ScalaJsReactVersion,

    Compile / npmDependencies ++= Seq(
      "react" -> "17.0.2",
      "react-dom" -> "17.0.2"
    )
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
  .aggregate(shared.jvm, shared.js, db, backend, reactToolz, frontend)
