package za.co.absa.spline.consumer.rest.services

import org.springframework.beans.factory.annotation.{Autowired, Value}
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import za.co.absa.spline.consumer.rest.model.ModelStatus

@Service
class BSIModelServiceImpl @Autowired()(@Value("${bsi.service.url}") private val url: String, private val restOperations: RestOperations)
  extends BSIModelService {

  override def getBSIModelStatus(): ResponseEntity[Array[ModelStatus]] = {
    restOperations.getForEntity(url, classOf[Array[ModelStatus]])
  }
}
