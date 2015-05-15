package org.broadinstitute.dsde.vault

import akka.actor.{ActorRefFactory, ActorLogging}
import com.gettyimages.spray.swagger.SwaggerHttpService
import com.wordnik.swagger.model.ApiInfo
import org.broadinstitute.dsde.vault.common.directives.OpenAMDirectives._
import org.broadinstitute.dsde.vault.services._
import spray.http.StatusCodes._
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

  val uBAMIngest = new uBAM.UBamIngestService with ActorRefFactoryContext
  val uBAMDescribe = new uBAM.UBamDescribeService with ActorRefFactoryContext
  val uBAMDescribeList = new uBAM.UBamDescribeListService with ActorRefFactoryContext
  val uBAMRedirect = new uBAM.UBamRedirectService with ActorRefFactoryContext

  val analysisIngest = new analysis.AnalysisIngestService with ActorRefFactoryContext
  val analysisDescribe = new analysis.AnalysisDescribeService with ActorRefFactoryContext
  val analysisUpdate = new analysis.AnalysisUpdateService with ActorRefFactoryContext
  val analysisRedirect = new analysis.AnalysisRedirectService with ActorRefFactoryContext

  val uBamCollectionsIngest = new uBAMCollection.UBamCollectionIngestService with ActorRefFactoryContext
  val uBamCollectionsDescribe = new uBAMCollection.UBamCollectionDescribeService with ActorRefFactoryContext

  val lookupService = new lookup.LookupService with ActorRefFactoryContext

  private implicit val ec = context.dispatcher

  // this actor runs all routes
  def receive = runRoute(
    swaggerService.routes ~ swaggerUiService ~
      logOpenAMRequest() {
        uBAMIngest.ubiRoute ~ uBAMDescribe.ubdRoute ~ uBAMRedirect.ubrRoute ~uBAMDescribeList.routes ~
          uBAMIngest.ubiRoute ~ uBAMDescribe.ubdRoute ~ uBAMRedirect.ubrRoute ~
          analysisIngest.aiRoute ~ analysisDescribe.adRoute ~ analysisUpdate.auRoute ~ analysisRedirect.arRoute ~
          uBamCollectionsIngest.ubciRoute ~ uBamCollectionsDescribe.ubcdRoute ~ lookupService.lRoute
      }
  )

  val swaggerService = new SwaggerHttpService {
    // All documented API services must be added to these API types
    override def apiTypes = Seq(
      typeOf[uBAM.UBamIngestService],
      typeOf[uBAM.UBamDescribeService],
      typeOf[uBAM.UBamDescribeListService],
      typeOf[uBAM.UBamRedirectService],
      typeOf[uBAMCollection.UBamCollectionIngestService],
      typeOf[uBAMCollection.UBamCollectionDescribeService],
      typeOf[analysis.AnalysisIngestService],
      typeOf[analysis.AnalysisDescribeService],
      typeOf[analysis.AnalysisUpdateService],
      typeOf[analysis.AnalysisRedirectService],
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
          // the base context path may be different in various environments
          val dynamicContext = VaultConfig.SwaggerConfig.baseUrl
          p.redirect(dynamicContext + "swagger/index.html?url=" + dynamicContext + "api-docs", TemporaryRedirect)
        } ~
          pathPrefix("swagger/index.html") {
            getFromResource("META-INF/resources/webjars/swagger-ui/2.1.8-M1/index.html")
          } ~
          getFromResourceDirectory("META-INF/resources/webjars/swagger-ui/2.1.8-M1")
      }
    }
  }

}
