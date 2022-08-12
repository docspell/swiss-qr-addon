import com.typesafe.sbt.packager.Keys._
import com.typesafe.sbt.packager.archetypes.JavaServerAppPackaging
import com.typesafe.sbt.packager.docker._
import sbt._
import sbt.Keys._
import sbtassembly.AssemblyPlugin
import sbtassembly.AssemblyKeys._

object DocspellAddonPlugin extends AutoPlugin {

  override def requires = DockerPlugin && JavaServerAppPackaging && AssemblyPlugin
  override def trigger = AllRequirements

  object autoImport {
    val addonMetaName = settingKey[String]("Name of the addon")
    val addonMetaVersion = settingKey[String]("Version of the addon")
    val addonMetaDescription = settingKey[String]("Description")

    val addonTriggers = settingKey[List[String]]("The list of triggers for this addon")

    val addonNixEnable = settingKey[Boolean]("Enable the nix runner")
    val addonDockerEnable = settingKey[Boolean]("Enable the docker runner")
    val addonDockerImageName = settingKey[Option[String]]("The docker image name to use")
    val addonTrivialEnable = settingKey[Boolean]("Enable the trivial runner")

    val addonNetworking = settingKey[Boolean]("Allow networking for the addon")
    val addonCollectOutput = settingKey[Boolean]("The addon produces output")

    val addonCreateDescriptor = taskKey[File]("Creates the addon descriptor file")
    val addonPackage = taskKey[File]("Creates a package (zip) of the addon")
  }

  import autoImport._
  import DockerPlugin.autoImport._

  val dockerSettings = Seq(
    Docker / daemonUser := "root",
    dockerUpdateLatest := true,
    Docker / dockerUsername := Some("docspell"),
    dockerBaseImage := "openjdk:11"
  )

  val addonSettings = Seq(
    addonMetaName := name.value,
    addonMetaVersion := version.value,
    addonMetaDescription := description.value,
    addonTriggers := Nil,
    addonNixEnable := false,
    addonDockerEnable := false,
    addonDockerImageName :=
      (Docker / dockerUsername).value
        .map(user =>
          s"$user/${(Docker / packageName).value}:${(Docker / version).value}"
        ),
    addonTrivialEnable := true,
    addonNetworking := false,
    addonCollectOutput := true,
    addonCreateDescriptor :=
      createDescriptor(
        target.value / "addon",
        addonMetaName.value,
        addonMetaVersion.value,
        addonMetaDescription.value,
        addonTriggers.value,
        addonNixEnable.value,
        addonDockerEnable.value,
        addonDockerImageName.value,
        addonTrivialEnable.value,
        addonNetworking.value,
        addonCollectOutput.value
      ),
    addonPackage :=
      packageAddon(
        addonMetaName.value,
        addonMetaVersion.value,
        addonCreateDescriptor.value,
        assembly.value
      )
  )

  override def projectSettings =
    dockerSettings ++ addonSettings

  def packageAddon(
      name: String,
      version: String,
      descriptor: File,
      assemblyJar: File
  ): File = {
    val executable = s"$name-$version"
    val dir = descriptor.getParentFile
    val zipFile = dir / s"$executable.zip"

    IO.copyFile(assemblyJar, dir / executable)
    IO.zip(
      Seq(descriptor -> descriptor.getName, assemblyJar -> executable),
      zipFile,
      None
    )
    zipFile
  }

  def createDescriptor(
      directory: File,
      name: String,
      version: String,
      description: String,
      triggers: List[String],
      nixEnable: Boolean,
      dockerEnable: Boolean,
      dockerImage: Option[String],
      trivialEnable: Boolean,
      networking: Boolean,
      collectOutput: Boolean
  ): File = {
    val out = directory / "docspell-addon.yaml"
    IO.createDirectory(directory)
    val desc = description.trim.split("\r?\n").map(line => s"    $line").mkString("\n")
    val trig = triggers.map(line => s"""  - "$line"""").mkString("\n")
    val image = dockerImage.map(n => s"""image: "$n"""").getOrElse("")
    val executable = s"$name-$version"
    IO.write(
      out,
      s"""meta:
         |  name: "$name"
         |  version: "$version"
         |  description: |
         |$desc
         |    
         |triggers:
         |$trig
         |
         |runner:
         |  nix:
         |    enable: $nixEnable
         |  docker:
         |    enable: $dockerEnable
         |    $image
         |  trivial:
         |    enable: $trivialEnable
         |    exec: "$executable"
         |    
         |options:
         |  networking: $networking
         |  collectOutput: $collectOutput
         |""".stripMargin
    )
    out
  }
}
