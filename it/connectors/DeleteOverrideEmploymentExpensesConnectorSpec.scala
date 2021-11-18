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

package connectors

import com.github.tomakehurst.wiremock.http.HttpHeader
import config.{AppConfig, BackendAppConfig}
import helpers.WiremockSpec
import models.{DesErrorBodyModel, DesErrorModel}
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import play.api.http.Status._
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames, HttpClient, SessionId}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.DESTaxYearHelper.desTaxYearConverter

class DeleteOverrideEmploymentExpensesConnectorSpec extends PlaySpec with WiremockSpec {


  lazy val connector: DeleteOverrideEmploymentExpensesConnector = app.injector.instanceOf[DeleteOverrideEmploymentExpensesConnector]
  lazy val httpClient: HttpClient = app.injector.instanceOf[HttpClient]

  def appConfig(desHost: String): AppConfig = new BackendAppConfig(app.injector.instanceOf[Configuration], app.injector.instanceOf[ServicesConfig]) {
    override val desBaseUrl: String = s"http://$desHost:$wireMockPort"
  }

  val nino = "123456789"
  val taxYear = 2021


  "DeleteOverrideEmploymentExpensesConnector" should {

    val appConfigWithInternalHost = appConfig("localhost")
    val appConfigWithExternalHost = appConfig("127.0.0.1")

    val url = s"/income-tax/expenses/employments/$nino/${desTaxYearConverter(taxYear)}"

    "include internal headers" when {

      val headersSentToDes = Seq(
        new HttpHeader(HeaderNames.authorisation, "Bearer secret"),
        new HttpHeader(HeaderNames.xSessionId, "sessionIdValue")
      )

      "the host for DES is internal" in {

        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))
        val connector = new DeleteOverrideEmploymentExpensesConnector(httpClient, appConfigWithInternalHost)

        stubDeleteWithoutResponseBody(url, NO_CONTENT, headersSentToDes)

        val result = await(connector.deleteOverrideEmploymentExpenses(nino, taxYear)(hc))

        result mustBe Right(())
      }

      "the host for DES is external" in {

        implicit val hc:HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))
        val connector = new DeleteOverrideEmploymentExpensesConnector(httpClient, appConfigWithExternalHost)

        stubDeleteWithoutResponseBody(url, NO_CONTENT, headersSentToDes)

        val result = await(connector.deleteOverrideEmploymentExpenses(nino, taxYear)(hc))

        result mustBe Right(())
      }
    }

    "handle error" when {

      val desErrorBodyModel = DesErrorBodyModel("DES_CODE", "DES_REASON")

      Seq(INTERNAL_SERVER_ERROR, SERVICE_UNAVAILABLE, NOT_FOUND, BAD_REQUEST).foreach { status =>

        s"Des returns $status" in {
          val desError = DesErrorModel(status, desErrorBodyModel)
          implicit val hc: HeaderCarrier = HeaderCarrier()

          stubDeleteWithResponseBody(url, status, desError.toJson.toString())

          val result = await(connector.deleteOverrideEmploymentExpenses(nino, taxYear)(hc))

          result mustBe Left(desError)
        }
      }

      "DES returns an unexpected error code - 502 BadGateway" in {
        val desError = DesErrorModel(INTERNAL_SERVER_ERROR, desErrorBodyModel)
        implicit val hc: HeaderCarrier = HeaderCarrier()

        stubDeleteWithResponseBody(url, BAD_GATEWAY, desError.toJson.toString())

        val result = await(connector.deleteOverrideEmploymentExpenses(nino, taxYear)(hc))

        result mustBe Left(desError)
      }

    }


  }
}
