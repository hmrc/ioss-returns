/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.iossreturns.models

import play.api.libs.functional.syntax.*
import play.api.libs.json.*
import play.api.mvc.{PathBindable, QueryStringBindable}

import java.time.Month.*
import java.time.{LocalDate, Month, YearMonth}
import scala.util.Try
import scala.util.matching.Regex

trait Period {
  val year: Int
  val month: Month
  val firstDay: LocalDate
  val lastDay: LocalDate
  val isPartial: Boolean

  val paymentDeadline: LocalDate =
    LocalDate.of(year, month, 1).plusMonths(2).minusDays(1)

  def isBefore(other: Period): Boolean = {
    val yearMonth: YearMonth = YearMonth.of(year, month)
    val yearMonthOther: YearMonth = YearMonth.of(other.year, other.month)

    yearMonth.isBefore(yearMonthOther)
  }

}

case class StandardPeriod(year: Int, month: Month) extends Period {

  override val firstDay: LocalDate = LocalDate.of(year, month, 1)
  override val lastDay: LocalDate = firstDay.plusMonths(1).minusDays(1)
  override val isPartial: Boolean = false

  override def toString: String = s"$year-M${month.getValue}"

}

object StandardPeriod {
  val reads: Reads[StandardPeriod] = {
    (
      (__ \ "year").read[Int] and
        (__ \ "month").read[String].map(m => Month.of(m.substring(1).toInt))
      )((year, month) => StandardPeriod(year, month))
  }

  val writes: OWrites[StandardPeriod] = {
    (
      (__ \ "year").write[Int] and
        (__ \ "month").write[String].contramap[Month](m => s"M${m.getValue}")
      )(standardPeriod => Tuple.fromProductTyped(standardPeriod))
  }

  implicit val format: Format[StandardPeriod] = Format(reads, writes)
}

object Period {
  private val pattern: Regex = """(\d{4})-M(1[0-2]|[1-9])""".r.anchored

  def apply(yearMonth: YearMonth): Period = StandardPeriod(yearMonth.getYear, yearMonth.getMonth)

  def apply(yearString: String, monthString: String): Try[Period] =
    for {
      year <- Try(yearString.toInt)
      month <- Try(Month.of(monthString.toInt))
    } yield StandardPeriod(year, month)

  def fromString(string: String): Option[Period] = {
    string match {
      case pattern(yearString, monthString) =>
        Period(yearString, monthString).toOption
      case _ =>
        None
    }
  }

  def convertFromCorePeriodString(string: String): Option[Period] = {

    val pattern: Regex = """(\d{4})-M(0[1-9]|1[0-2]|[1-9])""".r.anchored

    string match {
      case pattern(yearString, monthString) =>
        Period(yearString, monthString).toOption

      case _ =>
        None
    }
  }

  def toEtmpPeriodString(currentPeriod: Period): String = {
    val standardPeriod = StandardPeriod(currentPeriod.year, currentPeriod.month)
    val year = standardPeriod.year
    val month = standardPeriod.month
    val lastYearDigits = year.toString.substring(2)

    s"$lastYearDigits${toEtmpMonthString(month)}"
  }

  // TODO create util & tests - is there a better way of doing this?
  private def toEtmpMonthString(month: Month): String = {
    month match {
      case JANUARY => "AA"
      case FEBRUARY => "AB"
      case MARCH => "AC"
      case APRIL => "AD"
      case MAY => "AE"
      case JUNE => "AF"
      case JULY => "AG"
      case AUGUST => "AH"
      case SEPTEMBER => "AI"
      case OCTOBER => "AJ"
      case NOVEMBER => "AK"
      case DECEMBER => "AL"
    }
  }

  def fromKey(key: String): Period = {
    val yearLast2 = key.take(2)
    val month = key.drop(2)
    StandardPeriod(s"20$yearLast2".toInt, fromEtmpMonthString(month))
  }

  def getNext(currentPeriod: Period): Period = {
    if (currentPeriod.month == Month.DECEMBER)
      StandardPeriod(currentPeriod.year + 1, Month.JANUARY)
    else
      StandardPeriod(currentPeriod.year, currentPeriod.month.plus(1))
  }

  def getPrevious(currentPeriod: Period): Period = {
    if (currentPeriod.month == Month.JANUARY)
      StandardPeriod(currentPeriod.year - 1, Month.DECEMBER)
    else
      StandardPeriod(currentPeriod.year, currentPeriod.month.minus(1))
  }

  def getRunningPeriod(date: LocalDate): Period =
    StandardPeriod(date.getYear, date.getMonth)


  private def fromEtmpMonthString(keyMonth: String): Month = {
    keyMonth match {
      case "AA" => Month.JANUARY
      case "AB" => Month.FEBRUARY
      case "AC" => Month.MARCH
      case "AD" => Month.APRIL
      case "AE" => Month.MAY
      case "AF" => Month.JUNE
      case "AG" => Month.JULY
      case "AH" => Month.AUGUST
      case "AI" => Month.SEPTEMBER
      case "AJ" => Month.OCTOBER
      case "AK" => Month.NOVEMBER
      case "AL" => Month.DECEMBER
    }
  }

  val reads: Reads[Period] =
    StandardPeriod.format.widen[Period] orElse
      PartialReturnPeriod.format.widen[Period]


  val writes: Writes[Period] = {
    case s: StandardPeriod => Json.toJson(s)(StandardPeriod.format)
    case p: PartialReturnPeriod => Json.toJson(p)(PartialReturnPeriod.format)
  }

  implicit val format: Format[Period] = Format(reads, writes)

  implicit val pathBindable: PathBindable[Period] = new PathBindable[Period] {

    override def bind(key: String, value: String): Either[String, Period] =
      fromString(value) match {
        case Some(period) => Right(period)
        case None => Left("Invalid period")
      }

    override def unbind(key: String, value: Period): String =
      value.toString
  }

  implicit def orderingByPeriod[A <: Period]: Ordering[A] =
    Ordering.by(e => YearMonth.of(e.year, e.month))

  implicit val queryBindable: QueryStringBindable[Period] = new QueryStringBindable[Period] {
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, Period]] = {
      params.get(key).flatMap(_.headOption).map {
        periodString =>
          fromString(periodString) match {
            case Some(period) => Right(period)
            case _ => Left("Invalid period")
          }
      }
    }

    override def unbind(key: String, value: Period): String = {
      s"$key=$value"
    }
  }

}