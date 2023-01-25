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

package services

import connectors.{CreateOrAmendEmploymentExpensesConnector, DeleteOverrideEmploymentExpensesConnector}
import models.ToRemove.{All, Customer, HmrcHeld}
import models.{DesErrorBodyModel, DesErrorModel, EmploymentExpensesRequestModel, IgnoreExpenses}
import play.api.http.Status._
import uk.gov.hmrc.http.HeaderCarrier
import utils.TestUtils

import scala.concurrent.Future

class DeleteOrIgnoreEmploymentExpensesServiceSpec extends TestUtils {

  private val mockDeleteOverrideExpensesConnector = mock[DeleteOverrideEmploymentExpensesConnector]
  private val mockCreateOrAmendEmploymentExpensesConnector = mock[CreateOrAmendEmploymentExpensesConnector]
  private val mockDeleteOverrideExpensesService =
    new DeleteOrIgnoreEmploymentExpensesService(mockDeleteOverrideExpensesConnector, mockCreateOrAmendEmploymentExpensesConnector)

  val nino = "123456789"
  val taxYear = 2022

  "deleteOverrideEmploymentExpenses" should {

    "return Right" when {

      "toRemove is HmrcHeld and connector call(s) succeed" in {
        (mockCreateOrAmendEmploymentExpensesConnector.createOrAmendEmploymentExpenses(_: String, _: Int, _: EmploymentExpensesRequestModel)(_: HeaderCarrier))
          .expects(nino, taxYear, IgnoreExpenses(true), *)
          .returning(Future.successful(Right(())))

        val result = mockDeleteOverrideExpensesService.deleteOrIgnoreEmploymentExpenses(nino, HmrcHeld, taxYear)

        await(result) mustBe Right(())
      }

      "toRemove is Customer and connector call(s) succeed" in {
        (mockDeleteOverrideExpensesConnector.deleteOverrideEmploymentExpenses(_: String, _: Int)(_: HeaderCarrier))
          .expects(nino, taxYear, *)
          .returning(Future.successful(Right(())))

        val result = mockDeleteOverrideExpensesService.deleteOrIgnoreEmploymentExpenses(nino, Customer, taxYear)

        await(result) mustBe Right(())
      }

      "toRemove is All and connector call(s) succeed" in {
        (mockDeleteOverrideExpensesConnector.deleteOverrideEmploymentExpenses(_: String, _: Int)(_: HeaderCarrier))
          .expects(nino, taxYear, *)
          .returning(Future.successful(Right(())))

        (mockCreateOrAmendEmploymentExpensesConnector.createOrAmendEmploymentExpenses(_: String, _: Int, _: EmploymentExpensesRequestModel)(_: HeaderCarrier))
          .expects(nino, taxYear, IgnoreExpenses(true), *)
          .returning(Future.successful(Right(())))

        val result = mockDeleteOverrideExpensesService.deleteOrIgnoreEmploymentExpenses(nino, All, taxYear)

        await(result) mustBe Right(())
      }
    }

    "return Left containing a DES error" when {

      "toRemove is HmrcHeld and createOrAmendEmploymentExpenses connector call fails" in {
        val desError = DesErrorModel(INTERNAL_SERVER_ERROR, DesErrorBodyModel("DES_CODE", "DES_REASON"))

        (mockCreateOrAmendEmploymentExpensesConnector.createOrAmendEmploymentExpenses(_: String, _: Int, _: EmploymentExpensesRequestModel)(_: HeaderCarrier))
          .expects(nino, taxYear, IgnoreExpenses(true), *)
          .returning(Future.successful(Left(desError)))

        val result = mockDeleteOverrideExpensesService.deleteOrIgnoreEmploymentExpenses(nino, HmrcHeld, taxYear)

        await(result) mustBe Left(desError)
      }

      "toRemove is Customer and deleteOverrideExpenses connector call fails" in {
        val desError = DesErrorModel(INTERNAL_SERVER_ERROR, DesErrorBodyModel("DES_CODE", "DES_REASON"))

        (mockDeleteOverrideExpensesConnector.deleteOverrideEmploymentExpenses(_: String, _: Int)(_: HeaderCarrier))
          .expects(nino, taxYear, *)
          .returning(Future.successful(Left(desError)))

        val result = mockDeleteOverrideExpensesService.deleteOrIgnoreEmploymentExpenses(nino, Customer, taxYear)

        await(result) mustBe Left(desError)
      }

      "toRemove is All and deleteOverrideExpenses connector call fails" in {
        val desError = DesErrorModel(INTERNAL_SERVER_ERROR, DesErrorBodyModel("DES_CODE", "DES_REASON"))

        (mockDeleteOverrideExpensesConnector.deleteOverrideEmploymentExpenses(_: String, _: Int)(_: HeaderCarrier))
          .expects(nino, taxYear, *)
          .returning(Future.successful(Left(desError)))

        val result = mockDeleteOverrideExpensesService.deleteOrIgnoreEmploymentExpenses(nino, All, taxYear)

        await(result) mustBe Left(desError)
      }

      "toRemove is All and createOrAmendEmploymentExpenses connector call fails" in {
        val desError = DesErrorModel(INTERNAL_SERVER_ERROR, DesErrorBodyModel("DES_CODE", "DES_REASON"))

        (mockDeleteOverrideExpensesConnector.deleteOverrideEmploymentExpenses(_: String, _: Int)(_: HeaderCarrier))
          .expects(nino, taxYear, *)
          .returning(Future.successful(Right(())))

        (mockCreateOrAmendEmploymentExpensesConnector.createOrAmendEmploymentExpenses(_: String, _: Int, _: EmploymentExpensesRequestModel)(_: HeaderCarrier))
          .expects(nino, taxYear, IgnoreExpenses(true), *)
          .returning(Future.successful(Left(desError)))

        val result = mockDeleteOverrideExpensesService.deleteOrIgnoreEmploymentExpenses(nino, All, taxYear)

        await(result) mustBe Left(desError)
      }
    }
  }

}
