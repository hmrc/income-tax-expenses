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
import models.ToRemove.HmrcHeld
import models.{DesErrorBodyModel, DesErrorModel, ToRemove}
import org.scalamock.handlers.CallHandler5
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, defaultAwaitTimeout}
import services.DeleteOrIgnoreEmploymentExpensesService
import uk.gov.hmrc.http.HeaderCarrier
import utils.TestUtils

import scala.concurrent.{ExecutionContext, Future}

class DeleteOrIgnoreEmploymentExpensesControllerSpec extends TestUtils {

  val deleteOverrideEmploymentExpensesService: DeleteOrIgnoreEmploymentExpensesService = mock[DeleteOrIgnoreEmploymentExpensesService]

  val deleteOverrideEmploymentExpensesController = new DeleteOrIgnoreEmploymentExpensesController(
    deleteOverrideEmploymentExpensesService,
    authorisedAction,
    mockControllerComponents)

  val nino = "123456789"
  val taxYear = 2022


  "deleteOverrideEmploymentExpenses" when {

    //noinspection ScalaStyle
    def mockDeleteOverrideEmploymentExpensesSuccess(): CallHandler5[String, ToRemove, Int, HeaderCarrier, ExecutionContext, Future[DeleteOverrideEmploymentExpensesResponse]] = {
      val response: DeleteOverrideEmploymentExpensesResponse = Right(())
      (deleteOverrideEmploymentExpensesService.deleteOrIgnoreEmploymentExpenses(_: String, _: ToRemove, _: Int)(_: HeaderCarrier, _: ExecutionContext))
        .expects(*, *, *, *, *)
        .returning(Future.successful(response))
    }

    //noinspection ScalaStyle
    def mockDeleteOverrideEmploymentExpensesFailure(httpStatus: Int): CallHandler5[String, ToRemove, Int, HeaderCarrier, ExecutionContext, Future[DeleteOverrideEmploymentExpensesResponse]] = {
      val error: DeleteOverrideEmploymentExpensesResponse = Left(DesErrorModel(httpStatus, DesErrorBodyModel("DES_CODE", "DES_REASON")))
      (deleteOverrideEmploymentExpensesService.deleteOrIgnoreEmploymentExpenses(_: String, _: ToRemove, _: Int)(_: HeaderCarrier, _: ExecutionContext))
        .expects(*, *, *, *, *)
        .returning(Future.successful(error))
    }

    val mtditid: String = "1234567890"
    val fakeRequest = FakeRequest("DELETE", "/TBC").withHeaders("mtditid" -> mtditid)
    val validToRemove = HmrcHeld.value
    val invalidToRemove = "unsupported"

    "the request is from an individual" should {

      "return a NoContent 204 response when the delete is successful" in {
        val result = {
          mockAuth()
          mockDeleteOverrideEmploymentExpensesSuccess()
          deleteOverrideEmploymentExpensesController.deleteOrIgnoreEmploymentExpenses(nino, validToRemove, taxYear)(fakeRequest)
        }
        status(result) mustBe NO_CONTENT
      }

      s"return a BadRequest response when toRemove string is not supported" in {
        val result = {
          mockAuth()
          deleteOverrideEmploymentExpensesController.deleteOrIgnoreEmploymentExpenses(nino, invalidToRemove, taxYear)(fakeRequest)
        }

        status(result) mustBe BAD_REQUEST
        contentAsJson(result) mustBe Json.obj("code" -> "INVALID_TO_REMOVE_PARAMETER" , "reason" -> "toRemove parameter is not: ALL, HMRC-HELD or CUSTOMER")
      }

      Seq(INTERNAL_SERVER_ERROR, SERVICE_UNAVAILABLE, BAD_REQUEST, NOT_FOUND).foreach { httpErrorCode =>
        s"return a $httpErrorCode response when des returns a $httpErrorCode" in {
          val result = {
            mockAuth()
            mockDeleteOverrideEmploymentExpensesFailure(httpErrorCode)
            deleteOverrideEmploymentExpensesController.deleteOrIgnoreEmploymentExpenses(nino, validToRemove, taxYear)(fakeRequest)
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
          deleteOverrideEmploymentExpensesController.deleteOrIgnoreEmploymentExpenses(nino, validToRemove, taxYear)(fakeRequest)
        }

        status(result) mustBe NO_CONTENT
      }

      Seq(INTERNAL_SERVER_ERROR, SERVICE_UNAVAILABLE, BAD_REQUEST, NOT_FOUND).foreach { httpErrorCode =>
        s"return a $httpErrorCode response when des returns a $httpErrorCode" in {
          val result = {
            mockAuthAsAgent()
            mockDeleteOverrideEmploymentExpensesFailure(httpErrorCode)
            deleteOverrideEmploymentExpensesController.deleteOrIgnoreEmploymentExpenses(nino, validToRemove, taxYear)(fakeRequest)
          }

          status(result) mustBe httpErrorCode
          contentAsJson(result) mustBe Json.obj("code" -> "DES_CODE" , "reason" -> "DES_REASON")
        }
      }
    }
  }

}
