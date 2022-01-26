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

package connectors

import com.github.tomakehurst.wiremock.http.HttpHeader
import config.{AppConfig, BackendAppConfig}
import connectors.GetEmploymentExpensesConnectorSpec.expectedResponseBody
import helpers.WiremockSpec
import models.{DesErrorBodyModel, DesErrorModel, GetEmploymentExpensesModel}
import play.api.Configuration
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames, HttpClient, SessionId}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.DESTaxYearHelper.desTaxYearConverter

class GetEmploymentExpensesConnectorSpec extends WiremockSpec {

  lazy val connector: GetEmploymentExpensesConnector = app.injector.instanceOf[GetEmploymentExpensesConnector]

  lazy val httpClient: HttpClient = app.injector.instanceOf[HttpClient]
  def appConfig(desHost: String): AppConfig = new BackendAppConfig(app.injector.instanceOf[Configuration], app.injector.instanceOf[ServicesConfig]) {
    override val desBaseUrl: String = s"http://$desHost:$wireMockPort"
  }

  val nino: String = "123456789"
  val taxYear: Int = 1999
  val view: String = "latest"

  ".GetEmploymentExpensesConnector" should {

    "include internal headers" when {
      val headersSentToDes = Seq(
        new HttpHeader(HeaderNames.authorisation, "Bearer secret"),
        new HttpHeader(HeaderNames.xSessionId, "sessionIdValue")
      )

      val internalHost = "localhost"
      val externalHost = "127.0.0.1"

      "the host for DES is 'Internal'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))
        val connector = new GetEmploymentExpensesConnector(httpClient, appConfig(internalHost))
        val expectedResult = Json.parse(expectedResponseBody).as[GetEmploymentExpensesModel]

        stubGetWithResponseBody(s"/income-tax/expenses/employments/$nino/${desTaxYearConverter(taxYear)}\\?view=$view",
          OK, expectedResponseBody, headersSentToDes)

        val result = await(connector.getEmploymentExpenses(nino, taxYear, view)(hc))

        result mustBe Right(expectedResult)
      }

      "the host for DES is 'External'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))
        val connector = new GetEmploymentExpensesConnector(httpClient, appConfig(externalHost))
        val expectedResult = Json.parse(expectedResponseBody).as[GetEmploymentExpensesModel]

        stubGetWithResponseBody(s"/income-tax/expenses/employments/$nino/${desTaxYearConverter(taxYear)}\\?view=$view",
          OK, expectedResponseBody, headersSentToDes)

        val result = await(connector.getEmploymentExpenses(nino, taxYear, view)(hc))

        result mustBe Right(expectedResult)
      }
    }

    "return a GetEmploymentExpensesModel" when {
      "only nino and taxYear are present" in {
        val expectedResult = Json.parse(expectedResponseBody).as[GetEmploymentExpensesModel]
        stubGetWithResponseBody(s"/income-tax/expenses/employments/$nino/${desTaxYearConverter(taxYear)}\\?view=$view", OK, expectedResponseBody)

        implicit val hc: HeaderCarrier = HeaderCarrier()
        val result = await(connector.getEmploymentExpenses(nino, taxYear, view)(hc)).right.get

        result mustBe expectedResult
      }

      "nino, taxYear and sourceType are present" in {
        val expectedResult = Json.parse(expectedResponseBody).as[GetEmploymentExpensesModel]
        stubGetWithResponseBody(s"/income-tax/expenses/employments/$nino/${desTaxYearConverter(taxYear)}\\?view=$view", OK, expectedResponseBody)

        implicit val hc: HeaderCarrier = HeaderCarrier()
        val result = await(connector.getEmploymentExpenses(nino, taxYear, view)(hc)).right.get

        result mustBe expectedResult
      }
    }

    "return a Parsing error INTERNAL_SERVER_ERROR response" in {
      val invalidJson = Json.obj(
        "source" -> true
      )

      val expectedResult = DesErrorModel(INTERNAL_SERVER_ERROR, DesErrorBodyModel.parsingError)

      stubGetWithResponseBody(s"/income-tax/expenses/employments/$nino/${desTaxYearConverter(taxYear)}\\?view=$view", OK, invalidJson.toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getEmploymentExpenses(nino, taxYear, view)(hc))

      result mustBe Left(expectedResult)
    }

    "return a NO_CONTENT" in {
      val expectedResult = DesErrorModel(INTERNAL_SERVER_ERROR, DesErrorBodyModel.parsingError)

      stubGetWithResponseBody(s"/income-tax/expenses/employments/$nino/${desTaxYearConverter(taxYear)}\\?view=$view", NO_CONTENT, "{}")
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getEmploymentExpenses(nino, taxYear, view)(hc))

      result mustBe Left(expectedResult)
    }

    "return a Bad Request" in {
      val responseBody = Json.obj(
        "code" -> "INVALID_NINO",
        "reason" -> "Nino is invalid"
      )
      val expectedResult = DesErrorModel(BAD_REQUEST, DesErrorBodyModel("INVALID_NINO", "Nino is invalid"))

      stubGetWithResponseBody(s"/income-tax/expenses/employments/$nino/${desTaxYearConverter(taxYear)}\\?view=$view", BAD_REQUEST, responseBody.toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getEmploymentExpenses(nino, taxYear, view)(hc))

      result mustBe Left(expectedResult)
    }

    "return a Not found" in {
      val responseBody = Json.obj(
        "code" -> "NOT_FOUND_INCOME_SOURCE",
        "reason" -> "Can't find income source"
      )
      val expectedResult = DesErrorModel(NOT_FOUND, DesErrorBodyModel("NOT_FOUND_INCOME_SOURCE", "Can't find income source"))

      stubGetWithResponseBody(s"/income-tax/expenses/employments/$nino/${desTaxYearConverter(taxYear)}\\?view=$view", NOT_FOUND, responseBody.toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getEmploymentExpenses(nino, taxYear, view)(hc))

      result mustBe Left(expectedResult)
    }

    "return an Internal server error" in {
      val responseBody = Json.obj(
        "code" -> "SERVER_ERROR",
        "reason" -> "Internal server error"
      )
      val expectedResult = DesErrorModel(INTERNAL_SERVER_ERROR, DesErrorBodyModel("SERVER_ERROR", "Internal server error"))

      stubGetWithResponseBody(s"/income-tax/expenses/employments/$nino/${desTaxYearConverter(taxYear)}\\?view=$view", INTERNAL_SERVER_ERROR, responseBody.toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getEmploymentExpenses(nino, taxYear, view)(hc))

      result mustBe Left(expectedResult)
    }

    "return a Service Unavailable" in {
      val responseBody = Json.obj(
        "code" -> "SERVICE_UNAVAILABLE",
        "reason" -> "Service is unavailable"
      )
      val expectedResult = DesErrorModel(SERVICE_UNAVAILABLE, DesErrorBodyModel("SERVICE_UNAVAILABLE", "Service is unavailable"))

      stubGetWithResponseBody(s"/income-tax/expenses/employments/$nino/${desTaxYearConverter(taxYear)}\\?view=$view", SERVICE_UNAVAILABLE, responseBody.toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getEmploymentExpenses(nino, taxYear, view)(hc))

      result mustBe Left(expectedResult)
    }

    "return an Internal Server Error when DES throws an unexpected result" in {
      val expectedResult = DesErrorModel(INTERNAL_SERVER_ERROR, DesErrorBodyModel.parsingError)

      stubGetWithoutResponseBody(s"/income-tax/expenses/employments/$nino/${desTaxYearConverter(taxYear)}\\?view=$view", NO_CONTENT)
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getEmploymentExpenses(nino, taxYear, view)(hc))

      result mustBe Left(expectedResult)
    }

    "return an Internal Server Error when DES throws an unexpected result that is parsable" in {
      val responseBody = Json.obj(
        "code" -> "SERVICE_UNAVAILABLE",
        "reason" -> "Service is unavailable"
      )
      val expectedResult = DesErrorModel(INTERNAL_SERVER_ERROR,  DesErrorBodyModel("SERVICE_UNAVAILABLE", "Service is unavailable"))

      stubGetWithResponseBody(s"/income-tax/expenses/employments/$nino/${desTaxYearConverter(taxYear)}\\?view=$view", CONFLICT, responseBody.toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getEmploymentExpenses(nino, taxYear, view)(hc))

      result mustBe Left(expectedResult)
    }

    "return an Internal Server Error when DES throws an unexpected result that isn't parsable" in {
      val responseBody = Json.obj(
        "code" -> "SERVICE_UNAVAILABLE"
      )
      val expectedResult = DesErrorModel(INTERNAL_SERVER_ERROR,  DesErrorBodyModel.parsingError)

      stubGetWithResponseBody(s"/income-tax/expenses/employments/$nino/${desTaxYearConverter(taxYear)}\\?view=$view", CONFLICT, responseBody.toString())
      implicit val hc: HeaderCarrier = HeaderCarrier()
      val result = await(connector.getEmploymentExpenses(nino, taxYear, view)(hc))

      result mustBe Left(expectedResult)
    }
  }
}

object GetEmploymentExpensesConnectorSpec {
  val expectedResponseBody: String =
    """
      |{
      |	"submittedOn": "2020-12-12T12:12:12Z",
      |	"dateIgnored": "2020-12-11T12:12:12Z",
      |	"source": "CUSTOMER",
      |	"totalExpenses": 123.12,
      |	"expenses": {
      |		"businessTravelCosts": 123.12,
      |		"jobExpenses": 123.12,
      |		"flatRateJobExpenses": 123.12,
      |		"professionalSubscriptions": 123.12,
      |		"hotelAndMealExpenses": 123.12,
      |		"otherAndCapitalAllowances": 123.12,
      |		"vehicleExpenses": 123.12,
      |		"mileageAllowanceRelief": 123.12
      |	}
      |}
      |""".stripMargin
}
