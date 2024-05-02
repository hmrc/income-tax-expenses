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

package services

import connectors.{CreateOrAmendEmploymentExpensesConnector, DeleteOverrideEmploymentExpensesConnector}
import connectors.httpParsers.CreateOrAmendEmploymentExpensesHttpParser.CreateOrAmendEmploymentExpenseResponse
import connectors.httpParsers.DeleteOverrideEmploymentExpensesHttpParser.DeleteOverrideEmploymentExpensesResponse
import models._
import play.api.http.Status.NOT_FOUND
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class CreateOrAmendEmploymentExpensesService @Inject()(
                                                        connector: CreateOrAmendEmploymentExpensesConnector,
                                                        deleteConnector: DeleteOverrideEmploymentExpensesConnector
                                                      ) {

  def createOrAmendEmploymentExpenses(nino: String, taxYear: Int, expenses: CreateExpensesRequestModel)
                                     (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[CreateOrAmendEmploymentExpenseResponse] = {

    val ignoreExpensesOpt: Option[IgnoreExpenses] = expenses.ignoreExpenses.map(IgnoreExpenses(_))

    def performCreateOrDelete(nino: String, taxYear: Int, expenses: CreateExpensesRequestModel): Future[CreateOrAmendEmploymentExpenseResponse] = {
      if (expenses.expenses.isEmpty) {
        performDeleteWhenEmptyExpenses(nino, taxYear)
      } else {
        connector.createOrAmendEmploymentExpenses(nino, taxYear, Expenses(expenses.expenses))
      }
    }

    def performDeleteWhenEmptyExpenses(nino: String, taxYear: Int): Future[DeleteOverrideEmploymentExpensesResponse] = {
      deleteConnector.deleteOverrideEmploymentExpenses(nino, taxYear).flatMap {
        case Left(error) if (error.status == NOT_FOUND) => Future(Right(()))
        case Left(error) => Future(Left(error))
        case Right(response) => Future(Right(response))
      }
    }

    ignoreExpensesOpt match {
      case Some(ignoreExpensesRequestModel) if ignoreExpensesRequestModel.ignoreExpenses =>
        connector.createOrAmendEmploymentExpenses(nino, taxYear, ignoreExpensesRequestModel)
          .flatMap {
            case Right(_) => performCreateOrDelete(nino, taxYear, expenses)
            case Left(error) => Future(Left(error))
          }
      case _ => performCreateOrDelete(nino, taxYear, expenses)
    }
  }

}
