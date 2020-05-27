package za.co.absa.spline.consumer.rest.controller

import io.swagger.annotations.{Api, ApiOperation}
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation._
import za.co.absa.spline.consumer.rest.model.ModelStatus
import za.co.absa.spline.consumer.rest.services.BSIModelService

@RestController
@Api(tags = Array("bsi"))
class BSIBrokerController @Autowired()(private final val bsiModelClient: BSIModelService) {
  val logger = LoggerFactory.getLogger(classOf[BSIBrokerController])

  @GetMapping(Array("bsi-model-status"))
  @ApiOperation(value = "Get all bsi models with status", notes = "Returns a list of bsi models with status")
  def bsiModelStatus(): Array[ModelStatus] = {
    val response = bsiModelClient.getBSIModelStatus()
    response.getBody
  }

  @GetMapping(Array("bsi-models"))
  @ApiOperation(value = "Get all bsi models", notes = "Returns a list of bsi models' name")
  def bsiModels(): Array[String] = {
    val response = bsiModelClient.getBSIModelStatus()
    response.getBody.map(x => x.model.modelName)
  }

}