package docspell.swissqr

import cats.*
import cats.syntax.all.*
import docspell.swissqr.SwissQR.*

import scala.annotation.tailrec
import scala.util.Try

object QrTextReader extends LineReader:

  def read(str: String): Either[String, SwissQR] =
    swissQr(str).map(_._1)

  def swissQr: P[SwissQR] =
    (
      header,
      account,
      address("CR"),
      addressOpt("UCR"),
      amount,
      addressOpt("DEB"),
      reference,
      additional
    )
      .mapN(SwissQR.apply)

  val referenceType: P[ReferenceType] =
    line.emap(ReferenceType.fromString)

  val reference: P[Reference] =
    (referenceType, line).mapN(Reference.apply)

  val additional: P[List[String]] =
    restAsLines
      .map(_.filter(_.nonEmpty))
      .map(lines => if (lines.isEmpty) lines else lines.init)

  val version: P[Version] =
    line.emap(Version.fromString)

  val header: P[Header] =
    (line, version, int).mapN(Header.apply).withError(cause => s"Header failed: $cause")

  val account: P[String] = line.withError(cause => s"Account failed: $cause")

  val amount: P[Amount] =
    (line.fold(line, bigDecimal).map(_.toOption), line)
      .mapN(Amount.apply)
      .withError(cause => s"Amount failed: $cause")

  val addressType: P[AddressType] =
    line
      .emap(AddressType.fromString)
      .withError(cause => s"Address-Type failed: $cause")

  def address(hint: String): P[Address] =
    (addressType, line, line, line, line, line, line)
      .mapN(Address.apply)
      .withError(cause => s"Address $hint failed: $cause")

  def addressOpt(hint: String): P[Option[Address]] =
    line.fold(skip(7), address(hint)).map(_.toOption)
