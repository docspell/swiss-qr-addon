import com.github.sbt.git.SbtGit.GitKeys._

val sharedSettings = Seq(
  organization := "org.docspell.addon.swissqr",
  scalaVersion := Dependencies.V.scala,
  startYear := Some(2022),
  licenses += ("GPL-3.0-or-later", url(
    "https://spdx.org/licenses/GPL-3.0-or-later.html"
  )),
  javacOptions ++= Seq("-target", "1.8", "-source", "1.8"),
  scalacOptions ++= Seq(
    "-feature",
    "-deprecation",
    "-unchecked",
    "-encoding",
    "UTF-8",
    "-language:higherKinds",
    "-explain",
    "-explain-types",
    "-indent",
    "-print-lines",
    "-Ykind-projector",
    "-Xmigration",
    "-Xfatal-warnings"
  )
)

val buildInfoSettings = Seq(
  buildInfoKeys := Seq[BuildInfoKey](
    name,
    version,
    scalaVersion,
    sbtVersion,
    gitHeadCommit,
    gitHeadCommitDate,
    gitUncommittedChanges,
    gitDescribedVersion
  ),
  buildInfoOptions += BuildInfoOption.ToJson,
  buildInfoOptions += BuildInfoOption.BuildTime
)

// -- modules

val core = project
  .in(file("modules/core"))
  .withTestSettings
  .settings(sharedSettings)
  .settings(
    name := "docspell-swissqr-core",
    libraryDependencies ++=
      Dependencies.cats ++
        Dependencies.boofcv ++
        Dependencies.pdfbox ++
        Dependencies.circe ++
        Dependencies.fs2 ++
        Dependencies.fs2Io
  )

val cli = project
  .in(file("modules/cli"))
  .withTestSettings
  .settings(sharedSettings)
  .settings(
    name := "docspell-swissqr-cli",
    libraryDependencies ++=
      Dependencies.decline,
    assembly / mainClass := Some("docspell.swissqr.cli.Main"),
    assembly / assemblyPrependShellScript := Some(
      AssemblyPlugin.defaultUniversalScript()
    ),
    assembly / assemblyJarName := "swissqr-cli.jar"
  )
  .dependsOn(core)

val addon = project
  .in(file("modules/addon"))
  .enablePlugins(DocspellAddonPlugin)
  .withTestSettings
  .settings(sharedSettings)
  .settings(
    name := "swissqr-addon",
    libraryDependencies ++=
      Dependencies.circeYaml ++
        Dependencies.circeParser,
    assembly / mainClass := Some("docspell.swissqr.addon.Main"),
    assembly / assemblyPrependShellScript := Some(
      AssemblyPlugin.defaultUniversalScript()
    ),
    assembly / assemblyJarName := "swissqr-addon.jar",
    addonMetaDescription :=
      """
        |Detect Swiss QR-Bill QR codes in files and amend the 
        |item with this information.
        |
        |It must be run in the context of an item, allowing triggers: `final-process-item`,
        |`final-reprocess-item` and `existing-item`.
        |
        |Please see [the README](https://github.com/docspell/swiss-qr-addon) for
        |how to configure it. It works without a config as well.""".stripMargin,
    addonTriggers := List(
      "existing-item",
      "final-process-item",
      "final-reprocess-item"
    ),
    addonDockerEnable := true,
    addonNetworking := false,
    addonCollectOutput := true
  )
  .dependsOn(core, cli)

// -- root

val root = project
  .in(file("."))
  .settings(sharedSettings)
  .settings(
    name := "docspell-swissqr"
  )
  .aggregate(core, cli, addon)

addCommandAlias("ci", "test; addon/addonPackage")
addCommandAlias("make-pkg", "addon/addonPackage")
