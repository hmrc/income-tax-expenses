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

import play.core.PlayVersion.current
import sbt.*

object AppDependencies {

  private val bootstrapPlayVersion = "10.3.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                   %% "bootstrap-backend-play-30"  % bootstrapPlayVersion,
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                   %% "bootstrap-test-play-30"     % bootstrapPlayVersion    % Test,
    "org.scalatest"                 %% "scalatest"                  % "3.2.19"                % Test,
    "org.playframework"             %% "play-test"                  % current                 % Test,
    "org.scalamock"                 %% "scalamock"                  % "7.5.0"                 % Test,
    "com.vladsch.flexmark"          %  "flexmark-all"               % "0.64.8"                % Test,
    "org.scalatestplus.play"        %% "scalatestplus-play"         % "7.0.2"                 % Test,
    "com.github.tomakehurst"        %  "wiremock-jre8-standalone"   % "3.0.1"                 % Test
  )
}
