/*
 * Copyright 2024 HM Revenue & Customs
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

import play.api.libs.json._

sealed trait EmploymentExpensesRequestModel

final case class Expenses(expenses: ExpensesType) extends EmploymentExpensesRequestModel
object Expenses { implicit val formats = Json.format[Expenses] }

final case class IgnoreExpenses(ignoreExpenses: Boolean) extends EmploymentExpensesRequestModel
object IgnoreExpenses { implicit val formats = Json.format[IgnoreExpenses] }

object EmploymentExpensesRequestModel {
  implicit val writes = new Writes[EmploymentExpensesRequestModel] {
    override def writes(model: EmploymentExpensesRequestModel) = model match {
      case e: Expenses => Json.toJson(e)
      case ie: IgnoreExpenses => Json.toJson(ie)
    }
  }

  implicit val reads = new Reads[EmploymentExpensesRequestModel] {
    override def reads(json: JsValue): JsResult[EmploymentExpensesRequestModel] = {
      IgnoreExpenses.formats.reads(json) orElse Expenses.formats.reads(json)
    }
  }
}
