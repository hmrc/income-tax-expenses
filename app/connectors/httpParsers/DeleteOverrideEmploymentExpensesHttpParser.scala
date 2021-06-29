
package connectors.httpParsers

import models.DesErrorModel
import play.api.http.Status._
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import utils.PagerDutyHelper.PagerDutyKeys._
import utils.PagerDutyHelper.pagerDutyLog

object DeleteOverrideEmploymentExpensesHttpParser extends DESParser {
  type DeleteOverrideEmploymentExpensesResponse = Either[DesErrorModel, Unit]

  override val parserName: String = "DeleteOverrideEmploymentExpensesHttpParser"

  implicit object DeleteOverrideEmploymentExpensesHttpReads extends HttpReads[DeleteOverrideEmploymentExpensesResponse] {

    override def read(method: String, url: String, response: HttpResponse): DeleteOverrideEmploymentExpensesResponse = {
      response.status match {

        case NO_CONTENT => Right(())
        case INTERNAL_SERVER_ERROR =>
          pagerDutyLog(INTERNAL_SERVER_ERROR_FROM_DES, logMessage(response))
          handleDESError(response)
        case SERVICE_UNAVAILABLE =>
          pagerDutyLog(SERVICE_UNAVAILABLE_FROM_DES, logMessage(response))
          handleDESError(response)
        case BAD_REQUEST | NOT_FOUND =>
          pagerDutyLog(FOURXX_RESPONSE_FROM_DES, logMessage(response))
          handleDESError(response)
        case _ =>
          pagerDutyLog(UNEXPECTED_RESPONSE_FROM_DES, logMessage(response))
          handleDESError(response, Some(INTERNAL_SERVER_ERROR))
      }
    }
  }


}
