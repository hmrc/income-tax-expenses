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

package services

import connectors.DeleteOverrideEmploymentExpensesConnector
import models.{DesErrorBodyModel, DesErrorModel}
import play.api.http.Status._
import uk.gov.hmrc.http.HeaderCarrier
import utils.TestUtils

import scala.concurrent.Future

class DeleteOverrideEmploymentExpensesServiceSpec extends TestUtils {

  private val mockDeleteOverrideExpensesConnector = mock[DeleteOverrideEmploymentExpensesConnector]
  private val mockDeleteOverrideExpensesService = new DeleteOverrideEmploymentExpensesService(mockDeleteOverrideExpensesConnector)

  val nino = "123456789"
  val taxYear = 2022

  "deleteOverrideEmploymentExpenses" should {

    "return Right" when {

      "deleteOverrideEmploymentExpenses connector call succeeds" in {

        (mockDeleteOverrideExpensesConnector.deleteOverrideEmploymentExpenses(_: String, _: Int)(_: HeaderCarrier))
          .expects(nino, taxYear, *)
          .returning(Future.successful(Right(())))

        val result = mockDeleteOverrideExpensesService.deleteOverrideEmploymentExpenses(nino, taxYear)

        await(result) mustBe Right(())
      }
    }

    "return Left containing a DES error" when {

      "deleteOverrideEmploymentExpenses connector call fails" in {

        val desError = DesErrorModel(INTERNAL_SERVER_ERROR, DesErrorBodyModel("DES_CODE", "DES_REASON"))

        (mockDeleteOverrideExpensesConnector.deleteOverrideEmploymentExpenses(_: String, _: Int)(_: HeaderCarrier))
          .expects(nino, taxYear, *)
          .returning(Future.successful(Left(desError)))

        val result = mockDeleteOverrideExpensesService.deleteOverrideEmploymentExpenses(nino, taxYear)

        await(result) mustBe Left(desError)

      }
    }
  }

}
