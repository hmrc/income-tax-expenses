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

import connectors.CreateOrAmendEmploymentExpensesConnector
import connectors.httpParsers.CreateOrAmendEmploymentExpensesHttpParser.CreateOrAmendEmploymentExpenseResponse
import models.{DesErrorBodyModel, DesErrorModel, EmploymentExpensesRequestModel, IgnoreExpenses}
import org.scalamock.handlers.CallHandler4
import play.api.http.Status.INTERNAL_SERVER_ERROR
import uk.gov.hmrc.http.HeaderCarrier
import utils.TestUtils

import scala.concurrent.Future

class CreateOrAmendEmploymentExpensesServiceTest extends TestUtils {

  val mockConnector = mock[CreateOrAmendEmploymentExpensesConnector]
  val service = new CreateOrAmendEmploymentExpensesService(mockConnector)

  val nino: String = "123456789"
  val taxYear: Int = 2022
  val serverErrorModel: DesErrorBodyModel = DesErrorBodyModel("SERVER_ERROR", "Internal server error")

  def mockCreateOrAmendEmploymentExpensesSuccess(): CallHandler4[String, Int, EmploymentExpensesRequestModel, HeaderCarrier, Future[CreateOrAmendEmploymentExpenseResponse]] = {
    (mockConnector.createOrAmendEmploymentExpenses(_: String, _: Int, _: EmploymentExpensesRequestModel)(_: HeaderCarrier))
      .expects(*, *, *, *)
      .returning(Future.successful(Right(())))
  }

  def mockCreateOrAmendEmploymentExpensesError(): CallHandler4[String, Int, EmploymentExpensesRequestModel, HeaderCarrier, Future[CreateOrAmendEmploymentExpenseResponse]] = {
    (mockConnector.createOrAmendEmploymentExpenses(_: String, _: Int, _: EmploymentExpensesRequestModel)(_: HeaderCarrier))
      .expects(*, *, *, *)
      .returning(Future.successful(Left(DesErrorModel(INTERNAL_SERVER_ERROR, serverErrorModel))))
  }


  ".createOrAmendEmploymentExpenses" should {

    val expenses = IgnoreExpenses(true)

    "return Right(()) when connector calls succeeds" in {
      val result = {
        mockCreateOrAmendEmploymentExpensesSuccess()
        service.createOrAmendEmploymentExpenses(nino, taxYear, expenses)
      }

      await(result) mustBe Right(())
    }

    "return Left(Error) when connector calls fails" in {
      val result = {
        mockCreateOrAmendEmploymentExpensesError()
        service.createOrAmendEmploymentExpenses(nino, taxYear, expenses)
      }

      await(result) mustBe Left(DesErrorModel(INTERNAL_SERVER_ERROR,  DesErrorBodyModel("SERVER_ERROR", "Internal server error")))
    }

  }

}
