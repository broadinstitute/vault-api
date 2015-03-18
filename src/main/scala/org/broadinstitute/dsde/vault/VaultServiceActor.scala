package org.broadinstitute.dsde.vault

import akka.actor.ActorLogging
import com.gettyimages.spray.swagger.SwaggerHttpService
import com.wordnik.swagger.model.ApiInfo
import org.broadinstitute.dsde.vault.services._
import spray.routing.HttpServiceActor

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

  // this actor runs all routes
  def receive = runRoute(uBAMIngest.routes ~ uBAMDescribe.routes ~ uBAMRedirect.routes ~
    analysisIngest.routes ~ analysisDescribe.routes ~ analysisUpdate.routes ~
    swaggerService.routes ~
    get {
      pathPrefix("swagger") {
        pathEndOrSingleSlash { getFromResource("swagger/index.html") }
      } ~ getFromResourceDirectory("swagger")
    })

  val swaggerService = new SwaggerHttpService {
    // All documented API services must be added to these API types
    override def apiTypes = Seq(
      typeOf[uBAM.IngestService],
      typeOf[uBAM.DescribeService],
      typeOf[uBAM.RedirectService],
      typeOf[analysis.IngestService],
      typeOf[analysis.DescribeService],
      typeOf[analysis.UpdateService])

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

}
