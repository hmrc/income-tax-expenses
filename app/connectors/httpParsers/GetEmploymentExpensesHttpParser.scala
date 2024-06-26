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

package connectors.httpParsers

import models.{DesErrorBodyModel, DesErrorModel, GetEmploymentExpensesModel}
import play.api.Logging
import play.api.http.Status._
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import utils.PagerDutyHelper.PagerDutyKeys._
import utils.PagerDutyHelper.pagerDutyLog

object GetEmploymentExpensesHttpParser extends DESParser with Logging {
  type GetEmploymentExpensesResponse = Either[DesErrorModel, GetEmploymentExpensesModel]

  override val parserName: String = "GetEmploymentExpensesHttpParser"

  implicit object GetEmploymentExpensesHttpReads extends HttpReads[GetEmploymentExpensesResponse] {

    override def read(method: String, url: String, response: HttpResponse): GetEmploymentExpensesResponse = {
      response.status match {
        case OK => response.json.validate[GetEmploymentExpensesModel].fold[GetEmploymentExpensesResponse](
          _ => {
            pagerDutyLog(BAD_SUCCESS_JSON_FROM_DES,s"[GetEmploymentExpensesHttpParser][read] Invalid Json from DES.")
            Left(DesErrorModel(INTERNAL_SERVER_ERROR, DesErrorBodyModel.parsingError))
          },
          parsedModel => Right(parsedModel)
        )
        case NOT_FOUND =>
          logger.info(logMessage(response))
          handleDESError(response)
        case INTERNAL_SERVER_ERROR =>
          pagerDutyLog(INTERNAL_SERVER_ERROR_FROM_DES, logMessage(response))
          handleDESError(response)
        case SERVICE_UNAVAILABLE =>
          pagerDutyLog(SERVICE_UNAVAILABLE_FROM_DES, logMessage(response))
          handleDESError(response)
        case BAD_REQUEST | UNPROCESSABLE_ENTITY =>
          pagerDutyLog(FOURXX_RESPONSE_FROM_DES, logMessage(response))
          handleDESError(response)
        case _ =>
          pagerDutyLog(UNEXPECTED_RESPONSE_FROM_DES, logMessage(response))
          handleDESError(response, Some(INTERNAL_SERVER_ERROR))
      }
    }
  }

}
