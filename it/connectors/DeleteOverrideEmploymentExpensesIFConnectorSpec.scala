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

package connectors

import com.github.tomakehurst.wiremock.http.HttpHeader
import config.{AppConfig, BackendAppConfig}
import helpers.WiremockSpec
import models._
import play.api.Configuration
import play.api.http.Status._
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames, HttpClient, SessionId}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

class DeleteOverrideEmploymentExpensesIFConnectorSpec extends WiremockSpec {

  private lazy val connector: DeleteOverrideEmploymentExpensesIFConnector = app.injector.instanceOf[DeleteOverrideEmploymentExpensesIFConnector]
  private lazy val httpClient: HttpClient = app.injector.instanceOf[HttpClient]

  def appConfig(integrationFrameworkHost: String): AppConfig =
    new BackendAppConfig(app.injector.instanceOf[Configuration], app.injector.instanceOf[ServicesConfig]) {
      override val integrationFrameworkBaseUrl: String = s"http://$integrationFrameworkHost:$wireMockPort"
    }

  private val nino: String = "123456789"
  private val taxYear: Int = 2024

  private implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))

  ".DeleteOverrideEmploymentExpensesIFConnector" should {
    val appConfigWithInternalHost = appConfig("localhost")
    val appConfigWithExternalHost = appConfig("127.0.0.1")
    val url = s"/income-tax/expenses/employments/23-24/$nino"

    "return a successful response including internal headers" when {
      val headers = Seq(
        new HttpHeader(HeaderNames.authorisation, "Bearer secret"),
        new HttpHeader(HeaderNames.xSessionId, "sessionIdValue")
      )

      "the host for IF is internal" in {
        val connector = new DeleteOverrideEmploymentExpensesIFConnector(httpClient, appConfigWithInternalHost)

        stubDeleteWithoutResponseBody(url, NO_CONTENT, headers)

        await(connector.deleteOverrideEmploymentExpenses(nino, taxYear)(hc)) mustBe Right(())
      }

      "the host for IF is external" in {
        val connector = new DeleteOverrideEmploymentExpensesIFConnector(httpClient, appConfigWithExternalHost)

        stubDeleteWithoutResponseBody(url, NO_CONTENT, headers)

        await(connector.deleteOverrideEmploymentExpenses(nino, taxYear)(hc)) mustBe Right(())
      }
    }

    "handle error" when {
      val errorBody = DesErrorBodyModel("CODE", "REASON")

      Seq(INTERNAL_SERVER_ERROR, SERVICE_UNAVAILABLE, NOT_FOUND, BAD_REQUEST, UNPROCESSABLE_ENTITY).foreach { status =>
        s"IF returns $status" in {
          val error = DesErrorModel(status, errorBody)

          stubDeleteWithResponseBody(url, status, error.toJson.toString())

          await(connector.deleteOverrideEmploymentExpenses(nino, taxYear)(hc)) mustBe Left(error)
        }
      }

      "IF returns an unexpected error code" in {
        val error = DesErrorModel(INTERNAL_SERVER_ERROR, errorBody)

        stubDeleteWithResponseBody(url, BAD_GATEWAY, error.toJson.toString())

        await(connector.deleteOverrideEmploymentExpenses(nino, taxYear)(hc)) mustBe Left(error)
      }
    }
  }
}
