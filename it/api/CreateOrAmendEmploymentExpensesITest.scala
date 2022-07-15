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

package api

import helpers.WiremockSpec
import models.{DesErrorBodyModel, Expenses, ExpensesType, IgnoreExpenses}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import utils.DESTaxYearHelper.desTaxYearConverter

class CreateOrAmendEmploymentExpensesITest extends WiremockSpec with ScalaFutures {

  private val successNino: String = "AA123123A"
  private val taxYear = 2021

  trait Setup {
    private val spanUnitLength = 5
    implicit val patienceConfig: PatienceConfig = PatienceConfig(Span(spanUnitLength, Seconds))
    val authorization: (String, String) = HeaderNames.AUTHORIZATION -> "mock-bearer-token"
    val agentClientCookie: Map[String, String] = Map("MTDITID" -> "555555555")
    val mtditidHeader: (String, String) = ("mtditid", "555555555")
    val view = "LATEST"
    auditStubs()
  }

  private val requestBodyWithoutIgnoreExpenses =
    Json.obj(
      "expenses" -> Json.obj(
        "businessTravelCosts" -> 0,
        "jobExpenses" -> 0,
        "flatRateJobExpenses" -> 0,
        "professionalSubscriptions" -> 0,
        "hotelAndMealExpenses" -> 0,
        "otherAndCapitalAllowances" -> 0,
        "vehicleExpenses" -> 0,
        "mileageAllowanceRelief" -> 0,
      )
    )

  private def requestBodyWithIgnoreExpenses(ignoreExpenses: Boolean) =
    Json.obj(
      "ignoreExpenses" -> ignoreExpenses,
      "expenses" -> Json.obj(
        "businessTravelCosts" -> 0,
        "jobExpenses" -> 0,
        "flatRateJobExpenses" -> 0,
        "professionalSubscriptions" -> 0,
        "hotelAndMealExpenses" -> 0,
        "otherAndCapitalAllowances" -> 0,
        "vehicleExpenses" -> 0,
        "mileageAllowanceRelief" -> 0,
      )
    )

  "create or amend employment expenses" when {

    val integrationFrameworkCreateExpenseUrl = s"/income-tax/expenses/employments/$successNino/${desTaxYearConverter(taxYear)}"

    "the user is an individual" must {
      val ignoreExpenseDesModel = Json.toJson(IgnoreExpenses(true)).toString()
      val expenseDesModel = Json.toJson(Expenses(ExpensesType(Some(0), Some(0), Some(0), Some(0), Some(0), Some(0), Some(0), Some(0)))).toString()

      "return 204 when request contains ignoreExpense(true)" in new Setup {
        val result: WSResponse = {
          authorised()
          stubPutWithoutResponseBody(integrationFrameworkCreateExpenseUrl, ignoreExpenseDesModel, NO_CONTENT)
          stubPutWithoutResponseBody(integrationFrameworkCreateExpenseUrl, expenseDesModel, NO_CONTENT)

          await(buildClient(s"/income-tax-expenses/income-tax/nino/$successNino/sources")
            .withQueryStringParameters("taxYear" -> s"$taxYear")
            .withHttpHeaders(mtditidHeader, authorization)
            .put(requestBodyWithIgnoreExpenses(true))
          )
        }

        result.status mustBe NO_CONTENT
      }

      "return 204 when request does not contain ignoreExpense" in new Setup {
        val result: WSResponse = {
          authorised()
          stubPutWithoutResponseBody(integrationFrameworkCreateExpenseUrl, expenseDesModel, NO_CONTENT)

          await(buildClient(s"/income-tax-expenses/income-tax/nino/$successNino/sources")
            .withQueryStringParameters("taxYear" -> s"$taxYear")
            .withHttpHeaders(mtditidHeader, authorization)
            .put(requestBodyWithoutIgnoreExpenses)
          )
        }

        result.status mustBe NO_CONTENT
      }

      "return 204 when request contains ignoreExpense(false)" in new Setup {
        val result: WSResponse = {
          authorised()
          stubPutWithoutResponseBody(integrationFrameworkCreateExpenseUrl, expenseDesModel, NO_CONTENT)

          await(buildClient(s"/income-tax-expenses/income-tax/nino/$successNino/sources")
            .withQueryStringParameters("taxYear" -> s"$taxYear")
            .withHttpHeaders(mtditidHeader, authorization)
            .put(requestBodyWithIgnoreExpenses(false))
          )
        }

        result.status mustBe NO_CONTENT
      }

      "return 400 if the request body is invalid" in new Setup {
        val result: WSResponse = {
          authorised()

          await(buildClient(s"/income-tax-expenses/income-tax/nino/$successNino/sources")
            .withQueryStringParameters("taxYear" -> s"$taxYear")
            .withHttpHeaders(mtditidHeader, authorization)
            .put(Json.obj())
          )
        }

        result.status mustBe BAD_REQUEST
      }

      "return 503 if a downstream error occurs" in new Setup {
        val errorResponseBody: String = Json.toJson(DesErrorBodyModel("SERVICE_UNAVAILABLE", "The service is temporarily unavailable")).toString()

        val result: WSResponse = {
          authorised()
          stubPutWithResponseBody(integrationFrameworkCreateExpenseUrl, expenseDesModel, errorResponseBody, SERVICE_UNAVAILABLE)

          await(buildClient(s"/income-tax-expenses/income-tax/nino/$successNino/sources")
            .withQueryStringParameters("taxYear" -> s"$taxYear")
            .withHttpHeaders(mtditidHeader, authorization)
            .put(requestBodyWithoutIgnoreExpenses)
          )
        }

        result.status mustBe SERVICE_UNAVAILABLE
        Json.parse(result.body) mustBe Json.obj("code" -> "SERVICE_UNAVAILABLE", "reason" -> "The service is temporarily unavailable")
      }

      "return 401 if the user has no HMRC-MTD-IT enrolment" in new Setup {
        val result: WSResponse = {
          unauthorisedOtherEnrolment()

          await(buildClient(s"/income-tax-expenses/income-tax/nino/$successNino/sources")
            .withQueryStringParameters("taxYear" -> s"$taxYear")
            .withHttpHeaders(mtditidHeader, authorization)
            .put(requestBodyWithIgnoreExpenses(true))
          )
        }

        result.status mustBe UNAUTHORIZED
        result.body mustBe ""
      }

      "return 401 if the request has no MTDITID header present" in new Setup {
        val result: WSResponse = {
          unauthorisedOtherEnrolment()

          await(buildClient(s"/income-tax-expenses/income-tax/nino/$successNino/sources")
            .withQueryStringParameters("taxYear" -> s"$taxYear")
            .put(requestBodyWithIgnoreExpenses(true))
          )
        }

        result.status mustBe UNAUTHORIZED
        result.body mustBe ""
      }
    }
  }
}
