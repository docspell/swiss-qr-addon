package docspell.swissqr

import boofcv.factory.fiducial.FactoryFiducial
import boofcv.io.image.ConvertBufferedImage
import boofcv.struct.image.GrayU8
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.apache.pdfbox.pdmodel.{PDDocument, PDPage}
import org.apache.pdfbox.rendering.{ImageType, PDFRenderer}

import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.util.logging.{Level, Logger}
import scala.jdk.CollectionConverters.*

object QrReader:
  private[this] val logger: Logger = Logger.getLogger("QrReader")

  def readImage(img: BufferedImage): List[Either[String, SwissQR]] =
    val detector = FactoryFiducial.qrcode(null, classOf[GrayU8])
    detector.process(ConvertBufferedImage.convertFrom(img, null: GrayU8))
    detector.getDetections.asScala.map(qr => QrTextReader.read(qr.message)).toList

  def readPdf(doc: PDDocument, dpi: Float = 96.0f): Seq[Either[String, SwissQR]] =
    val renderer = createPdfRenderer(doc)
    (0 until doc.getNumberOfPages).flatMap { index =>
      val page = doc.getPage(index)
      renderer.setSubsamplingAllowed(enableSubsampling(doc.getPage(index)))
      val img = renderer.renderImageWithDPI(index, dpi, ImageType.RGB)
      readImage(img)
    }

  private def createPdfRenderer(doc: PDDocument) =
    val renderer = new PDFRenderer(doc)
    renderer.setImageDownscalingOptimizationThreshold(0.85f)
    val hints = new RenderingHints(
      RenderingHints.KEY_RENDERING,
      RenderingHints.VALUE_RENDER_QUALITY
    )
    hints.put(
      RenderingHints.KEY_COLOR_RENDERING,
      RenderingHints.VALUE_COLOR_RENDER_QUALITY
    )
    hints.put(
      RenderingHints.KEY_INTERPOLATION,
      RenderingHints.VALUE_INTERPOLATION_BICUBIC
    )
    hints.put(
      RenderingHints.KEY_TEXT_ANTIALIASING,
      RenderingHints.VALUE_TEXT_ANTIALIAS_ON
    )
    hints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    renderer.setRenderingHints(hints)
    renderer

  private def enableSubsampling(page: PDPage): Boolean = {
    val res = page.getResources
    val largestImage =
      res.getXObjectNames.asScala
        .map(name => res.getXObject(name))
        .collect { case xobj: PDImageXObject => xobj }
        .map { imgobj =>
          val w = imgobj.getWidth
          val h = imgobj.getHeight
          logger.log(Level.FINE, s"Found image in pdf of size ${w}x${h} (${w * h}px)")
          w * h
        }
        .maxOption
    largestImage.exists(_ > 10 * 1024 * 1024)
  }
