package docspell.swissqr.addon

import cats.data.NonEmptyList
import cats.effect.*
import cats.syntax.all.*
import fs2.io.file.Path
import io.circe.syntax.*
import docspell.swissqr.{FileLoader, FileTypeTest, QrFromFile, SwissQR}
import docspell.swissqr.FileTypeTest.FileType

object Main extends IOApp:

  def run(args: List[String]): IO[ExitCode] =
    for {
      cfg <- args.headOption
        .map(path => Config.fromYamlFile[IO](Path(path)))
        .getOrElse(IO.pure(Config.default))
      _ <- errln("Reading environment data")
      env <- AddonEnv.fromEnv[IO]
      itemData <- env.itemMeta[IO]
      output <-
        if (isApplicable(itemData, cfg))
          processFiles(env)
            .flatTap(qrs => errln(s"Creating result data from ${qrs.size} qr codes"))
            .map { qrs =>
              NonEmptyList
                .fromList(qrs)
                .map(qrs => createOutput(itemData, qrs, cfg))
                .getOrElse(Output.empty)
            }
        else IO.pure(Output.empty)

      _ <- errln("Done")
      _ <- IO.println(output.asJson.noSpaces)
    } yield ExitCode.Success

  /** Checks whether the user wants to run this addon using the `checkTags` */
  def isApplicable(itemMetadata: ItemMetadata, cfg: Config): Boolean =
    cfg.checkTags.isEmpty || cfg.checkTagsMatch(
      itemMetadata.tags ::: itemMetadata.assumedTags
    )

  def processFiles(env: AddonEnv): IO[List[SwissQR]] =
    for {
      pdfProps <- env.pdfProperties[IO]
      origProps <- env.originalProperites[IO]
      fileTest = new FileTest(env, pdfProps, origProps)
      reader = QrFromFile(FileLoader[IO], fileTest)
      attachments = pdfProps.map(a => env.itemOriginalDir / a.id -> env.itemPdfDir / a.id)

      _ <- errln(s"Detecting QR codes in ${pdfProps.size} files")
      qrResults <- attachments
        .flatTraverse { case (orig, converted) =>
          reader
            .read(orig, 96f)
            .orElse(reader.read(converted, 96f))
            .value
            .map(_.toList.flatten)
        }
      (failed, ok) = qrResults.partitionEither(identity)
      _ <-
        if (failed.nonEmpty) errln(s"Failed to read ${failed.size} files")
        else IO.unit
      _ <- failed.traverse_(line => errln(s"- $line"))
      _ <-
        if (ok.isEmpty) errln("No QR codes found.")
        else errln(s"Found ${ok.size} qr codes")
    } yield ok

  /** Generates docspell commands from the given qr code information */
  def createOutput(
      itemMetadata: ItemMetadata,
      qrs: NonEmptyList[SwissQR],
      cfg: Config
  ): Output =
    val amounts: Map[String, BigDecimal] =
      qrs
        .map(_.amount)
        .groupBy(_.currency)
        .view
        .mapValues(_.map(_.amount))
        .withFilter((k, nel) => nel.exists(_.isDefined))
        .map((k, nel) => (k, nel.reduce(_ |+| _).getOrElse(BigDecimal(0))))
        .toMap

    val setFields: List[Output.Action] = amounts.toList.flatMap { case (cc, am) =>
      cfg.getFieldName(cc).map(field => Output.SetField(field, am.setScale(2).toString()))
    }
    val addTags: List[Output.Action] =
      cfg.addTags.toList.flatten match {
        case l if l.nonEmpty => List(Output.AddTags(l))
        case _               => Nil
      }
    val addNotes: Output.Action =
      Output.AddNotes(qrs.map(_.show).toList.mkString("\n\n"), "----")

    val setName: List[Output.Action] =
      if (cfg.additionalAsName.exists(identity))
        qrs.head.additional.headOption.toList.map(name => Output.SetName(name))
      else Nil

    Output(itemMetadata.id, addNotes :: setFields ::: addTags ::: setName)

  /** Print to stderr. */
  def errln(msg: String): IO[Unit] =
    IO(Console.err.println(msg))

  class FileTest(
      env: AddonEnv,
      attachments: List[FileProperties],
      originals: List[FileProperties]
  ) extends FileTypeTest[IO]:
    def getFileType(file: Path): IO[Option[FileType]] =
      val id = file.fileName.toString
      val parent = file.parent
      if (env.itemOriginalDir.some == parent) findMime(originals, id)
      else findMime(attachments, id)

    private def findMime(list: List[FileProperties], id: String) =
      list
        .find(_.id == id)
        .map(_.mimetype)
        .map {
          case m if m.endsWith("/pdf")     => IO(FileType.Pdf.some)
          case m if m.startsWith("image/") => IO(FileType.Image.some)
          case _                           => IO(None)
        }
        .getOrElse(IO(None))
