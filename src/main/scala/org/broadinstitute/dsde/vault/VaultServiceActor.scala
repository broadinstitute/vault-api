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

  val ingest = new uBAM.IngestService with ActorRefFactoryContext
  val describe = new uBAM.DescribeService with ActorRefFactoryContext
  val redirect = new uBAM.RedirectService with ActorRefFactoryContext

  // this actor runs all routes
  def receive = runRoute(ingest.routes ~ describe.routes ~ redirect.routes ~ swaggerService.routes ~
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
      typeOf[uBAM.RedirectService])

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
