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
import helpers.WiremockSpec
import models._
import play.api.Configuration
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames, HttpClient, SessionId}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.DESTaxYearHelper.desTaxYearConverter

class CreateOrAmendEmploymentExpensesConnectorSpec extends WiremockSpec {

  lazy val connector: CreateOrAmendEmploymentExpensesConnector = app.injector.instanceOf[CreateOrAmendEmploymentExpensesConnector]

  lazy val httpClient: HttpClient = app.injector.instanceOf[HttpClient]

  def appConfig(integrationFrameworkHost: String): AppConfig =
    new BackendAppConfig(app.injector.instanceOf[Configuration], app.injector.instanceOf[ServicesConfig]) {
      override val integrationFrameworkBaseUrl: String = s"http://$integrationFrameworkHost:$wireMockPort"
    }

  val nino: String = "123456789"
  val taxYear: Int = 1999
  val view: String = "latest"

  ".CreateOrAmendEmploymentExpensesConnector" should {
    val ignoreExpenses = IgnoreExpenses(true)
    val expenses = Expenses(ExpensesType(Some(0), Some(0), Some(0), Some(0), Some(0), Some(0), Some(0), Some(0)))

    val ignoreExpensesRequestBody = Json.toJson(ignoreExpenses)
    val expensesRequestBody = Json.toJson(expenses)

    val stubUrl = s"/income-tax/expenses/employments/$nino/${desTaxYearConverter(taxYear)}"

    "include internal headers" when {
      val headersSentToIF = Seq(
        new HttpHeader(HeaderNames.authorisation, "Bearer secret"),
        new HttpHeader(HeaderNames.xSessionId, "sessionIdValue")
      )

      val internalHost = "localhost"
      val externalHost = "127.0.0.1"

      "the host for Integration Framework is 'Internal'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))
        val connector = new CreateOrAmendEmploymentExpensesConnector(httpClient, appConfig(internalHost))

        stubPutWithoutResponseBody(stubUrl, expensesRequestBody.toString(), NO_CONTENT, headersSentToIF)

        val result = await(connector.createOrAmendEmploymentExpenses(nino, taxYear, expenses)(hc))

        result mustBe Right(())
      }

      "the host for Integration Framework is 'External'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))
        val connector = new CreateOrAmendEmploymentExpensesConnector(httpClient, appConfig(externalHost))

        stubPutWithoutResponseBody(stubUrl, expensesRequestBody.toString(), NO_CONTENT, headersSentToIF)

        val result = await(connector.createOrAmendEmploymentExpenses(nino, taxYear, expenses)(hc))

        result mustBe Right(())
      }
    }

    "return a Right(())" when {
      "request body is Expenses model" in {
        stubPutWithoutResponseBody(stubUrl, expensesRequestBody.toString(), NO_CONTENT)

        implicit val hc: HeaderCarrier = HeaderCarrier()
        val result = await(connector.createOrAmendEmploymentExpenses(nino, taxYear, expenses)(hc))

        result mustBe Right(())
      }

      "request body is IgnoreExpenses model" in {
        stubPutWithoutResponseBody(stubUrl, ignoreExpensesRequestBody.toString(), NO_CONTENT)

        implicit val hc: HeaderCarrier = HeaderCarrier()
        val result = await(connector.createOrAmendEmploymentExpenses(nino, taxYear, ignoreExpenses)(hc))

        result mustBe Right(())
      }

      "response is NOT_FOUND" in {
        stubPutWithoutResponseBody(stubUrl, ignoreExpensesRequestBody.toString(), NOT_FOUND)

        implicit val hc: HeaderCarrier = HeaderCarrier()
        val result = await(connector.createOrAmendEmploymentExpenses(nino, taxYear, ignoreExpenses)(hc))

        result mustBe Right(())
      }
    }

    "return Left(error)" when {
      Seq(BAD_REQUEST, UNPROCESSABLE_ENTITY, INTERNAL_SERVER_ERROR, SERVICE_UNAVAILABLE).foreach { httpErrorStatus =>

        s"Integration Framework returns $httpErrorStatus response that has a parsable error body" in {
          val responseBody = Json.obj(
            "code" -> "SOME_DES_ERROR_CODE",
            "reason" -> "SOME_DES_ERROR_REASON"
          )
          val expectedResult = DesErrorModel(httpErrorStatus, DesErrorBodyModel("SOME_DES_ERROR_CODE", "SOME_DES_ERROR_REASON"))

          stubPutWithResponseBody(stubUrl, expensesRequestBody.toString(), responseBody.toString(), httpErrorStatus)
          implicit val hc: HeaderCarrier = HeaderCarrier()
          val result = await(connector.createOrAmendEmploymentExpenses(nino, taxYear, expenses)(hc))

          result mustBe Left(expectedResult)
        }

        s"Integration Framework returns $httpErrorStatus response that does not have a parsable error body" in {
          val expectedResult = DesErrorModel(httpErrorStatus, DesErrorBodyModel.parsingError)

          stubPutWithResponseBody(stubUrl, expensesRequestBody.toString(), "UNEXPECTED RESPONSE BODY", httpErrorStatus)
          implicit val hc: HeaderCarrier = HeaderCarrier()
          val result = await(connector.createOrAmendEmploymentExpenses(nino, taxYear, expenses)(hc))

          result mustBe Left(expectedResult)
        }
      }

      "Integration Framework returns an unexpected http response that is parsable" in {
        val responseBody = Json.obj(
          "code" -> "BAD_GATEWAY",
          "reason" -> "bad gateway"
        )
        val expectedResult = DesErrorModel(INTERNAL_SERVER_ERROR, DesErrorBodyModel("BAD_GATEWAY", "bad gateway"))

        stubPutWithResponseBody(stubUrl, expensesRequestBody.toString(), responseBody.toString(), BAD_GATEWAY)
        implicit val hc: HeaderCarrier = HeaderCarrier()
        val result = await(connector.createOrAmendEmploymentExpenses(nino, taxYear, expenses)(hc))

        result mustBe Left(expectedResult)
      }

      "Integration Framework returns an unexpected http response that is not parsable" in {
        val expectedResult = DesErrorModel(INTERNAL_SERVER_ERROR, DesErrorBodyModel.parsingError)

        stubPutWithResponseBody(stubUrl, expensesRequestBody.toString(), "Bad Gateway", BAD_GATEWAY)
        implicit val hc: HeaderCarrier = HeaderCarrier()
        val result = await(connector.createOrAmendEmploymentExpenses(nino, taxYear, expenses)(hc))

        result mustBe Left(expectedResult)
      }

    }
  }
}
