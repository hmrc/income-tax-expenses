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

import connectors.httpParsers.DeleteOverrideEmploymentExpensesHttpParser.DeleteOverrideEmploymentExpensesResponse
import connectors.{CreateOrAmendEmploymentExpensesConnector, DeleteOverrideEmploymentExpensesConnector, DeleteOverrideEmploymentExpensesIFConnector}
import models.ToRemove.{All, Customer, HmrcHeld}
import models.{IgnoreExpenses, ToRemove}
import uk.gov.hmrc.http.HeaderCarrier
import utils.TaxYearUtils.isYearAfter2324

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeleteOrIgnoreEmploymentExpensesService @Inject()(deleteDESConnector: DeleteOverrideEmploymentExpensesConnector,
                                                        deleteIFConnector: DeleteOverrideEmploymentExpensesIFConnector,
                                                        createOrAmendConnector: CreateOrAmendEmploymentExpensesConnector) {

  def deleteOrIgnoreEmploymentExpenses(nino: String, toRemove: ToRemove, taxYear: Int)
                                      (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[DeleteOverrideEmploymentExpensesResponse] = {

    toRemove match {
      case HmrcHeld => createOrAmendConnector.createOrAmendEmploymentExpenses(nino, taxYear, IgnoreExpenses(true))
      case Customer => deleteOverrideEmploymentExpenses(nino, taxYear)
      case All =>
        deleteOverrideEmploymentExpenses(nino, taxYear)
          .flatMap {
            case Right(_) => createOrAmendConnector.createOrAmendEmploymentExpenses(nino, taxYear, IgnoreExpenses(true))
            case Left(error) => Future(Left(error))
          }
    }
  }

  private def deleteOverrideEmploymentExpenses(nino: String, taxYear: Int)(implicit hc: HeaderCarrier): Future[DeleteOverrideEmploymentExpensesResponse] = {
    if (isYearAfter2324(taxYear)) {
      deleteIFConnector.deleteOverrideEmploymentExpenses(nino, taxYear)
    } else {
      deleteDESConnector.deleteOverrideEmploymentExpenses(nino, taxYear)
    }
  }

}
