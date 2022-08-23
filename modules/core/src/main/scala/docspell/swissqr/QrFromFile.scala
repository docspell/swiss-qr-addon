package docspell.swissqr

import cats.effect.*
import cats.syntax.all.*
import docspell.swissqr.FileTypeTest.FileType
import docspell.swissqr.*
import fs2.io.file.Path

trait QrFromFile[F[_]]:
  def read(file: Path, scaleFactor: Float): QrResult[F]

object QrFromFile:
  def apply[F[_]: Async](
      loader: FileLoader[F],
      fileTest: FileTypeTest[F]
  ): QrFromFile[F] =
    new QrFromFile[F]:
      def read(file: Path, scaleFactor: Float) =
        QrResult[F](fileTest.getFileType(file).flatMap {
          case Some(FileType.Pdf) =>
            loader
              .loadPDF(file)
              .use(doc => Sync[F].delay(QrReader.readPdf(doc, scaleFactor)))
              .map(_.toList.some)

          case Some(FileType.Image) =>
            loader.loadImage(file).map(QrReader.readImage).map(_.some)

          case Some(FileType.Text) =>
            loader.loadText(file).map(QrTextReader.read).map(List(_).some)

          case None =>
            None.pure[F]
        })
