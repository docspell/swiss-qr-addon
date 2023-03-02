package docspell.swissqr

import docspell.swissqr.SwissQR.{Amount, Header, Reference, ReferenceType}
import munit.*
import org.apache.pdfbox.pdmodel.PDDocument

import javax.imageio.ImageIO

class QrReaderTest extends FunSuite:

  test("quick test") {
    val image = getClass.getResourceAsStream("/files/qr-test-image.png")
    val pdf = PDDocument.load(getClass.getResourceAsStream("/files/qr-test-image.pdf"))

    val txtText = QrTextReader.read(testQR).fold(fail(_), identity)
    val imgText = QrReader.readImage(ImageIO.read(image)).map(_.fold(fail(_), identity))
    val pdfText = QrReader.readPdf(pdf).map(_.fold(fail(_), identity))

    assertEquals(pdfText, Vector(expectedQR))
    assertEquals(imgText, List(expectedQR))
    assertEquals(txtText, expectedQR)
  }

  val expectedQR = SwissQR(
    header = Header(qrType = "SPC", version = SwissQR.Version.V20, codingType = 1),
    account = "CH4431999123000889012",
    creditor = SwissQR.Address(
      addressType = SwissQR.AddressType.Structured,
      name = "Robert Schneider AG",
      line1 = "Rue du Lac",
      line2 = "1268/2/22",
      postalCode = "2501",
      city = "Biel",
      country = "CH"
    ),
    ultimateCreditor = Some(
      SwissQR.Address(
        addressType = SwissQR.AddressType.Structured,
        name = "Robert Schneider Services Switzerland AG",
        line1 = "Rue du Lac",
        line2 = "1268/3/1",
        postalCode = "2501",
        city = "Biel",
        country = "CH"
      )
    ),
    amount = Amount(Some(BigDecimal("32.00")), "CHF"),
    debitor = Some(
      SwissQR.Address(
        addressType = SwissQR.AddressType.Structured,
        name = "Pia-Maria Rutschmann-Schnyder",
        line1 = "Grosse Marktgasse",
        line2 = "28",
        postalCode = "9400",
        city = "Rorschach",
        country = "CH"
      )
    ),
    reference = Reference(ReferenceType.QRR, "210000000003139471430009017"),
    additional = List("QWERTY")
  )

  val testQR =
    """SPC
      |0200
      |1
      |CH4431999123000889012
      |S
      |Robert Schneider AG
      |Rue du Lac
      |1268/2/22
      |2501
      |Biel
      |CH
      |S
      |Robert Schneider Services Switzerland AG
      |Rue du Lac
      |1268/3/1
      |2501
      |Biel
      |CH
      |32.00
      |CHF
      |S
      |Pia-Maria Rutschmann-Schnyder
      |Grosse Marktgasse
      |28
      |9400
      |Rorschach
      |CH
      |QRR
      |210000000003139471430009017
      |QWERTY
    """.stripMargin

val testQR2 =
  """SPC
    |0200
    |1
    |CH4431999123000889012
    |S
    |Robert Schneider AG
    |Rue du Lac
    |
    |2501
    |Biel
    |CH
    |
    |
    |
    |
    |
    |
    |
    |40.00
    |CHF
    |S
    |Pia-Maria Rutschmann-Schnyder
    |Grosse Marktgasse
    |28
    |9400
    |Rorschach
    |CH
    |QRR
    |210000000003139471430009017
    |QWERTY
    |EPD
    """.stripMargin
