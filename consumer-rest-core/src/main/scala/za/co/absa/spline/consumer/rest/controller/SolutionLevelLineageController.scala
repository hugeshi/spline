package za.co.absa.spline.consumer.rest.controller


import io.swagger.annotations.{Api, ApiOperation, ApiParam}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.{GetMapping, RequestParam, RestController}
import za.co.absa.spline.consumer.service.repo.{BSISolutionRepository, SolutionLineageRepository}

import scala.collection.mutable
import scala.concurrent.Future

@RestController
@Api(tags = Array("lineage"))
class SolutionLevelLineageController @Autowired()(val repo: SolutionLineageRepository, val bsiRepo: BSISolutionRepository) {

  import scala.concurrent.ExecutionContext.Implicits._

  @GetMapping(Array("attribute-lineage"))
  @ApiOperation(
    value = "Get attributes name with input data source that depends on attribute with provided name")
  def attributeLineageAndImpact(
                                 @ApiParam(value = "Model Name")
                                 @RequestParam("modelName") modelName: String,
                                 @ApiParam(value = "Variable Name")
                                 @RequestParam("variableName") variableName: String
                               ): Future[mutable.Map[String, List[String]]] = {
    bsiRepo.findAppNameBySolutionName(modelName).map(x => repo.findVariableDependencies(variableName, x.appId))
  }
}
