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
import connectors.httpParsers.GetEmploymentExpensesHttpParser.{GetEmploymentExpensesHttpReads, GetEmploymentExpensesResponse}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import utils.DESTaxYearHelper.desTaxYearConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GetEmploymentExpensesConnector @Inject()(val http: HttpClient,
                                               val appConfig: AppConfig)(implicit ec:ExecutionContext) extends IFConnector {

  def getEmploymentExpenses(nino: String, taxYear: Int, view: String)(implicit hc: HeaderCarrier): Future[GetEmploymentExpensesResponse] = {

    val incomeSourcesUri: String =
      appConfig.integrationFrameworkBaseUrl + s"/income-tax/expenses/employments/$nino/${desTaxYearConverter(taxYear)}" + s"?view=$view"

    def integrationFrameworkCall(implicit hc: HeaderCarrier): Future[GetEmploymentExpensesResponse] = {
      http.GET[GetEmploymentExpensesResponse](incomeSourcesUri)
    }

    integrationFrameworkCall(integrationFrameworkHeaderCarrier(incomeSourcesUri, apiNumber = "1668"))
  }
}
