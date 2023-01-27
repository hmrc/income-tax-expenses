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

import connectors.{CreateOrAmendEmploymentExpensesConnector, DeleteOverrideEmploymentExpensesConnector, DeleteOverrideEmploymentExpensesIFConnector}
import models.ToRemove.{All, Customer, HmrcHeld}
import models.{DesErrorBodyModel, DesErrorModel, EmploymentExpensesRequestModel, IgnoreExpenses}
import play.api.http.Status._
import uk.gov.hmrc.http.HeaderCarrier
import utils.TestUtils

import scala.concurrent.Future

class DeleteOrIgnoreEmploymentExpensesServiceSpec extends TestUtils {

  private val mockDeleteOverrideExpensesConnector = mock[DeleteOverrideEmploymentExpensesConnector]
  private val mockDeleteOverrideExpensesIFConnector = mock[DeleteOverrideEmploymentExpensesIFConnector]
  private val mockCreateOrAmendEmploymentExpensesConnector = mock[CreateOrAmendEmploymentExpensesConnector]
  private val underTest = new DeleteOrIgnoreEmploymentExpensesService(mockDeleteOverrideExpensesConnector,
    mockDeleteOverrideExpensesIFConnector,
    mockCreateOrAmendEmploymentExpensesConnector)

  private val nino = "123456789"
  private val taxYear = 2022
  private val taxYear2024 = 2024

