package org.broadinstitute.dsde.vault

import akka.actor.ActorLogging
import com.gettyimages.spray.swagger.SwaggerHttpService
import com.wordnik.swagger.model.ApiInfo
import org.broadinstitute.dsde.vault.services.HelloWorldService
import spray.routing.HttpServiceActor

import scala.reflect.runtime.universe._


//the actor which will accept request and distribute to other actors/objects
class VaultServiceActor extends HttpServiceActor with ActorLogging {

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  override def actorRefFactory = context

  val helloWorld = new HelloWorldService {
    def actorRefFactory = context
  }

  // this actor runs all routes
  def receive = runRoute(helloWorld.helloRoute ~ swaggerService.routes ~
    get {
      pathPrefix("swagger") {
        pathEndOrSingleSlash { getFromResource("swagger/index.html") }
      } ~ getFromResourceDirectory("swagger")
    })

  val swaggerService = new SwaggerHttpService {
    // All documented API services must be added to these API types
    override def apiTypes = Seq(typeOf[HelloWorldService])
    override def apiVersion = "2.0"
    override def baseUrl = "/"
    // let swagger-ui determine the host and port
    override def docsPath = "api-docs"
    override def actorRefFactory = context
    override def apiInfo = Some(new ApiInfo("Vault API", "Vault API services using spray and spray-swagger.", "http://www.github.com/broadinstitute/vault-api", "vault@broadinstitute.org", "Apache V2", "http://www.apache.org/licenses/LICENSE-2.0"))
  }

}
