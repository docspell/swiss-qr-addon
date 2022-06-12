package docspell.swissqr.cli

import cats.syntax.all._
import com.monovore.decline._
import fs2.io.file.Path

case class Config(pdfScaleFactor: Float, file: Path)

object Config:

  private val factorOpt: Opts[Float] =
    Opts
      .option[Float](
        "pdf-dpi",
        "The dpi used to render images from PDF files. Default: 96"
      )
      .withDefault(96f)

  private val fileOpt: Opts[Path] =
    Opts
      .option[java.nio.file.Path](
        "file",
        "The txt, pdf or image file to look for qr codes"
      )
      .validate(s"File does not exist!")(p => java.nio.file.Files.exists(p))
      .map(Path.fromNioPath)

  val opts: Opts[Config] =
    (factorOpt, fileOpt).mapN(Config.apply)
