/*
 * Copyright 2021 HM Revenue & Customs
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

package services

import connectors.CreateOrAmendEmploymentExpensesConnector
import connectors.httpParsers.CreateOrAmendEmploymentExpensesHttpParser.CreateOrAmendEmploymentExpenseResponse
import models._
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CreateOrAmendEmploymentExpensesService @Inject()(connector: CreateOrAmendEmploymentExpensesConnector) {

  def createOrAmendEmploymentExpenses(nino: String, taxYear: Int, expenses: CreateExpensesRequestModel)
                           (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[CreateOrAmendEmploymentExpenseResponse] = {

    val ignoreExpensesOpt = expenses.ignoreExpenses.map(IgnoreExpenses(_))

    ignoreExpensesOpt match {
      case Some(ignoreExpensesRequestModel) if ignoreExpensesRequestModel.ignoreExpenses =>
        connector.createOrAmendEmploymentExpenses(nino, taxYear, ignoreExpensesRequestModel)
          .flatMap {
            case Right(_) => connector.createOrAmendEmploymentExpenses(nino, taxYear, Expenses(expenses.expenses))
            case Left(error) => Future(Left(error))
          }
      case _ => connector.createOrAmendEmploymentExpenses(nino, taxYear, Expenses(expenses.expenses))
    }
  }

}
