package docspell.swissqr.cli

import cats.effect._
import cats.syntax.all.*
import com.monovore.decline.*
import com.monovore.decline.effect.*
import io.circe.syntax.*

import docspell.swissqr.*

object Main
    extends CommandIOApp(
      name = "swissqr",
      version = "0.1.0",
      header = "Read Swiss QR-Bill qr codes and print as JSON"
    ):

  def main: Opts[IO[ExitCode]] =
    Config.opts.map { cfg =>
      val loader = FileLoader[IO]
      val fileTest = FileTypeTest.fromExt[IO]
      QrFromFile(loader, fileTest).read(cfg.file, cfg.pdfScaleFactor).flatMap {
        case Some(qrs) =>
          val (failed, ok) = qrs.partitionEither(identity)
          for {
            _ <-
              if (failed.nonEmpty) {
                IO.println(s"${failed.size} failures!")
              } else IO.unit
            _ <-
              if (ok.isEmpty) {
                IO.println("No QR code found.")
              } else {
                ok.map(_.asJson.spaces2).traverse_(IO.println)
              }
          } yield ExitCode.Success
        case None =>
          IO.println("File type not supported! Only text, images or pdfs")
            .as(ExitCode.Error)
      }
    }
