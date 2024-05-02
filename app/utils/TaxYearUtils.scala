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

package utils

object TaxYearUtils {

  def toTaxYearParam(taxYear: Int): String = {
    if (taxYear - 1 >= 2023) {
      s"${(taxYear - 1).toString takeRight 2}-${taxYear.toString takeRight 2}"
    } else {
      s"${taxYear - 1}-${taxYear.toString takeRight 2}"
    }
  }

  def isYearAfter2324(taxYear: Int): Boolean = {
    taxYear >= 2024
  }

}
