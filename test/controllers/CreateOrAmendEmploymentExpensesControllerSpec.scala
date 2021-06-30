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

package controllers

import connectors.httpParsers.CreateOrAmendEmploymentExpensesHttpParser.CreateOrAmendEmploymentExpenseResponse
import models.{DesErrorBodyModel, DesErrorModel, EmploymentExpensesRequestModel}
import org.scalamock.handlers.CallHandler4
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, NO_CONTENT}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, defaultAwaitTimeout}
import services.CreateOrAmendEmploymentExpensesService
import uk.gov.hmrc.http.HeaderCarrier
import utils.TestUtils

import scala.concurrent.Future

class CreateOrAmendEmploymentExpensesControllerSpec extends TestUtils {


  val mockService = mock[CreateOrAmendEmploymentExpensesService]
  val controller = new CreateOrAmendEmploymentExpensesController(mockService, authorisedAction, mockControllerComponents)

  val nino: String = "123456789"
  val mtditid: String = "1234567890"
  val taxYear: Int = 2022

  val fakePutRequest = FakeRequest("PUT", "/some_path_tbc").withHeaders("mtditid" -> mtditid)

  val serverErrorModel: DesErrorBodyModel = DesErrorBodyModel("SERVER_ERROR", "Internal server error")

  def mockCreateOrAmendEmploymentExpensesSuccess(): CallHandler4[String, Int, EmploymentExpensesRequestModel, HeaderCarrier, Future[CreateOrAmendEmploymentExpenseResponse]] = {
    val response: CreateOrAmendEmploymentExpenseResponse = Right(())
    (mockService.createOrAmendEmploymentExpenses(_: String, _: Int, _: EmploymentExpensesRequestModel)(_: HeaderCarrier))
      .expects(*, *, *, *)
      .returning(Future.successful(response))
  }

  def mockCreateOrAmendEmploymentExpensesError(): CallHandler4[String, Int, EmploymentExpensesRequestModel, HeaderCarrier, Future[CreateOrAmendEmploymentExpenseResponse]] = {
    val response: CreateOrAmendEmploymentExpenseResponse = Left(DesErrorModel(INTERNAL_SERVER_ERROR, serverErrorModel))
    (mockService.createOrAmendEmploymentExpenses(_: String, _: Int, _: EmploymentExpensesRequestModel)(_: HeaderCarrier))
      .expects(*, *, *, *)
      .returning(Future.successful(response))
  }

  ".createOrAmendEmploymentExpenses" should {
    val ignoreExpensesRequestBody = Json.obj("ignoreExpenses" -> true)
    val expensesRequestBody =
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

    "return NO_CONTENT" when {

      "request body is valid and has the structure of IgnoreExpenses" in {
        val request = fakePutRequest.withJsonBody(ignoreExpensesRequestBody)

        val result = {
          mockAuth()
          mockCreateOrAmendEmploymentExpensesSuccess()
          controller.createOrAmendEmploymentExpenses(nino, taxYear)(request)
        }

        status(result) mustBe NO_CONTENT
      }

      "request body is valid and has the structure of Expenses" in {
        val request = fakePutRequest.withJsonBody(expensesRequestBody)

        val result = {
          mockAuth()
          mockCreateOrAmendEmploymentExpensesSuccess()
          controller.createOrAmendEmploymentExpenses(nino, taxYear)(request)
        }

        status(result) mustBe NO_CONTENT
      }

    }

    "return a BAD_REQUEST response when request body is invalid" in {
      val request = fakePutRequest.withJsonBody(Json.obj())

      val result = {
        mockAuth()
        controller.createOrAmendEmploymentExpenses(nino, taxYear)(request)
      }

      status(result) mustBe BAD_REQUEST
    }

    "return an INTERNAL_SERVER_ERROR response when DES returns an INTERNAL_SERVER_ERROR" in {
      val request = fakePutRequest.withJsonBody(expensesRequestBody)

      val result = {
        mockAuth()
        mockCreateOrAmendEmploymentExpensesError()
        controller.createOrAmendEmploymentExpenses(nino, taxYear)(request)
      }

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsJson(result) mustBe Json.obj("code" -> "SERVER_ERROR", "reason" -> "Internal server error")
    }
  }

}
