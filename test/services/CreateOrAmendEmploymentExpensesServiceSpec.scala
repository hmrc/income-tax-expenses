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

package services

import connectors.{CreateOrAmendEmploymentExpensesConnector, DeleteOverrideEmploymentExpensesConnector}
import connectors.httpParsers.CreateOrAmendEmploymentExpensesHttpParser.CreateOrAmendEmploymentExpenseResponse
import connectors.httpParsers.DeleteOverrideEmploymentExpensesHttpParser.DeleteOverrideEmploymentExpensesResponse
import models.{DesErrorModel, _}
import org.scalamock.handlers.{CallHandler3, CallHandler4}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND}
import uk.gov.hmrc.http.HeaderCarrier
import utils.TestUtils

import scala.concurrent.Future

class CreateOrAmendEmploymentExpensesServiceSpec extends TestUtils {

  val mockDeleteConnector: DeleteOverrideEmploymentExpensesConnector = mock[DeleteOverrideEmploymentExpensesConnector]
  val mockConnector: CreateOrAmendEmploymentExpensesConnector = mock[CreateOrAmendEmploymentExpensesConnector]

  val service = new CreateOrAmendEmploymentExpensesService(mockConnector, mockDeleteConnector)

  val nino: String = "123456789"
  val taxYear: Int = 2022
  val serverErrorModel: DesErrorBodyModel = DesErrorBodyModel("SERVER_ERROR", "Internal server error")
  val notFoundErrorModel: DesErrorBodyModel = DesErrorBodyModel("NOT_FOUND", "resource not found")

  def mockCreateOrAmendEmploymentExpensesSuccess(requestModel: EmploymentExpensesRequestModel)
  : CallHandler4[String, Int, EmploymentExpensesRequestModel, HeaderCarrier, Future[CreateOrAmendEmploymentExpenseResponse]] = {
    (mockConnector.createOrAmendEmploymentExpenses(_: String, _: Int, _: EmploymentExpensesRequestModel)(_: HeaderCarrier))
      .expects(*, *, requestModel, *)
      .returning(Future.successful(Right(())))
  }

  def mockCreateOrAmendEmploymentExpensesError(requestModel: EmploymentExpensesRequestModel):
  CallHandler4[String, Int, EmploymentExpensesRequestModel, HeaderCarrier, Future[CreateOrAmendEmploymentExpenseResponse]] = {
    (mockConnector.createOrAmendEmploymentExpenses(_: String, _: Int, _: EmploymentExpensesRequestModel)(_: HeaderCarrier))
      .expects(*, *, requestModel, *)
      .returning(Future.successful(Left(DesErrorModel(INTERNAL_SERVER_ERROR, serverErrorModel))))
  }

  def mockDeleteOverrideEmploymentExpensesSuccess(nino: String, taxYear: Int)
  : CallHandler3[String, Int, HeaderCarrier, Future[DeleteOverrideEmploymentExpensesResponse]] = {
    (mockDeleteConnector.deleteOverrideEmploymentExpenses(_: String, _: Int)(_: HeaderCarrier))
      .expects(nino, taxYear, *)
      .returning(Future.successful(Right(Unit)))

  }

  def mockDeleteOverrideEmploymentExpensesError(nino: String, taxYear: Int, desErrorModel: DesErrorModel)
  : CallHandler3[String, Int, HeaderCarrier, Future[DeleteOverrideEmploymentExpensesResponse]] = {
    (mockDeleteConnector.deleteOverrideEmploymentExpenses(_: String, _: Int)(_: HeaderCarrier))
      .expects(nino, taxYear, *)
      .returning(Future.successful(Left(desErrorModel)))
  }

