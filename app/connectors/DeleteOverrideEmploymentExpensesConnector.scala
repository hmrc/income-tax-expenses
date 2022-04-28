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

package connectors

import config.AppConfig
import connectors.httpParsers.DeleteOverrideEmploymentExpensesHttpParser.{DeleteOverrideEmploymentExpensesHttpReads, DeleteOverrideEmploymentExpensesResponse}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import utils.DESTaxYearHelper.desTaxYearConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeleteOverrideEmploymentExpensesConnector @Inject()(val http: HttpClient,
                                                          val appConfig: AppConfig)(implicit val ec: ExecutionContext) extends DesConnector {

  def deleteOverrideEmploymentExpenses(nino: String, taxYear: Int)(implicit hc: HeaderCarrier): Future[DeleteOverrideEmploymentExpensesResponse] = {

    val incomeSourceUri: String = baseUrl + s"/income-tax/expenses/employments/$nino/${desTaxYearConverter(taxYear)}"

    def desCall(implicit hc: HeaderCarrier): Future[DeleteOverrideEmploymentExpensesResponse] = {
      http.DELETE[DeleteOverrideEmploymentExpensesResponse](incomeSourceUri)
    }

    desCall(desHeaderCarrier(incomeSourceUri))

  }

}
