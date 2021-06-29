
package controllers

import controllers.predicates.AuthorisedAction
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import services.DeleteOverrideEmploymentExpensesService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class DeleteOverrideEmploymentExpensesController @Inject()(service: DeleteOverrideEmploymentExpensesService,
                                                           authorisedAction: AuthorisedAction,
                                                           cc: ControllerComponents)
                                                          (implicit ec: ExecutionContext) extends BackendController(cc) {

  def deleteOverrideEmploymentExpenses(nino: String, taxYear: Int): Action[AnyContent] = authorisedAction.async { implicit user =>
    service.deleteOverrideEmploymentExpenses(nino, taxYear).map {
      case Right(_) => NoContent
      case Left(errorModel) => Status(errorModel.status)(Json.toJson(errorModel.toJson))
    }
  }

}
