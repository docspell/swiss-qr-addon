package docspell.swissqr

import munit.*

import javax.imageio.ImageIO

class QrTextReaderTest extends FunSuite:

  test("quick test") {

    println(QrTextReader.read(testQR2))
  }

  test("quick image test") {
    val image = getClass.getResourceAsStream("/files/qr-test-image.png")
    val result = QrReader.readImage(ImageIO.read(image))
    println(result)
  }

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
      |EPD
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
