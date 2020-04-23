name := """easiersolar"""

version := "1.0-SNAPSHOT"

testOptions in Test := Seq(Tests.Filter(s => s.endsWith("Test")))

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"
resolvers += Resolver.sonatypeRepo("releases")
resolvers += "tapquality" at "http://leadpath.staging.easiersolar.com"

lazy val root = (project in file(".")).enablePlugins(PlayScala, SbtWeb)

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  cache,
  evolutions,
  ws,
  // https://mvnrepository.com/artifact/com.glassdoor.planout4j/planout4j-api
  "com.glassdoor.planout4j" % "planout4j-api" % "1.2",
  // https://mvnrepository.com/artifact/com.glassdoor.planout4j/planout4j-tools
  "com.glassdoor.planout4j" % "planout4j-tools" % "1.2",
  "mysql" % "mysql-connector-java" % "5.1.34",
  "com.typesafe.play" %% "play-slick" % "2.0.0",
  "com.typesafe.play" %% "play-slick-evolutions" % "2.0.0",
  "com.github.tototoshi" %% "slick-joda-mapper" % "2.2.0",
  "joda-time" % "joda-time" % "2.7",
  "org.joda" % "joda-convert" % "1.7",
  "org.uaparser" % "uap-scala_2.11" % "0.1.0",
  "com.typesafe.play" % "play-mailer_2.11" % "5.0.0",
  "com.googlecode.libphonenumber" % "libphonenumber" % "7.7.2",
  "com.google.inject.extensions" % "guice-multibindings" % "4.0",
  "net.debasishg" %% "redisclient" % "3.2",
  "com.solarmosaic.client" %% "mail-client" % "0.1.0",
  // https://mvnrepository.com/artifact/commons-io/commons-io
  "commons-io" % "commons-io" % "2.5",
  "com.googlecode.json-simple" % "json-simple" % "1.1",
  "com.tapquality" % "shared-libs" % "1.0-SNAPSHOT",
  "com.amazonaws" % "aws-java-sdk" % "1.11.119",
  "com.google.code.gson" % "gson" % "2.8.0",
  specs2 % Test,
  filters,
  "com.h2database" % "h2" % "1.4.193" % Test,
  "be.objectify" % "deadbolt-scala_2.11" % "2.5.0"
)

routesGenerator := InjectedRoutesGenerator

resourceGenerators in Compile <+= (resourceManaged in Compile, name, version) map { (dir, n, v) =>

  var cwd: java.io.File = null
  if(java.nio.file.Files.exists(java.nio.file.Paths.get(".git"))) {
    cwd = new java.io.File(".")
  } else {
    cwd = new java.io.File("../../repo")
  }
  val file = dir / "git.properties"
  val contents = "git_hash=%s".format(sys.process.Process("git rev-parse --short HEAD", cwd).!!)
  IO.write(file, contents)
  var seq = Seq(file)

  val cacheBreakerFile = dir / "cache_breaker.properties"
  val cacheBreakerContents = "cache_breaker=%s".format(sys.process.Process("date +%Y%m%d%H%M%S", cwd).!!)
  IO.write(cacheBreakerFile, cacheBreakerContents)
  seq = seq :+ cacheBreakerFile

  seq
}

PlayKeys.playRunHooks += Grunt(baseDirectory.value)