import java.nio.file.StandardCopyOption

val sftFullBuild = taskKey[Unit]("Builds the back-end assembly and front-end and pushes it into the back-end target folder")

val Http4sVersion = "1.0.0-M37"
val CirceVersion = "0.14.3"
//val MunitVersion = "0.7.29"
val LogbackVersion = "1.3.5"
//val MunitCatsEffectVersion = "1.0.6"
val EnumeratumVersion = "1.7.0"
val CatsEffectVersion = "3.3.14"
val MyScalaVersion = "2.13.7"
val DoobieVersion = "1.0.0-RC2"
val ScalaJsReactVersion = "2.1.1"
val ReactVersion = "17.0.2"
val MyProjectName = "simple-finance-tracker-v2"

scalaVersion := MyScalaVersion

lazy val basicSettings = Seq(
  organization := "org.big.pete",
  version := "0.3.1",
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
    libraryDependencies += "com.beachape" %%% "enumeratum" % EnumeratumVersion,
    libraryDependencies += "io.circe" %%% "circe-generic" % CirceVersion,
    libraryDependencies += "com.beachape" %%% "enumeratum-circe" % EnumeratumVersion
  )
//  .jsConfigure(_.enablePlugins(ScalaJSWeb))

lazy val sharedJvm = shared.jvm
lazy val sharedJs = shared.js

lazy val cache = (project in file("cache"))
  .settings(basicSettings)
  .settings(
    name := "scala-toolz-cache",
    libraryDependencies += "org.typelevel" %% "cats-effect" % CatsEffectVersion,
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
  )

lazy val db = (project in file("db"))
  .dependsOn(sharedJvm)
  .settings(basicSettings)
  .settings(
    name := s"$MyProjectName-db",
    libraryDependencies += "org.tpolecat" %% "doobie-core" % DoobieVersion,
    libraryDependencies += "io.circe" %% "circe-jawn" % CirceVersion,
    libraryDependencies += "org.wvlet.airframe" %% "airframe-log" % "22.11.4"
  )

lazy val backend = (project in file("backend"))
  .dependsOn(db, cache)
  .settings(basicSettings)
  .settings(
    name := s"$MyProjectName-backend",

    libraryDependencies += "com.softwaremill.sttp.client3" %% "cats" % "3.8.5",
    libraryDependencies += "org.tpolecat" %% "doobie-hikari" % DoobieVersion,
    libraryDependencies += "org.http4s" %% "http4s-ember-server" % Http4sVersion,
    libraryDependencies += "org.http4s" %% "http4s-circe" % Http4sVersion,
    libraryDependencies += "org.http4s" %% "http4s-dsl" % Http4sVersion,
    libraryDependencies += "ch.qos.logback" % "logback-classic" % LogbackVersion,
    libraryDependencies += "org.scalameta" %% "svm-subs" % "20.2.0",
    libraryDependencies += "com.typesafe" % "config" % "1.4.2",
    libraryDependencies += "mysql" % "mysql-connector-java" % "8.0.30",

    addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.13.2" cross CrossVersion.full),
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),

    scalacOptions += "-Ymacro-annotations",
    assembly / assemblyMergeStrategy := {
      case x if x.endsWith("netty.versions.properties") => MergeStrategy.concat
      case x if x.endsWith("module-info.class") => MergeStrategy.concat
      case x => (assembly / assemblyMergeStrategy).value.apply(x)
    }
  )

lazy val shapeFun = (project in file("shape-fun"))
  .settings(basicSettings)
  .settings(
    name := s"shape-fun",
    libraryDependencies += "com.chuusai" %% "shapeless" % "2.3.10"
  )

lazy val scalajsToolz = (project in file("scalajs-toolz"))
  .enablePlugins(ScalaJSPlugin, ScalaJSWeb, ScalaJSBundlerPlugin)
  .settings(basicSettings)
  .settings(
    name := "scalajs-toolz",
    scalaJSUseMainModuleInitializer := false,

    libraryDependencies += "io.circe" %%% "circe-parser" % CirceVersion,

    Compile / npmDependencies ++= Seq(
      "js-cookie" -> "3.0.1"
    )
  )

lazy val reactToolz = (project in file("react-toolz"))
  .enablePlugins(ScalaJSPlugin, ScalaJSWeb, ScalaJSBundlerPlugin)
  .settings(basicSettings)
  .settings(
    name := "react-toolz",
    scalaJSUseMainModuleInitializer := true,

    libraryDependencies += "com.beachape" %%% "enumeratum" % EnumeratumVersion,
    libraryDependencies +="com.github.japgolly.scalajs-react" %%% "core" % ScalaJsReactVersion,
    libraryDependencies += "com.github.japgolly.scalajs-react" %%% "extra" % ScalaJsReactVersion,
//    libraryDependencies += "com.github.japgolly.scalacss" %%% "ext-react" % "1.0.0",

    libraryDependencies += "io.github.cquiroz" %%% "scala-java-time" % "2.3.0",
    libraryDependencies += "io.github.cquiroz" %%% "scala-java-time-tzdb" % "2.3.0",
    libraryDependencies += "com.chuusai" %%% "shapeless" % "2.3.9",

    Compile / npmDependencies ++= Seq(
      "react" -> ReactVersion,
      "react-dom" -> ReactVersion
    )
  )

