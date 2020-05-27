package za.co.absa.spline.consumer.rest.model

import com.fasterxml.jackson.annotation.{JsonCreator, JsonIgnoreProperties, JsonProperty}

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonCreator
case class Model(@JsonProperty("id") id: Int,
                 @JsonProperty("modelName") modelName: String,
                 @JsonProperty("modelAkaNames") modelAkaNames: Array[String],
                 @JsonProperty("tags") tags: Array[String],
                 @JsonProperty("repoName") repoName: String,
                 @JsonProperty("frequency") frequency: String,
                 @JsonProperty("retired") retired: Boolean,
                 @JsonProperty("uc4Chain") uc4Chain: String,
                 @JsonProperty("uc4MajorModule") uc4MajorModule: String)

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonCreator
case class ModelStatus(@JsonProperty("model") model: Model, @JsonProperty("tables") tables: Array[TableStatus])

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonCreator
case class PartitionCdc(@JsonProperty("partition") partition: String, @JsonProperty("cdcDate") cdcDate: String)

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonCreator
case class TableStatus(
                        @JsonProperty("tableId") tableId: Int,
                        @JsonProperty("isInput") isInput: Boolean,
                        @JsonProperty("isVariableTable") isVariableTable: Boolean,
                        @JsonProperty("hasHiveTable") hasHiveTable: Boolean,
                        @JsonProperty("hiveTable") hiveTable: String,
                        @JsonProperty("hiveCdcCol") hiveCdcCol: String,
                        @JsonProperty("hivePartitions") hivePartitions: Array[String],
                        @JsonProperty("hivePartitionCdcDates") hivePartitionCdcDates: Array[PartitionCdc],
                        @JsonProperty("hiveLatestPartition") hiveLatestPartition: String,
                        @JsonProperty("hiveLatestDate") hiveLatestDate: String,
                        @JsonProperty("hiveLastCrawlEpoch") hiveLastCrawlEpoch: Integer, // Integer instead of Int to allow null
                        @JsonProperty("hasTDTable") hasTDTable: Boolean,
                        @JsonProperty("tdCluster") tdCluster: String,
                        @JsonProperty("tdTable") tdTable: String,
                        @JsonProperty("tdCdcCol") tdCdcCol: String,
                        @JsonProperty("tdLatestDate") tdLatestDate: String,
                        @JsonProperty("tdLastCrawlEpoch") tdLastCrawlEpoch: Int, // Integer instead of Int to allow null
                        @JsonProperty("scoreCols") scoreCols: Array[String])
