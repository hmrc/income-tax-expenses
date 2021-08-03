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
import models._
import org.scalamock.handlers.CallHandler4
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR}
import uk.gov.hmrc.http.HeaderCarrier
import utils.TestUtils

import scala.concurrent.Future

class CreateOrAmendEmploymentExpensesServiceSpec extends TestUtils {

  val mockConnector = mock[CreateOrAmendEmploymentExpensesConnector]
  val service = new CreateOrAmendEmploymentExpensesService(mockConnector)

  val nino: String = "123456789"
  val taxYear: Int = 2022
  val serverErrorModel: DesErrorBodyModel = DesErrorBodyModel("SERVER_ERROR", "Internal server error")

  def mockCreateOrAmendEmploymentExpensesSuccess(requestModel: EmploymentExpensesRequestModel): CallHandler4[String, Int, EmploymentExpensesRequestModel, HeaderCarrier, Future[CreateOrAmendEmploymentExpenseResponse]] = {
    (mockConnector.createOrAmendEmploymentExpenses(_: String, _: Int, _: EmploymentExpensesRequestModel)(_: HeaderCarrier))
      .expects(*, *, requestModel, *)
      .returning(Future.successful(Right(())))
  }

  def mockCreateOrAmendEmploymentExpensesError(requestModel: EmploymentExpensesRequestModel): CallHandler4[String, Int, EmploymentExpensesRequestModel, HeaderCarrier, Future[CreateOrAmendEmploymentExpenseResponse]] = {
    (mockConnector.createOrAmendEmploymentExpenses(_: String, _: Int, _: EmploymentExpensesRequestModel)(_: HeaderCarrier))
      .expects(*, *, requestModel, *)
      .returning(Future.successful(Left(DesErrorModel(INTERNAL_SERVER_ERROR, serverErrorModel))))
  }


  ".createOrAmendEmploymentExpenses" should {

    val expensesType = ExpensesType(Some(1), Some(1), None, None, None, None, None, None)

    "return Right(())" when {
      "request model contains ignoreExpenses (true) and All connector calls succeed" in {
        val requestModel = CreateExpensesRequestModel(Some(true), expensesType)

        val ignoreRequestModel = IgnoreExpenses(requestModel.ignoreExpenses.get)
        val expenseRequestModel = Expenses(requestModel.expenses)

        val result = {
          mockCreateOrAmendEmploymentExpensesSuccess(ignoreRequestModel)
          mockCreateOrAmendEmploymentExpensesSuccess(expenseRequestModel)
          service.createOrAmendEmploymentExpenses(nino, taxYear, requestModel)
        }

        await(result) mustBe Right(())
      }

      "request model contains ignoreExpenses(false)" in {
        val requestModel = CreateExpensesRequestModel(Some(false), expensesType)

        val expenseRequestModel = Expenses(requestModel.expenses)

        val result = {
          mockCreateOrAmendEmploymentExpensesSuccess(expenseRequestModel)
          service.createOrAmendEmploymentExpenses(nino, taxYear, requestModel)
        }

        await(result) mustBe Right(())
      }

      "request model does not contain ignoreExpenses and All connector calls succeed" in {
        val requestModel = CreateExpensesRequestModel(None, expensesType)
        val expenseRequestModel = Expenses(requestModel.expenses)

        val result = {
          mockCreateOrAmendEmploymentExpensesSuccess(expenseRequestModel)
          service.createOrAmendEmploymentExpenses(nino, taxYear, requestModel)
        }

        await(result) mustBe Right(())
      }
    }

    "return Left(Error) when connector calls fails" when {
      "request model contains ignoreExpenses(true) and All connector calls fail" in {
        val requestModel = CreateExpensesRequestModel(Some(true), expensesType)

        val ignoreRequestModel = IgnoreExpenses(requestModel.ignoreExpenses.get)

        val result = {
          mockCreateOrAmendEmploymentExpensesError(ignoreRequestModel)
          service.createOrAmendEmploymentExpenses(nino, taxYear, requestModel)
        }

        await(result) mustBe Left(DesErrorModel(INTERNAL_SERVER_ERROR,  DesErrorBodyModel("SERVER_ERROR", "Internal server error")))
      }

      "request model contains ignoreExpenses(true) and first connector call succeeds but second connector call fails" in {
        val requestModel = CreateExpensesRequestModel(Some(true), expensesType)
        val ignoreRequestModel = IgnoreExpenses(requestModel.ignoreExpenses.get)
        val expenseRequestModel = Expenses(requestModel.expenses)

        val result = {
          mockCreateOrAmendEmploymentExpensesSuccess(ignoreRequestModel)
          mockCreateOrAmendEmploymentExpensesError(expenseRequestModel)
          service.createOrAmendEmploymentExpenses(nino, taxYear, requestModel)
        }

        await(result) mustBe Left(DesErrorModel(INTERNAL_SERVER_ERROR,  DesErrorBodyModel("SERVER_ERROR", "Internal server error")))
      }

      "request model does not contain ignoreExpenses and All connector calls fail" in {
        val requestModel = CreateExpensesRequestModel(None, expensesType)
        val expenseRequestModel = Expenses(requestModel.expenses)

        val result = {
          mockCreateOrAmendEmploymentExpensesError(expenseRequestModel)
          service.createOrAmendEmploymentExpenses(nino, taxYear, requestModel)
        }

        await(result) mustBe Left(DesErrorModel(INTERNAL_SERVER_ERROR,  DesErrorBodyModel("SERVER_ERROR", "Internal server error")))
      }
    }

  }

}
