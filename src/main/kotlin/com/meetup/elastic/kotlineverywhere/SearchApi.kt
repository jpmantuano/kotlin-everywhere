package com.meetup.elastic.kotlineverywhere

import com.fasterxml.jackson.databind.ObjectMapper
import io.inbot.eskotlinwrapper.JacksonModelReaderAndWriter
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.client.create
import org.elasticsearch.client.crudDao
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.SearchHit
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class RestController(
        @Autowired val esClient: RestHighLevelClient
) {

    @GetMapping("/search")
    fun search(@RequestParam(value = "q", defaultValue = "") query: String,
               @RequestParam(value = "fq", defaultValue = "") filters: List<String>): Array<SearchHit>? {
        val searchSourceBuilder =
                SearchSourceBuilder().query(
                        QueryBuilders.multiMatchQuery(query, "text", "title")
                )
        val searchRequest = SearchRequest("movies-1").source(searchSourceBuilder)
        val response = esClient.search(searchRequest, RequestOptions.DEFAULT)

        return response.hits.hits
    }

    @GetMapping("/search/dao")
    fun searchUsingDao(@RequestParam(value = "q", defaultValue = "") query: String,
                       @RequestParam(value = "fq", defaultValue = "") filters: List<String>): List<Movie> {
        val moviesDao = esClient.crudDao("movies-1",
                refreshAllowed = true,
                modelReaderAndWriter = JacksonModelReaderAndWriter(
                        Movie::class,
                        ObjectMapper().findAndRegisterModules()))

        val result = moviesDao.search {
            source(SearchSourceBuilder().query(
                    QueryBuilders.multiMatchQuery(query, "text", "title")
            ))
        }

        return result.mappedHits.toList()
    }

    @GetMapping("/index")
    fun createIndex(@RequestParam(value = "index", defaultValue = "search-index") indexName: String) {
        writeToElastic(indexName, esClient)
    }
}

@Configuration
class ElasticConfig {

    @Value("\${es.host}")
    val host = ""

    @Bean
    fun esClient(): RestHighLevelClient {
        return create(
                host = host
        )
    }
}

@SpringBootApplication
class SearchApi

fun main() {
    runApplication<SearchApi>()
}

