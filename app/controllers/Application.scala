package controllers

import play.api.mvc._
import play.api.libs.ws.WS

object Application extends Controller {
  import play.api.Play.current
  import play.api.libs.concurrent.Execution.Implicits._

  def index = Action.async {
    WS.url("https://www.howsmyssl.com/a/check").get().map { response =>
      val json = response.json
      //val rating = (json \ "rating").as[String]
      Ok(json)
    }
  }

}
