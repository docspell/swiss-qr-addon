import sbt._

object Dependencies {
  object V {
    val cats = "2.8.0"
    val circe = "0.14.2"
    val circeYaml = "0.14.1"
    val decline = "2.3.0"
    val munit = "0.7.29"
    val munitCatsEffect = "1.0.7"
    val scala = "3.2.0"
    val fs2 = "3.2.14"
    val pdfbox = "2.0.26"
    val boofcv = "0.41"
    val slf4j = "2.0.1"
  }

  val circe = Seq(
    "io.circe" %% "circe-core" % V.circe,
    "io.circe" %% "circe-generic" % V.circe
  )
  val circeParser = Seq(
    "io.circe" %% "circe-parser" % V.circe
  )
  val circeYaml = Seq(
    "io.circe" %% "circe-yaml" % V.circeYaml
  )

  val cats = Seq(
    "org.typelevel" %% "cats-core" % V.cats
  )

  val boofcv = Seq(
    "org.boofcv" % "boofcv-core" % V.boofcv
  )

  val munit = Seq(
    "org.scalameta" %% "munit" % V.munit,
    "org.scalameta" %% "munit-scalacheck" % V.munit,
    "org.typelevel" %% "munit-cats-effect-3" % V.munitCatsEffect
  )

  val decline = Seq(
    "com.monovore" %% "decline" % V.decline,
    "com.monovore" %% "decline-effect" % V.decline
  )

  val fs2 = Seq(
    "co.fs2" %% "fs2-core" % V.fs2
  )
  val fs2Io = Seq(
    "co.fs2" %% "fs2-io" % V.fs2
  )

  val jclOverSlf4j = Seq(
    "org.slf4j" % "jcl-over-slf4j" % V.slf4j
  )

  val pdfbox = Seq(
    ("org.apache.pdfbox" % "pdfbox" % V.pdfbox).excludeAll(
      ExclusionRule("org.bouncycastle"),
      ExclusionRule("commons-logging")
    )
  ) ++ jclOverSlf4j
}
