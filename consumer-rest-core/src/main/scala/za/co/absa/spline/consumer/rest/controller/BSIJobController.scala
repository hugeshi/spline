package za.co.absa.spline.consumer.rest.controller

import io.swagger.annotations.{Api, ApiOperation, ApiParam}
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.{GetMapping, RequestParam, RestController}
import za.co.absa.spline.consumer.service.model.sparkjob.{OutputTable, SparkJobIOs}
import za.co.absa.spline.consumer.service.repo.SparkJobIOsRepository

import scala.concurrent.Future

@RestController
@Api(tags = Array("bsi"))
class BSIJobController @Autowired()(val sparkJobIOsRepository: SparkJobIOsRepository) {
  val logger = LoggerFactory.getLogger(classOf[BSIBrokerController])

  import scala.concurrent.ExecutionContext.Implicits._

  @GetMapping(Array("job-ios"))
  @ApiOperation(value = "Get spark input and output path ")
  def jobIos(
              @ApiParam(value = "Spark Application Id")
              @RequestParam("appId") appId: String
            ): Future[SparkJobIOs] = {
    sparkJobIOsRepository.findJobIOs(appId)
  }

  @GetMapping(Array("output-tables"))
  @ApiOperation(value = "Get Job output tables")
  def jobOutputTables(
                       @ApiParam(value = "Spark Application Id")
                       @RequestParam("appId") appId: String
                     ): Future[Array[OutputTable]] = {
    sparkJobIOsRepository.findJobOutputTables(appId)
  }


  @GetMapping(Array("model-ios"))
  @ApiOperation(value = "Get spark input and output path ")
  def modelIOs(
                @ApiParam(value = "BSI model name")
                @RequestParam("modelName") modelName: String
              ): Future[SparkJobIOs] = {
    //TODO get the appId by modelName
    val appId = null
    sparkJobIOsRepository.findJobIOs(appId)
  }

  @GetMapping(Array("variable-model"))
  @ApiOperation(value = "Get model output variable and model tables")
  def variableAndModelTable(
                             @ApiParam(value = "BSI model name")
                             @RequestParam("modelName") modelName: String
                           ): Future[Array[OutputTable]] = {
    //TODO get the appId by modelName
    val appId = null
    sparkJobIOsRepository.findJobOutputTables(appId)

  }

}