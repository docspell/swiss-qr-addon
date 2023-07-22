package docspell.swissqr

import SwissQR.*
import cats.Show
import cats.syntax.show.*
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

case class SwissQR(
    header: Header,
    account: String,
    creditor: Address,
    ultimateCreditor: Option[Address],
    amount: Amount,
    debitor: Option[Address],
    reference: Reference,
    additional: List[String]
)

object SwissQR:
  enum Version(val value: String):
    case V20 extends Version("0200")

  object Version:
    def fromString(str: String): Either[String, Version] =
      Version.values
        .find(v => v.value.equalsIgnoreCase(str))
        .toRight(s"Invalid version: $str")

    given jsonDecoder: Decoder[Version] =
      Decoder.decodeString.emap(fromString)
    given jsonEncoder: Encoder[Version] =
      Encoder.encodeString.contramap(_.value)

  enum AddressType(val code: String):
    case Structured extends AddressType("S")
    case Combined extends AddressType("K")

  object AddressType:
    def fromString(str: String): Either[String, AddressType] =
      AddressType.values
        .find(v => v.code.equalsIgnoreCase(str))
        .toRight(s"Invalid address type: $str")

    given jsonDecoder: Decoder[AddressType] =
      Decoder.decodeString.emap(fromString)
    given jsonEncoder: Encoder[AddressType] =
      Encoder.encodeString.contramap(_.code)

  case class Address(
      addressType: AddressType,
      name: String,
      line1: String,
      line2: String,
      postalCode: String,
      city: String,
      country: String
  )

  object Address:
    given jsonDecoder: Decoder[Address] = deriveDecoder
    given jsonEncoder: Encoder[Address] = deriveEncoder
    given show: Show[Address] =
      Show.show { a =>
        val cc = Option(a.country)
          .map(_.trim)
          .filter(_.nonEmpty)
          .map(c => s"$c-")
          .getOrElse("")
        List(a.name, a.line1, a.line2, s"$cc${a.postalCode} ${a.city}")
          .map(_.trim)
          .filter(_.nonEmpty)
          .mkString("\n")
      }

  case class Amount(amount: Option[BigDecimal], currency: String):
    def formatAmount: Option[String] =
      amount.map(_.setScale(2, BigDecimal.RoundingMode.HALF_UP)).map(_.toString())

  object Amount:
    def chf(amount: Double): Amount = Amount(Some(BigDecimal.valueOf(amount)), "CHF")
    given jsonDecoder: Decoder[Amount] = deriveDecoder
    given jsonEncoder: Encoder[Amount] = deriveEncoder
    given show: Show[Amount] =
      Show.show(a => s"${a.currency}${a.formatAmount.map(s => s" $s").getOrElse("-")}")

  enum ReferenceType(val code: String):
    case QRR extends ReferenceType("QRR")
    case SCOR extends ReferenceType("SCOR")
    case NON extends ReferenceType("NON")

  object ReferenceType:
    def fromString(str: String): Either[String, ReferenceType] =
      ReferenceType.values
        .find(v => v.code.equalsIgnoreCase(str))
        .toRight(s"Invalid reference type: $str")

    given jsonDecoder: Decoder[ReferenceType] =
      Decoder.decodeString.emap(fromString)
    given jsonEncoder: Encoder[ReferenceType] =
      Encoder.encodeString.contramap(_.code)

  case class Reference(referenceType: ReferenceType, ref: String)

  object Reference:
    given jsonDecoder: Decoder[Reference] = deriveDecoder
    given jsonEncoder: Encoder[Reference] = deriveEncoder

  case class Header(qrType: String, version: Version, codingType: Int)

  object Header:
    given jsonDecoder: Decoder[Header] = deriveDecoder
    given jsonEncoder: Encoder[Header] = deriveEncoder

  given jsonDecoder: Decoder[SwissQR] = deriveDecoder
  given jsonEncoder: Encoder[SwissQR] = deriveEncoder

  given show: Show[SwissQR] =
    Show.show { qr =>
      val creditor =
        qr.creditor.show.split("\r?\n").map(line => s"  $line").mkString("\n")

      val debitor =
        qr.debitor.map { d =>
          val address = d.show.split("\r?\n").map(line => s"  $line").mkString("\n")
          s"""- Debitor:
             |  ```
             |$address
             |  ```""".stripMargin
        }

      val adds = qr.additional.map(line => s"  - $line").mkString("\n")

      val parts = List(
        s"- Account: `${qr.account}``",
        s"- Amount: ${qr.amount.show}",
        qr.reference match
          case Reference(ReferenceType.NON, _) => ""
          case Reference(_, id)                => s"- Reference: `$id`"
        ,
        s"""- Creditor:
           |  ```
           |$creditor
           |  ```""".stripMargin,
        debitor.getOrElse(""),
        qr.additional match
          case Nil => ""
          case _ =>
            s"""- Additional:
               |$adds""".stripMargin
      )

      parts.filter(_.nonEmpty).mkString("\n")
    }
