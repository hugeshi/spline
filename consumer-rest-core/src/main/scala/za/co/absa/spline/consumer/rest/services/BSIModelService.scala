package za.co.absa.spline.consumer.rest.services

import org.springframework.http.ResponseEntity
import za.co.absa.spline.consumer.rest.model.ModelStatus

trait BSIModelService {
  def getBSIModelStatus(): ResponseEntity[Array[ModelStatus]]
}
