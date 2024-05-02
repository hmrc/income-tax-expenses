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

package controllers

import controllers.predicates.AuthorisedAction
import models.{DesErrorBodyModel, ToRemove}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import services.DeleteOrIgnoreEmploymentExpensesService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeleteOrIgnoreEmploymentExpensesController @Inject()(service: DeleteOrIgnoreEmploymentExpensesService,
                                                           authorisedAction: AuthorisedAction,
                                                           cc: ControllerComponents)
                                                          (implicit ec: ExecutionContext) extends BackendController(cc) {

  def deleteOrIgnoreEmploymentExpenses(nino: String, toRemove: String, taxYear: Int): Action[AnyContent] = authorisedAction.async { implicit user =>
    ToRemove(toRemove).map { validToRemove =>
      service.deleteOrIgnoreEmploymentExpenses(nino, validToRemove, taxYear).map {
        case Right(_) => NoContent
        case Left(errorModel) => Status(errorModel.status)(Json.toJson(errorModel.toJson))
      }
    }.getOrElse(Future(BadRequest(Json.toJson(DesErrorBodyModel("INVALID_TO_REMOVE_PARAMETER", "toRemove parameter is not: ALL, HMRC-HELD or CUSTOMER")))))
  }

}
