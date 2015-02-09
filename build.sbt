import AssemblyKeys._

name := """product"""

version := "0.1.0"

scalaVersion := "2.11.5"

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "spray" at "http://repo.spray.io/"

resolvers += "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies ++= {
  val akkaV = "2.3.8"
  val sprayV = "1.3.2"
  Seq(
    "io.spray" %% "spray-can" % sprayV,
    "io.spray" %% "spray-routing" % sprayV,
    "io.spray" %% "spray-client" % sprayV,
    "io.spray" %% "spray-json" % "1.3.1",
    "org.reactivemongo" %% "reactivemongo" % "0.10.5.0.akka23",
    "ch.qos.logback" % "logback-classic" % "1.1.2",
    "io.netty" % "netty" % "3.6.5.Final" excludeAll(
      ExclusionRule(organization = "org.apache.logging.log4j")
      ),
    "io.spray" %% "spray-testkit" % sprayV % "test",
    "com.typesafe.akka" %% "akka-actor" % akkaV,
    "com.typesafe.akka" %% "akka-slf4j" % akkaV,
    "com.typesafe.akka" %% "akka-testkit" % akkaV % "test",
    "org.specs2" %% "specs2-core" % "2.3.11" % "test"
  )
}

assemblySettings

net.virtualvoid.sbt.graph.Plugin.graphSettings

mainClass in assembly := Some("de.leanovate.dose.product.Application")

val debian = TaskKey[Unit]("debian", "Create debian package")

debian <<= (assembly, baseDirectory, version) map { (asm, base, ver) =>
  val bintrayUser = System.getenv("BINTRAY_USER")
  val bintrayKey = System.getenv("BINTRAY_KEY")
  val release = ver + "-" + System.getenv("TRAVIS_BUILD_NUMBER")
  val debOut = (base / "target" / "microzon-product.deb")
  val debBase = (base / "target" / "deb")
  IO.copyFile(asm, debBase / "opt" / "product" / "product.jar")
  IO.copyFile(base / "src" / "main" / "supervisor" / "product.conf", debBase / "etc" / "supervisor" / "conf.d" / "product.conf")
  IO.write(debBase / "DEBIAN" / "control",
    s"""Package: microzon-product
    |Version: $release
    |Section: misc
    |Priority: extra
    |Architecture: all
    |Depends: supervisor, oracle-java8-installer
    |Maintainer: Bodo Junglas <landru@untoldwind.net>
    |Homepage: http://github.com/leanovate/microzon
    |Description: Product service
    |""".stripMargin)
  s"/usr/bin/fakeroot /usr/bin/dpkg-deb -b ${debBase.getPath} ${debOut.getPath}" !;
  s"/usr/bin/curl -T ${debOut.getPath} -u${bintrayUser}:${bintrayKey} https://api.bintray.com/content/untoldwind/deb/microzon/${ver}/pool/main/m/microzon/microzon-product-${release}_all.deb;deb_distribution=trusty;deb_component=main;deb_architecture=all?publish=1" !
}