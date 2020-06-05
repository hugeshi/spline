package za.co.absa.spline.consumer.service.bsi

import java.util.UUID

import io.swagger.annotations.{ApiModel, ApiModelProperty}

@ApiModel(description = "Attribute Dependencies")
case class AttributeDependencies(
                                  @ApiModelProperty(value = "List of attribute ids on which the requested attribute depends")
                                  attributes: Seq[UUID],
                                  @ApiModelProperty(value = "List of operation ids referencing provided attribute or dependent attributes")
                                  operations: Seq[String]
                                )
