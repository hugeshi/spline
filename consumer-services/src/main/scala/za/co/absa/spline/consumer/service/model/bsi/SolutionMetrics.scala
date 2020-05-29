package za.co.absa.spline.consumer.service.model.bsi

import io.swagger.annotations.{ApiModel, ApiModelProperty}

@ApiModel(description = "Spark AppName and AppId")
case class SolutionMetrics(@ApiModelProperty(value = "application name")
                           appName: String,
                           @ApiModelProperty(value = "application id")
                           appId: String) {
  def this() = this(null, null)
}