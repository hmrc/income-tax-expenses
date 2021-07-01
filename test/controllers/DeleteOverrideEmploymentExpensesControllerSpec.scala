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

import connectors.httpParsers.DeleteOverrideEmploymentExpensesHttpParser.DeleteOverrideEmploymentExpensesResponse
import models.{DesErrorBodyModel, DesErrorModel}
import org.scalamock.handlers.CallHandler3
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, defaultAwaitTimeout}
import services.DeleteOverrideEmploymentExpensesService
import uk.gov.hmrc.http.HeaderCarrier
import utils.TestUtils

import scala.concurrent.Future

class DeleteOverrideEmploymentExpensesControllerSpec extends TestUtils {

  val deleteOverrideEmploymentExpensesService: DeleteOverrideEmploymentExpensesService = mock[DeleteOverrideEmploymentExpensesService]

  val deleteOverrideEmploymentExpensesController = new DeleteOverrideEmploymentExpensesController(
    deleteOverrideEmploymentExpensesService,
    authorisedAction,
    mockControllerComponents)

  val nino = "123456789"
  val taxYear = 2022


  "deleteOverrideEmploymentExpenses" when {


    def mockDeleteOverrideEmploymentExpensesSuccess(): CallHandler3[String, Int, HeaderCarrier, Future[DeleteOverrideEmploymentExpensesResponse]] = {
      val response: DeleteOverrideEmploymentExpensesResponse = Right(())
      (deleteOverrideEmploymentExpensesService.deleteOverrideEmploymentExpenses(_: String, _: Int)(_: HeaderCarrier))
        .expects(*, *, *)
        .returning(Future.successful(response))
    }

    def mockDeleteOverrideEmploymentExpensesFailure(httpStatus: Int): CallHandler3[String, Int,
      HeaderCarrier, Future[DeleteOverrideEmploymentExpensesResponse]] = {
      val error: DeleteOverrideEmploymentExpensesResponse = Left(DesErrorModel(httpStatus, DesErrorBodyModel("DES_CODE", "DES_REASON")))
      (deleteOverrideEmploymentExpensesService.deleteOverrideEmploymentExpenses(_: String, _: Int)(_: HeaderCarrier))
        .expects(*, *, *)
        .returning(Future.successful(error))
    }

    val mtditid: String = "1234567890"
    val fakeRequest = FakeRequest("DELETE", "/TBC").withHeaders("mtditid" -> mtditid)


    "the request is from an individual" should {

      "return a NoContent 204 response when the delete is successful" in {

        val result = {
          mockAuth()
          mockDeleteOverrideEmploymentExpensesSuccess()
          deleteOverrideEmploymentExpensesController.deleteOverrideEmploymentExpenses(nino, taxYear)(fakeRequest)
        }
        status(result) mustBe NO_CONTENT
      }

      Seq(INTERNAL_SERVER_ERROR, SERVICE_UNAVAILABLE, BAD_REQUEST, NOT_FOUND).foreach { httpErrorCode =>
        s"return a $httpErrorCode response when des returns a $httpErrorCode" in {

          val result = {
            mockAuth()
            mockDeleteOverrideEmploymentExpensesFailure(httpErrorCode)
            deleteOverrideEmploymentExpensesController.deleteOverrideEmploymentExpenses(nino, taxYear)(fakeRequest)
          }

          status(result) mustBe httpErrorCode
          contentAsJson(result) mustBe Json.obj("code" -> "DES_CODE" , "reason" -> "DES_REASON")
        }
      }
    }

    "the request from an agent" should {

      "return a NoContent 204 response when the delete is successful" in {

        val result = {
          mockAuthAsAgent()
          mockDeleteOverrideEmploymentExpensesSuccess()
          deleteOverrideEmploymentExpensesController.deleteOverrideEmploymentExpenses(nino, taxYear)(fakeRequest)
        }

        status(result) mustBe NO_CONTENT
      }

      Seq(INTERNAL_SERVER_ERROR, SERVICE_UNAVAILABLE, BAD_REQUEST, NOT_FOUND).foreach { httpErrorCode =>
        s"return a $httpErrorCode response when des returns a $httpErrorCode" in {

          val result = {
            mockAuthAsAgent()
            mockDeleteOverrideEmploymentExpensesFailure(httpErrorCode)
            deleteOverrideEmploymentExpensesController.deleteOverrideEmploymentExpenses(nino, taxYear)(fakeRequest)
          }

          status(result) mustBe httpErrorCode
          contentAsJson(result) mustBe Json.obj("code" -> "DES_CODE" , "reason" -> "DES_REASON")
        }
      }
    }
  }

}
