/*
 * Copyright 2023 HM Revenue & Customs
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
import models.{CreateExpensesRequestModel, DesErrorBodyModel, DesErrorModel}
import org.scalamock.handlers.CallHandler5
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, NO_CONTENT}
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, defaultAwaitTimeout}
import services.CreateOrAmendEmploymentExpensesService
import uk.gov.hmrc.http.HeaderCarrier
import utils.TestUtils

import scala.concurrent.{ExecutionContext, Future}

class CreateOrAmendEmploymentExpensesControllerSpec extends TestUtils {


  val mockService: CreateOrAmendEmploymentExpensesService = mock[CreateOrAmendEmploymentExpensesService]
  val controller = new CreateOrAmendEmploymentExpensesController(mockService, authorisedAction, mockControllerComponents)

  val nino: String = "123456789"
  val mtditid: String = "1234567890"
  val taxYear: Int = 2022

  val fakePutRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("PUT", "/some_path_tbc").withHeaders("mtditid" -> mtditid)

  val serverErrorModel: DesErrorBodyModel = DesErrorBodyModel("SERVER_ERROR", "Internal server error")
  
  //noinspection ScalaStyle
  def mockCreateOrAmendEmploymentExpensesSuccess(): CallHandler5[String, Int, CreateExpensesRequestModel, HeaderCarrier, ExecutionContext, Future[CreateOrAmendEmploymentExpenseResponse]] = {
    val response: CreateOrAmendEmploymentExpenseResponse = Right(())
    (mockService.createOrAmendEmploymentExpenses(_: String, _: Int, _: CreateExpensesRequestModel)(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, *, *, *, *)
      .returning(Future.successful(response))
  }
  
  //noinspection ScalaStyle
  def mockCreateOrAmendEmploymentExpensesError(): CallHandler5[String, Int, CreateExpensesRequestModel, HeaderCarrier, ExecutionContext, Future[CreateOrAmendEmploymentExpenseResponse]] = {
    val response: CreateOrAmendEmploymentExpenseResponse = Left(DesErrorModel(INTERNAL_SERVER_ERROR, serverErrorModel))
    (mockService.createOrAmendEmploymentExpenses(_: String, _: Int, _: CreateExpensesRequestModel)(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, *, *, *, *)
      .returning(Future.successful(response))
  }

  ".createOrAmendEmploymentExpenses" should {
    val requestBodyWithoutIgnoreExpenses =
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

    val requestBodyWithIgnoreExpenses =
      Json.obj(
        "ignoreExpenses" -> true,
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

      "request body is valid and has ignoreExpenses field" in {
        val request = fakePutRequest.withJsonBody(requestBodyWithIgnoreExpenses)

        val result = {
          mockAuth()
          mockCreateOrAmendEmploymentExpensesSuccess()
          controller.createOrAmendEmploymentExpenses(nino, taxYear)(request)
        }

        status(result) mustBe NO_CONTENT
      }

      "request body is valid does not have ignoreExpenses field" in {
        val request = fakePutRequest.withJsonBody(requestBodyWithoutIgnoreExpenses)

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
      val request = fakePutRequest.withJsonBody(requestBodyWithIgnoreExpenses)

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
