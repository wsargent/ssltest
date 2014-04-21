package controllers

import play.api.mvc._
import play.api.libs.ws.WS

object Application extends Controller {
  import play.api.Play.current
  import play.api.libs.concurrent.Execution.Implicits._

  def index = Action { implicit request =>
    //    WS.url("https://www.example.com/" + request.path).get().map { response =>
    //      val body = response.body
    //      Ok(body)
    //    }
    Ok(views.html.index("message"))
  }

}