  "deleteOverrideEmploymentExpenses" should {
    "return Right" when {
      "toRemove is HmrcHeld and connector call(s) succeed" in {
        (mockCreateOrAmendEmploymentExpensesConnector.createOrAmendEmploymentExpenses(_: String, _: Int, _: EmploymentExpensesRequestModel)(_: HeaderCarrier))
          .expects(nino, taxYear, IgnoreExpenses(true), *)
          .returning(Future.successful(Right(())))

        await(underTest.deleteOrIgnoreEmploymentExpenses(nino, HmrcHeld, taxYear)) mustBe Right(())
      }

      "year is not 23-24" should {
        "toRemove is Customer and connector call(s) succeed using the DES connector" in {
          (mockDeleteOverrideExpensesConnector.deleteOverrideEmploymentExpenses(_: String, _: Int)(_: HeaderCarrier))
            .expects(nino, taxYear, *)
            .returning(Future.successful(Right(())))

          await(underTest.deleteOrIgnoreEmploymentExpenses(nino, Customer, taxYear)) mustBe Right(())
        }

        "toRemove is All and connector call(s) succeed using the DES connector" in {
          (mockDeleteOverrideExpensesConnector.deleteOverrideEmploymentExpenses(_: String, _: Int)(_: HeaderCarrier))
            .expects(nino, taxYear, *)
            .returning(Future.successful(Right(())))

          (mockCreateOrAmendEmploymentExpensesConnector.createOrAmendEmploymentExpenses(_: String, _: Int, _: EmploymentExpensesRequestModel)(_: HeaderCarrier))
            .expects(nino, taxYear, IgnoreExpenses(true), *)
            .returning(Future.successful(Right(())))

          await(underTest.deleteOrIgnoreEmploymentExpenses(nino, All, taxYear)) mustBe Right(())
        }
      }

      "year is 23-24" should {
        "toRemove is Customer and connector call(s) succeed using the IF connector" in {
          (mockDeleteOverrideExpensesIFConnector.deleteOverrideEmploymentExpenses(_: String)(_: HeaderCarrier))
            .expects(nino, *)
            .returning(Future.successful(Right(())))

          await(underTest.deleteOrIgnoreEmploymentExpenses(nino, Customer, taxYear2024)) mustBe Right(())
        }

        "toRemove is All and connector call(s) succeed using the IF connector" in {
          (mockDeleteOverrideExpensesIFConnector.deleteOverrideEmploymentExpenses(_: String)(_: HeaderCarrier))
            .expects(nino, *)
            .returning(Future.successful(Right(())))

          (mockCreateOrAmendEmploymentExpensesConnector.createOrAmendEmploymentExpenses(_: String, _: Int, _: EmploymentExpensesRequestModel)(_: HeaderCarrier))
            .expects(nino, taxYear2024, IgnoreExpenses(true), *)
            .returning(Future.successful(Right(())))

          await(underTest.deleteOrIgnoreEmploymentExpenses(nino, All, taxYear2024)) mustBe Right(())
        }
      }
    }

    "return Left containing an error" when {
      "toRemove is HmrcHeld and createOrAmendEmploymentExpenses connector call fails" in {
        val desError = DesErrorModel(INTERNAL_SERVER_ERROR, DesErrorBodyModel("DES_CODE", "DES_REASON"))

        (mockCreateOrAmendEmploymentExpensesConnector.createOrAmendEmploymentExpenses(_: String, _: Int, _: EmploymentExpensesRequestModel)(_: HeaderCarrier))
          .expects(nino, taxYear, IgnoreExpenses(true), *)
          .returning(Future.successful(Left(desError)))

        await(underTest.deleteOrIgnoreEmploymentExpenses(nino, HmrcHeld, taxYear)) mustBe Left(desError)
      }

      "toRemove is All and createOrAmendEmploymentExpenses connector call fails" in {
        val desError = DesErrorModel(INTERNAL_SERVER_ERROR, DesErrorBodyModel("DES_CODE", "DES_REASON"))

        (mockDeleteOverrideExpensesConnector.deleteOverrideEmploymentExpenses(_: String, _: Int)(_: HeaderCarrier))
          .expects(nino, taxYear, *)
          .returning(Future.successful(Right(())))

        (mockCreateOrAmendEmploymentExpensesConnector.createOrAmendEmploymentExpenses(_: String, _: Int, _: EmploymentExpensesRequestModel)(_: HeaderCarrier))
          .expects(nino, taxYear, IgnoreExpenses(true), *)
          .returning(Future.successful(Left(desError)))

        await(underTest.deleteOrIgnoreEmploymentExpenses(nino, All, taxYear)) mustBe Left(desError)
      }

      "year is not 23-24" should {
        "toRemove is Customer and deleteOverrideExpenses DES connector call fails" in {
          val desError = DesErrorModel(INTERNAL_SERVER_ERROR, DesErrorBodyModel("DES_CODE", "DES_REASON"))

          (mockDeleteOverrideExpensesConnector.deleteOverrideEmploymentExpenses(_: String, _: Int)(_: HeaderCarrier))
            .expects(nino, taxYear, *)
            .returning(Future.successful(Left(desError)))

          await(underTest.deleteOrIgnoreEmploymentExpenses(nino, Customer, taxYear)) mustBe Left(desError)
        }

        "toRemove is All and deleteOverrideExpenses DES connector call fails" in {
          val desError = DesErrorModel(INTERNAL_SERVER_ERROR, DesErrorBodyModel("DES_CODE", "DES_REASON"))

          (mockDeleteOverrideExpensesConnector.deleteOverrideEmploymentExpenses(_: String, _: Int)(_: HeaderCarrier))
            .expects(nino, taxYear, *)
            .returning(Future.successful(Left(desError)))

          await(underTest.deleteOrIgnoreEmploymentExpenses(nino, All, taxYear)) mustBe Left(desError)
        }
      }

      "year is 23-24" should {
        "toRemove is Customer and deleteOverrideExpenses IF connector call fails" in {
          val error = DesErrorModel(INTERNAL_SERVER_ERROR, DesErrorBodyModel("CODE", "REASON"))

          (mockDeleteOverrideExpensesIFConnector.deleteOverrideEmploymentExpenses(_: String)(_: HeaderCarrier))
            .expects(nino, *)
            .returning(Future.successful(Left(error)))

          await(underTest.deleteOrIgnoreEmploymentExpenses(nino, Customer, taxYear2024)) mustBe Left(error)
        }

        "toRemove is All and deleteOverrideExpenses IF connector call fails" in {
          val error = DesErrorModel(INTERNAL_SERVER_ERROR, DesErrorBodyModel("CODE", "REASON"))

          (mockDeleteOverrideExpensesIFConnector.deleteOverrideEmploymentExpenses(_: String)(_: HeaderCarrier))
            .expects(nino, *)
            .returning(Future.successful(Left(error)))

          await(underTest.deleteOrIgnoreEmploymentExpenses(nino, All, taxYear2024)) mustBe Left(error)
        }
      }
    }
  }
}
