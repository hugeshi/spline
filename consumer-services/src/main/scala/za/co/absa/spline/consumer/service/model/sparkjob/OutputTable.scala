package za.co.absa.spline.consumer.service.model.sparkjob

import io.swagger.annotations.{ApiModel, ApiModelProperty}

@ApiModel(description = " Spark Job output tables")
class OutputTable(
                   @ApiModelProperty(value = "database base")
                   database: String,
                   @ApiModelProperty(value = "table name")
                   table: String,
                   @ApiModelProperty(value = "list of schema name")
                   schema: Array[String]
                 ) {
  def this() = this(null, null, null)
}
