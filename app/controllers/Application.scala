package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import scala.concurrent.future
import scala.concurrent.ExecutionContext.Implicits._
import com.codahale.jerkson.Json._
import services.TinderService
import utils.tinder.TinderApi
import utils.tinder.model._


object Application extends Controller {

  /**
   * Home page let's you select active Tinder sessions or create one.
   */
  def index = Action.async { implicit request =>
    val f = future { views.html.Application.index() }
    f.map { result => Ok(result) }
  }

  /**
   * Pulls up an active Tinder session in a dashboard.
   */
  def dashboard(xAuthToken: String) = Action.async { implicit request =>
    val f = future { TinderService.fetchSession(xAuthToken) }
    f.map { result =>
      result match {
        case Some(session) => Ok(views.html.Application.dashboard(session))
        case None => BadRequest
      }
    }
  }

  /**
   * Home page let's you select active Tinder sessions or create one.
   */
  val authForm = Form(
    mapping(
      "facebook_token" -> text,
      "facebook_id" -> text
    )(FBAuth.apply)(FBAuth.unapply)
  )
  def authenticate = Action.async { implicit request =>
    authForm.bindFromRequest.fold(
      formWithErrors => {
        val f = future { views.html.Application.index(Some("There were errors in the credentials.")) }
        f.map { result => BadRequest(result) }
      },
      fbAuth => {
        new TinderApi().authorize(fbAuth.facebook_token, fbAuth.facebook_id).map { result =>
          result match {
            case Left(error) =>
              BadRequest(views.html.Application.index(Some(error.error)))

            case Right(auth) =>
              TinderService.storeSession(auth)
              Logger.info("Logging in user "+auth.user.name)
              Redirect(routes.Application.dashboard(auth.token))
          }
        }
      }
    )
  }

  /**
   * Ends the current session
   */
  def logout(xAuthToken: String) = Action.async { implicit request =>
    val f = future { TinderService.deleteSession(xAuthToken) }
    f.map { result => Ok(views.html.Application.index()) }
  }

  /**
   * Pulls up an active Tinder session in a dashboard.
   */
  def activeSessions = Action { implicit request =>
    Ok(generate(TinderService.activeSessions))
  }

}
