package docspell.swissqr

import boofcv.alg.fiducial.qrcode.EciEncoding
import boofcv.factory.fiducial.{ConfigQrCode, FactoryFiducial}
import boofcv.factory.filter.binary.{ConfigThreshold, ThresholdType}
import boofcv.factory.shape.{ConfigPolygonDetector, ConfigPolygonFromContour}
import boofcv.io.image.ConvertBufferedImage
import boofcv.struct.image.GrayU8
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.apache.pdfbox.pdmodel.{PDDocument, PDPage}
import org.apache.pdfbox.rendering.{ImageType, PDFRenderer}

import java.awt.{BasicStroke, Color, Graphics2D, Image, RenderingHints}
import java.awt.image.{BufferedImage, ImageObserver}
import java.io.File
import java.util.logging.{Level, Logger}
import javax.imageio.ImageIO
import scala.jdk.CollectionConverters.*
import scala.collection.immutable.Seq

object QrReader:
  private val logger: Logger = Logger.getLogger("QrReader")

  private val maxImageSize = 600 * 600

  def readImage(img: BufferedImage): List[Either[String, SwissQR]] =
    val qrCodes = readImage1(img)
    if (qrCodes.isEmpty && img.getHeight * img.getWidth < maxImageSize) {
      readImage1(createBorderImage(img))
    } else qrCodes

  private def readImage1(img: BufferedImage): List[Either[String, SwissQR]] =
    val cfg = ConfigQrCode()
    val detector = FactoryFiducial.qrcode(cfg, classOf[GrayU8])
    detector.process(ConvertBufferedImage.convertFrom(img, null: GrayU8))
    detector.getDetections.asScala.map(qr => QrTextReader.read(qr.message)).toList

  def readPdf(doc: PDDocument, dpi: Float = 96.0f): Seq[Either[String, SwissQR]] =
    val renderer = createPdfRenderer(doc)
    (0 until doc.getNumberOfPages).flatMap { index =>
      val page = doc.getPage(index)
      renderer.setSubsamplingAllowed(enableSubsampling(page))
      val img = renderer.renderImageWithDPI(index, dpi, ImageType.RGB)
      readImage(img)
    }

  /** Draws a white border around an image to help with qr code detection. */
  private def createBorderImage(img: BufferedImage): BufferedImage =
    val borderWidth = 1
    val out = new BufferedImage(
      img.getWidth + (borderWidth * 2),
      img.getHeight + (borderWidth * 2),
      BufferedImage.TYPE_USHORT_GRAY
    )
    val g = out.getGraphics.asInstanceOf[Graphics2D]
    g.setBackground(Color.WHITE)
    g.setPaint(Color.WHITE)
    g.setStroke(new BasicStroke(borderWidth * 2.0f))
    g.drawRect(0, 0, out.getWidth, out.getHeight)
    g.drawImage(
      img,
      borderWidth,
      borderWidth,
      Color.WHITE,
      null
    )
    out

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
      RenderingHints.KEY_ALPHA_INTERPOLATION,
      RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY
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
          logger.log(Level.FINE, s"Found image in pdf of size ${w}x$h (${w * h}px)")
          w * h
        }
        .maxOption
    largestImage.exists(_ > 10 * 1024 * 1024)
  }
