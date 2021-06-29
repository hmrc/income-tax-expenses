
package connectors

import config.AppConfig
import connectors.httpParsers.DeleteOverrideEmploymentExpensesHttpParser.{DeleteOverrideEmploymentExpensesHttpReads, DeleteOverrideEmploymentExpensesResponse}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeleteOverrideEmploymentExpensesConnector @Inject()(val http: HttpClient,
                                                          val appConfig: AppConfig)(implicit val ec: ExecutionContext) extends DesConnector {

  def deleteOverrideEmploymentExpenses(nino: String, taxYear: Int)(implicit hc: HeaderCarrier): Future[DeleteOverrideEmploymentExpensesResponse] = {

    val incomeSourceUri: String = appConfig.desBaseUrl + s"/income-tax/expenses/employments/$nino/$taxYear"

    def desCall(implicit hc: HeaderCarrier): Future[DeleteOverrideEmploymentExpensesResponse] = {
      http.DELETE[DeleteOverrideEmploymentExpensesResponse](incomeSourceUri)
    }

    desCall(desHeaderCarrier(incomeSourceUri))

  }

}
