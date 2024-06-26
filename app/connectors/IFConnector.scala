/*
 * Copyright 2024 HM Revenue & Customs
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

import com.typesafe.config.ConfigFactory
import config.AppConfig
import uk.gov.hmrc.http.HeaderCarrier.Config
import uk.gov.hmrc.http.{Authorization, HeaderCarrier}
import utils.HeaderCarrierSyntax.HeaderCarrierOps

import java.net.URL

trait IFConnector {

  val appConfig: AppConfig

  val GET_EXPENSES_BEFORE_23_24 = "1668"
  val GET_EXPENSES_AFTER_23_24 = "1866"

  val CREATE_UPDATE_EXPENSES_BEFORE_23_24 = "1669"
  val DELETE_OVERRIDE_EXPENSES_AFTER_23_24 = "1860"

  lazy val baseUrl: String = appConfig.integrationFrameworkBaseUrl

  val headerCarrierConfig: Config = HeaderCarrier.Config.fromConfig(ConfigFactory.load())

  private[connectors] def integrationFrameworkHeaderCarrier(url: URL, apiNumber: String)(implicit hc: HeaderCarrier): HeaderCarrier = {
    val isInternalHost = headerCarrierConfig.internalHostPatterns.exists(_.pattern.matcher(url.getHost).matches())

    val hcWithAuth = hc.copy(authorization = Some(Authorization(s"Bearer ${appConfig.integrationFrameworkAuthorisationToken(apiNumber)}")))

    if (isInternalHost) {
      hcWithAuth.withExtraHeaders("Environment" -> appConfig.integrationFrameworkEnvironment)
    } else {
      hcWithAuth.withExtraHeaders(("Environment" -> appConfig.integrationFrameworkEnvironment) +: hcWithAuth.toExplicitHeaders: _*)
    }
  }

}