  ".createOrAmendEmploymentExpenses" should {
    val desErrorNotFound = DesErrorModel(NOT_FOUND, DesErrorBodyModel("DES_CODE", "DES_REASON"))
    val desErrorInternalServerError = DesErrorModel(INTERNAL_SERVER_ERROR, DesErrorBodyModel("SERVER_ERROR", "Internal server error"))
    val expensesType = ExpensesType(Some(1), Some(1), None, None, None, None, None, None)
    val emptyExpensesType = ExpensesType(None, None, None, None, None, None, None, None)

    "return Right()" when {
      "request model contains ignoreExpenses (true) but expenses is empty and delete connector call returns NOT_FOUND" in {

        val requestModel = CreateExpensesRequestModel(Some(true), emptyExpensesType)
        val ignoreRequestModel = IgnoreExpenses(requestModel.ignoreExpenses.get)

        val result = {
          mockCreateOrAmendEmploymentExpensesSuccess(ignoreRequestModel)
          mockDeleteOverrideEmploymentExpensesError(nino, taxYear, desErrorNotFound)
          service.createOrAmendEmploymentExpenses(nino, taxYear, requestModel)
        }

        // Not found delete error is ignored
        await(result) mustBe Right(())
      }

      "request model has ignoreExpenses as None but expenses is empty and delete connector call returns NOT_FOUND" in {

        val requestModel = CreateExpensesRequestModel(None, emptyExpensesType)

        val result = {
          mockDeleteOverrideEmploymentExpensesError(nino, taxYear, desErrorNotFound)
          service.createOrAmendEmploymentExpenses(nino, taxYear, requestModel)
        }

        // Not found delete error is ignored
        await(result) mustBe Right(())
      }

      "request model has ignoreExpenses as false but expenses is empty and delete connector call returns NOT_FOUND" in {

        val requestModel = CreateExpensesRequestModel(Some(false), emptyExpensesType)

        val result = {
          mockDeleteOverrideEmploymentExpensesError(nino, taxYear, desErrorNotFound)
          service.createOrAmendEmploymentExpenses(nino, taxYear, requestModel)
        }

        // Not found delete error is ignored
        await(result) mustBe Right(())
      }

      "request model contains ignoreExpenses (true) and expenses is empty when all connector calls succeed" in {

        val requestModel = CreateExpensesRequestModel(Some(true), emptyExpensesType)
        val ignoreRequestModel = IgnoreExpenses(requestModel.ignoreExpenses.get)

        val result = {
          mockCreateOrAmendEmploymentExpensesSuccess(ignoreRequestModel)
          mockDeleteOverrideEmploymentExpensesSuccess(nino, taxYear)
          service.createOrAmendEmploymentExpenses(nino, taxYear, requestModel)
        }

        await(result) mustBe Right(())
      }

      "request model contains ignoreExpenses(false) and non-empty expenses" in {
        val requestModel = CreateExpensesRequestModel(Some(false), expensesType)

        val expenseRequestModel = Expenses(requestModel.expenses)

        val result = {
          mockCreateOrAmendEmploymentExpensesSuccess(expenseRequestModel)
          service.createOrAmendEmploymentExpenses(nino, taxYear, requestModel)
        }

        await(result) mustBe Right(())
      }

      "request model does not contain ignoreExpenses, has non-empty expenses and All connector calls succeed" in {
        val requestModel = CreateExpensesRequestModel(None, expensesType)
        val expenseRequestModel = Expenses(requestModel.expenses)

        val result = {
          mockCreateOrAmendEmploymentExpensesSuccess(expenseRequestModel)
          service.createOrAmendEmploymentExpenses(nino, taxYear, requestModel)
        }

        await(result) mustBe Right(())
      }

      "request model contains ignoreExpenses (true), has non-empty expenses and All connector calls succeed" in {
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

    }

    "return Left(Error) when connector calls fails" when {

      "request model has ignoreExpenses as false but expenses is empty and delete connector call returns INTERNAL_SERVER_ERROR" in {

        val requestModel = CreateExpensesRequestModel(Some(false), emptyExpensesType)

        val result = {
          mockDeleteOverrideEmploymentExpensesError(nino, taxYear, desErrorInternalServerError)
          service.createOrAmendEmploymentExpenses(nino, taxYear, requestModel)
        }

        // As it's different to Not found the error should not be ignored
        await(result) mustBe Left(desErrorInternalServerError)
      }

      "request model has ignoreExpenses as None but expenses is empty and delete connector call returns INTERNAL_SERVER_ERROR" in {

        val requestModel = CreateExpensesRequestModel(None, emptyExpensesType)

        val result = {
          mockDeleteOverrideEmploymentExpensesError(nino, taxYear, desErrorInternalServerError)
          service.createOrAmendEmploymentExpenses(nino, taxYear, requestModel)
        }

        // As it's different to Not found the error should not be ignored
        await(result) mustBe Left(desErrorInternalServerError)
      }

      "request model contains ignoreExpenses (true) but expenses is empty and Delete connector call returns an error that is other than NOT_FOUND" in {

        val requestModel = CreateExpensesRequestModel(Some(true), emptyExpensesType)
        val ignoreRequestModel = IgnoreExpenses(requestModel.ignoreExpenses.get)

        val result = {
          mockCreateOrAmendEmploymentExpensesSuccess(ignoreRequestModel)
          mockDeleteOverrideEmploymentExpensesError(nino, taxYear, desErrorInternalServerError)
          service.createOrAmendEmploymentExpenses(nino, taxYear, requestModel)
        }
        // As it's different to Not found the error should be ignored
        await(result) mustBe Left(desErrorInternalServerError)
      }

      "request model contains ignoreExpenses(true), non-empty expenses and connector calls fail" in {
        val requestModel = CreateExpensesRequestModel(Some(true), expensesType)

        val ignoreRequestModel = IgnoreExpenses(requestModel.ignoreExpenses.get)

        val result = {
          mockCreateOrAmendEmploymentExpensesError(ignoreRequestModel)
          service.createOrAmendEmploymentExpenses(nino, taxYear, requestModel)
        }

        await(result) mustBe Left(DesErrorModel(INTERNAL_SERVER_ERROR, DesErrorBodyModel("SERVER_ERROR", "Internal server error")))
      }

      "request model contains ignoreExpenses(true), non-empty expenses and first connector call succeeds but second connector call fails" in {
        val requestModel = CreateExpensesRequestModel(Some(true), expensesType)
        val ignoreRequestModel = IgnoreExpenses(requestModel.ignoreExpenses.get)
        val expenseRequestModel = Expenses(requestModel.expenses)

        val result = {
          mockCreateOrAmendEmploymentExpensesSuccess(ignoreRequestModel)
          mockCreateOrAmendEmploymentExpensesError(expenseRequestModel)
          service.createOrAmendEmploymentExpenses(nino, taxYear, requestModel)
        }

        await(result) mustBe Left(DesErrorModel(INTERNAL_SERVER_ERROR, DesErrorBodyModel("SERVER_ERROR", "Internal server error")))
      }

      "request model does not contain ignoreExpenses, has non-empty expenses and All connector calls fail" in {
        val requestModel = CreateExpensesRequestModel(None, expensesType)
        val expenseRequestModel = Expenses(requestModel.expenses)

        val result = {
          mockCreateOrAmendEmploymentExpensesError(expenseRequestModel)
          service.createOrAmendEmploymentExpenses(nino, taxYear, requestModel)
        }

        await(result) mustBe Left(DesErrorModel(INTERNAL_SERVER_ERROR, DesErrorBodyModel("SERVER_ERROR", "Internal server error")))
      }
    }

  }

}
