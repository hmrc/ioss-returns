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

import play.api.libs.json._
import play.api.mvc.{PathBindable, QueryStringBindable}

import java.time.Month._
import java.time.format.TextStyle
import java.time.{LocalDate, Month, YearMonth}
import java.util.Locale
import scala.util.Try
import scala.util.matching.Regex

case class Period(year: Int, month: Month) {
  val firstDay: LocalDate = LocalDate.of(year, month, 1)

  val lastDay: LocalDate = firstDay.plusMonths(1).minusDays(1)

  val paymentDeadline: LocalDate =
    firstDay.plusMonths(2).minusDays(1)

  def displayText: String =
    s"${month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${year}"

  override def toString: String = s"$year-M${month.getValue}"

  def toEtmpPeriodString: String = {
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

  def getNext(): Period = {
    if (this.month == Month.DECEMBER)
      Period(this.year + 1, Month.JANUARY)
    else
      Period(this.year, this.month.plus(1))
  }

  def getPrevious(): Period = {
    if (this.month == Month.JANUARY)
      Period(this.year - 1, Month.DECEMBER)
    else
      Period(this.year, this.month.minus(1))
  }

  def isBefore(other: Period): Boolean = {
    val yearMonth: YearMonth = YearMonth.of(year, month)
    val yearMonthOther: YearMonth = YearMonth.of(other.year, other.month)

    yearMonth.isBefore(yearMonthOther)
  }
}

object Period {
  private val pattern: Regex = """(\d{4})-M(1[0-2]|[1-9])""".r.anchored

  def apply(yearString: String, monthString: String): Try[Period] =
    for {
      year <- Try(yearString.toInt)
      month <- Try(Month.of(monthString.toInt))
    } yield Period(year, month)

  def fromString(string: String): Option[Period] =
    string match {
      case pattern(yearString, monthString) =>
        Period(yearString, monthString).toOption
      case _ =>
        None
    }

  def getRunningPeriod(date: LocalDate): Period =
    Period(date.getYear, date.getMonth)

  def fromKey(key: String): Period = {
    val yearLast2 = key.take(2)
    val month = key.drop(2)
    Period(s"20$yearLast2".toInt, fromEtmpMonthString(month))
  }

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

  implicit val monthReads: Reads[Month] = {
    Reads.at[Int](__ \ "month")
      .map(Month.of)
  }

  implicit val monthWrites: Writes[Month] = {
    Writes.at[Int](__ \ "month")
      .contramap(_.getValue)
  }

  implicit val format: OFormat[Period] = Json.format[Period]

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