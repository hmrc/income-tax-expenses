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

package controllers

import connectors.httpParsers.CreateOrAmendEmploymentExpensesHttpParser.CreateOrAmendEmploymentExpenseResponse
import controllers.predicates.AuthorisedAction
import models.EmploymentExpensesRequestModel
import play.api.Logging
import play.api.libs.json.{JsSuccess, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import services.CreateOrAmendEmploymentExpensesService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CreateOrAmendEmploymentExpensesController @Inject()(
                                             service: CreateOrAmendEmploymentExpensesService,
                                             auth: AuthorisedAction,
                                             cc: ControllerComponents
                                           )(implicit ec: ExecutionContext) extends BackendController(cc) with Logging {

  def createOrAmendEmploymentExpenses(nino: String, taxYear: Int): Action[AnyContent] = auth.async { implicit user =>

    user.body.asJson.map(_.validate[EmploymentExpensesRequestModel]) match {
      case Some(JsSuccess(model, _)) => responseHandler(service.createOrAmendEmploymentExpenses(nino, taxYear, model))
      case _ => Future.successful(BadRequest)
    }
  }

  def responseHandler(serviceResponse: Future[CreateOrAmendEmploymentExpenseResponse]): Future[Result] ={
    serviceResponse.map {
      case Right(_) => NoContent
      case Left(errorModel) => Status(errorModel.status)(Json.toJson(errorModel.toJson))
    }
  }
}
