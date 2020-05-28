package za.co.absa.spline.consumer.service.model.sparkjob

import io.swagger.annotations.{ApiModel, ApiModelProperty}

@ApiModel(description = "Spark job input and output tables")
case class SparkJobIOs(
                        @ApiModelProperty(value = "List of job input tables")
                        inputs: Array[String],
                        @ApiModelProperty(value = "List of job output tables")
                        outputs: Array[String]
                      ) {
  def this() = this(null, null)
}