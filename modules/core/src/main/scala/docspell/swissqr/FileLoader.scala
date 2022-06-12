package docspell.swissqr

import cats.effect.*
import fs2.Stream
import fs2.io.file.{Files, Path}
import org.apache.pdfbox.pdmodel.PDDocument

import java.awt.image.BufferedImage
import javax.imageio.ImageIO

trait FileLoader[F[_]]:
  def loadPDF(file: Path): Resource[F, PDDocument]
  def loadImage(file: Path): F[BufferedImage]
  def loadText(file: Path): F[String]

object FileLoader:
  def apply[F[_]: Async]: FileLoader[F] =
    new FileLoader[F]:
      def loadPDF(file: Path) =
        val make = Sync[F].blocking(PDDocument.load(file.toNioPath.toFile))
        def release(doc: PDDocument) = Sync[F].blocking(doc.close())
        Resource.make(make)(release)

      def loadImage(file: Path) =
        Sync[F].blocking(ImageIO.read(file.toNioPath.toFile))

      def loadText(file: Path) =
        Files[F].readAll(file).through(fs2.text.utf8.decode).compile.string