lazy val frontend = (project in file("frontend"))
  .dependsOn(sharedJs, reactToolz, scalajsToolz)
  .enablePlugins(ScalaJSPlugin, ScalaJSWeb, ScalaJSBundlerPlugin)
  .settings(basicSettings)
  .settings(
    name := s"$MyProjectName-frontend",
    scalaJSUseMainModuleInitializer := true,

    Compile / npmDependencies ++= Seq(
      "react" -> ReactVersion,
      "react-dom" -> ReactVersion
    ),

    Compile / fastOptJS / webpack := {
      val compiled = (Compile / fastOptJS / webpack).value
      val log = streams.value.log
      compiled.foreach { attributed =>
        val destinationPath = file(s"frontend/src/main/resources/ignore/${attributed.data.name}").toPath
        log.info(s"Copying: ${attributed.data} -> ${destinationPath.toString}")
        java.nio.file.Files.copy(attributed.data.toPath, destinationPath, StandardCopyOption.REPLACE_EXISTING)
      }
      compiled
    }
  )


lazy val root = (project in file("."))
  .settings(basicSettings)
  .settings(
    name := s"$MyProjectName-root",
    publish := {},
    sftFullBuild := {
      val targetFolder = target.value
      val deployFolder = s"${targetFolder.toString}/deployment"
      java.nio.file.Files.createDirectories(file(s"$deployFolder/static-assets/js").toPath)
      java.nio.file.Files.createDirectories(file(s"$deployFolder/static-assets/css").toPath)

      val frontendResourcesFolder = (frontend / Compile / resourceDirectory).value
      val reactToolzResourcesFolder = (reactToolz / Compile / resourceDirectory).value
      val backendResourcesFolder = (backend / Compile / resourceDirectory).value

      val jsFiles = (frontend / Compile / fullOptJS / webpack).value
      val log = streams.value.log

      val finalJsFiles = jsFiles.flatMap { attributed =>
        if (attributed.data.toString.endsWith(".js")) {
          val destFile = file(s"$deployFolder/static-assets/js/simple-finance-tracker-v2-frontend.js")
          log.info(s"Copying: ${attributed.data.toString} -> ${destFile.toString}")
          java.nio.file.Files.copy(attributed.data.toPath, destFile.toPath, StandardCopyOption.REPLACE_EXISTING)
          Some(destFile)
        } else None
      }

      val htmlSource = file(s"${frontendResourcesFolder.toString}/index-prod.html").toPath
      val htmlDestination = file(s"$deployFolder/static-assets/index-main.html")
      java.nio.file.Files.copy(htmlSource, htmlDestination.toPath, StandardCopyOption.REPLACE_EXISTING)

      val finalCssFiles = List(
        file(s"$frontendResourcesFolder/sft-v2-main.css"),
        file(s"$frontendResourcesFolder/my-materialize.css"),
        file(s"$reactToolzResourcesFolder/date-picker.css")
      ).map { fileToMove =>
        val destFile = file(s"$deployFolder/static-assets/css/${fileToMove.name}")
        log.info(s"Copying: ${fileToMove.toString} -> ${destFile.toString}")
        java.nio.file.Files.copy(fileToMove.toPath, destFile.toPath, StandardCopyOption.REPLACE_EXISTING)
        destFile
      }

      val backEndJar = (backend / assembly).value
      val backendJarDest = file(s"$deployFolder/simple-finance-tracker-v2.jar")
      java.nio.file.Files.copy(backEndJar.toPath, backendJarDest.toPath, StandardCopyOption.REPLACE_EXISTING)

      val logbackFileDest = file(s"$deployFolder/logback.xml")
      java.nio.file.Files.copy(file(s"${backendResourcesFolder.toString}/prod-logback.xml").toPath, logbackFileDest.toPath, StandardCopyOption.REPLACE_EXISTING)

      val allFiles = List(backendJarDest, logbackFileDest, htmlDestination) ++ finalJsFiles ++ finalCssFiles

      log.info("==================================================")
      log.info("Deployment Files:")
      allFiles.map(_.toString).foreach(log.info(_))
    }
//      "org.scalameta"   %% "munit"               % MunitVersion           % Test,
//      "org.typelevel"   %% "munit-cats-effect-3" % MunitCatsEffectVersion % Test,
//    testFrameworks += new TestFramework("munit.Framework")
  )
  .aggregate(shared.jvm, shared.js, cache, db, backend, scalajsToolz, reactToolz, frontend)
