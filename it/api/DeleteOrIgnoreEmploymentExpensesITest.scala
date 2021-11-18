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

package api

import helpers.WiremockSpec
import models.ToRemove.{All, Customer, HmrcHeld}
import models.{DesErrorBodyModel, IgnoreExpenses}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import play.api.http.Status._
import play.api.libs.json.Json
import utils.DESTaxYearHelper.desTaxYearConverter

class DeleteOrIgnoreEmploymentExpensesITest extends WiremockSpec with ScalaFutures {

  private val successNino: String = "AA123123A"
  private val taxYear = 2021

  trait Setup {
    implicit val patienceConfig: PatienceConfig = PatienceConfig(Span(5, Seconds))
    val agentClientCookie: Map[String, String] = Map("MTDITID" -> "555555555")
    val mtditidHeader = ("mtditid", "555555555")
    val view = "LATEST"
    auditStubs()
  }

  "delete or ignore employment expenses" when {

    val desExpenseUrl = s"/income-tax/expenses/employments/$successNino/${desTaxYearConverter(taxYear)}"

    "the user is an individual" must {
      val ignoreExpenseDesModel = Json.toJson(IgnoreExpenses(true)).toString()

      "return 204 when request contains 'HMRC-HELD' as toRemove path parameter" in new Setup {
        val result = {
          authorised()
          stubPutWithoutResponseBody(desExpenseUrl, ignoreExpenseDesModel, NO_CONTENT)

          await(buildClient(s"/income-tax-expenses/income-tax/nino/$successNino/sources/${HmrcHeld.value}")
            .withQueryStringParameters("taxYear" -> s"$taxYear")
            .withHttpHeaders(mtditidHeader)
            .delete()
          )
        }

        result.status mustBe NO_CONTENT
      }

      "return 204 when request contains 'CUSTOMER' as toRemove path parameter" in new Setup {
        val result = {
          authorised()
          stubDeleteWithoutResponseBody(desExpenseUrl, NO_CONTENT)

          await(buildClient(s"/income-tax-expenses/income-tax/nino/$successNino/sources/${Customer.value}")
            .withQueryStringParameters("taxYear" -> s"$taxYear")
            .withHttpHeaders(mtditidHeader)
            .delete()
          )
        }

        result.status mustBe NO_CONTENT
      }

      "return 204 when request contains 'ALL' as toRemove path parameter" in new Setup {
        val result = {
          authorised()
          stubDeleteWithoutResponseBody(desExpenseUrl, NO_CONTENT)
          stubPutWithoutResponseBody(desExpenseUrl, ignoreExpenseDesModel, NO_CONTENT)

          await(buildClient(s"/income-tax-expenses/income-tax/nino/$successNino/sources/${All.value}")
            .withQueryStringParameters("taxYear" -> s"$taxYear")
            .withHttpHeaders(mtditidHeader)
            .delete()
          )
        }

        result.status mustBe NO_CONTENT
      }

      "return 400 when request contains an unsupported toRemove path parameter" in new Setup {
        val unsupported = "unsupported-to-remove"
        val result = {
          authorised()

          await(buildClient(s"/income-tax-expenses/income-tax/nino/$successNino/sources/$unsupported")
            .withQueryStringParameters("taxYear" -> s"$taxYear")
            .withHttpHeaders(mtditidHeader)
            .delete()
          )
        }

        result.status mustBe BAD_REQUEST
        Json.parse(result.body) mustBe Json.toJson(DesErrorBodyModel("INVALID_TO_REMOVE_PARAMETER", "toRemove parameter is not: ALL, HMRC-HELD or CUSTOMER"))
      }

      "return 503 if a downstream error occurs" in new Setup {
        val errorResponseBody = Json.toJson(DesErrorBodyModel("SERVICE_UNAVAILABLE", "The service is temporarily unavailable")).toString()

        val result = {
          authorised()
          stubDeleteWithResponseBody(desExpenseUrl, SERVICE_UNAVAILABLE, errorResponseBody)

          await(buildClient(s"/income-tax-expenses/income-tax/nino/$successNino/sources/${All.value}")
            .withQueryStringParameters("taxYear" -> s"$taxYear")
            .withHttpHeaders(mtditidHeader)
            .delete()
          )
        }

        result.status mustBe SERVICE_UNAVAILABLE
        Json.parse(result.body) mustBe Json.obj("code" -> "SERVICE_UNAVAILABLE", "reason" -> "The service is temporarily unavailable")
      }

      "return 401 if the user has no HMRC-MTD-IT enrolment" in new Setup {
        val result = {
          unauthorisedOtherEnrolment()

          await(buildClient(s"/income-tax-expenses/income-tax/nino/$successNino/sources/${All.value}")
            .withQueryStringParameters("taxYear" -> s"$taxYear")
            .withHttpHeaders(mtditidHeader)
            .delete()
          )
        }

        result.status mustBe UNAUTHORIZED
        result.body mustBe ""
      }

      "return 401 if the request has no MTDITID header present" in new Setup {
        val result = {
          unauthorisedOtherEnrolment()

          await(buildClient(s"/income-tax-expenses/income-tax/nino/$successNino/sources/${All.value}")
            .withQueryStringParameters("taxYear" -> s"$taxYear")
            .delete()
          )
        }

        result.status mustBe UNAUTHORIZED
        result.body mustBe ""
      }
    }

  }
}
