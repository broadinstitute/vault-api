package org.broadinstitute.dsde.vault

import akka.actor.ActorLogging
import com.gettyimages.spray.swagger.SwaggerHttpService
import com.wordnik.swagger.model.ApiInfo
import org.broadinstitute.dsde.vault.services._
import spray.routing.HttpServiceActor
import spray.http.StatusCodes._

import scala.reflect.runtime.universe._

//the actor which will accept request and distribute to other actors/objects
class VaultServiceActor extends HttpServiceActor with ActorLogging {

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  override def actorRefFactory = context

  trait ActorRefFactoryContext {
    def actorRefFactory = context
  }

  val uBAMIngest = new uBAM.IngestService with ActorRefFactoryContext
  val uBAMDescribe = new uBAM.DescribeService with ActorRefFactoryContext
  val uBAMRedirect = new uBAM.RedirectService with ActorRefFactoryContext

  val analysisIngest = new analysis.IngestService with ActorRefFactoryContext
  val analysisDescribe = new analysis.DescribeService with ActorRefFactoryContext
  val analysisUpdate = new analysis.UpdateService with ActorRefFactoryContext

  val lookupService = new lookup.LookupService with ActorRefFactoryContext

  // this actor runs all routes
  def receive = runRoute(
    uBAMIngest.routes ~ uBAMDescribe.routes ~ uBAMRedirect.routes ~
    analysisIngest.routes ~ analysisDescribe.routes ~ analysisUpdate.routes ~
    lookupService.routes ~
    swaggerService.routes ~ swaggerUiService
    )

  val swaggerService = new SwaggerHttpService {
    // All documented API services must be added to these API types
    override def apiTypes = Seq(
      typeOf[uBAM.IngestService],
      typeOf[uBAM.DescribeService],
      typeOf[uBAM.RedirectService],
      typeOf[analysis.IngestService],
      typeOf[analysis.DescribeService],
      typeOf[analysis.UpdateService],
      typeOf[lookup.LookupService])

    override def apiVersion = VaultConfig.SwaggerConfig.apiVersion
    override def baseUrl = VaultConfig.SwaggerConfig.baseUrl
    override def docsPath = VaultConfig.SwaggerConfig.apiDocs
    override def actorRefFactory = context
    override def apiInfo = Some(
      new ApiInfo(
        VaultConfig.SwaggerConfig.info,
        VaultConfig.SwaggerConfig.description,
        VaultConfig.SwaggerConfig.termsOfServiceUrl,
        VaultConfig.SwaggerConfig.contact,
        VaultConfig.SwaggerConfig.license,
        VaultConfig.SwaggerConfig.licenseUrl)
    )
  }

  val swaggerUiService = {
    get {
      pathPrefix("swagger") {
        // if the user just hits "swagger", redirect to the index page with our api docs specified on the url
        pathEndOrSingleSlash { p =>
          // dynamically calculate the context path, which may be different in various environments
          val path = p.request.uri.path.toString
          val dynamicContext = path.substring(0, path.indexOf("swagger"))
          p.redirect("/swagger/index.html?url=" + dynamicContext + "api-docs", TemporaryRedirect)
        } ~
          pathPrefix("swagger/index.html") {
            getFromResource("META-INF/resources/webjars/swagger-ui/2.1.8-M1/index.html")
          } ~
          getFromResourceDirectory("META-INF/resources/webjars/swagger-ui/2.1.8-M1")
      }
    }
  }

}
