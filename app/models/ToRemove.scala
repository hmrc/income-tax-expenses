/*
 * Copyright 2022 HM Revenue & Customs
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

package models

sealed abstract class ToRemove(val value: String)

object ToRemove {
  private val hmrcHeld = "HMRC-HELD"
  private val customer = "CUSTOMER"
  private val all = "ALL"

  case object HmrcHeld extends ToRemove(hmrcHeld)
  case object Customer extends ToRemove(customer)
  case object All extends ToRemove(all)

  def apply(string: String): Option[ToRemove] = {
    string.toUpperCase match {
      case `hmrcHeld` => Some(HmrcHeld)
      case `customer` => Some(Customer)
      case `all` => Some(All)
      case _ => None
    }
  }
}
