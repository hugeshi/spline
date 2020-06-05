package za.co.absa.spline.consumer.service.model.sparkjob

import za.co.absa.spline.consumer.service.model.LineageDetailed.OperationID

case class AttributesMapping(writeOp: OperationID, readSchema: Array[String], writeSchema: Array[String]) {
  def this() = this(null, null, null)
}
