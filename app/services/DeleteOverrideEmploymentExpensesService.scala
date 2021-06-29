
package services

import connectors.DeleteOverrideEmploymentExpensesConnector
import connectors.httpParsers.DeleteOverrideEmploymentExpensesHttpParser.DeleteOverrideEmploymentExpensesResponse
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.Future

class DeleteOverrideEmploymentExpensesService @Inject() (connector: DeleteOverrideEmploymentExpensesConnector) {

  def deleteOverrideEmploymentExpenses(nino: String, taxYear: Int)(implicit hc: HeaderCarrier): Future[DeleteOverrideEmploymentExpensesResponse] = {
    connector.deleteOverrideEmploymentExpenses(nino, taxYear)
  }

}
