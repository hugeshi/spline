package za.co.absa.spline.consumer.service.model.sparkjob

case class SparkAppInfo(appName: String, appId: String, createTime: Long) {
  def this() = this(null, null, 0)
}