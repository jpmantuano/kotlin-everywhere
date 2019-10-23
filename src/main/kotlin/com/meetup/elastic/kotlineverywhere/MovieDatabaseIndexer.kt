package com.meetup.elastic.kotlineverywhere

import com.fasterxml.jackson.databind.ObjectMapper
import io.inbot.eskotlinwrapper.JacksonModelReaderAndWriter
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.client.crudDao
import org.json.JSONArray
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.nio.file.Paths

data class Movie(
        var movie_id: String = "",
        var title: String = "",
        var cast: MutableList<Any> = mutableListOf(),
        var crew: MutableList<Any> = mutableListOf()
)

fun writeToElastic(indexName: String, esClient: RestHighLevelClient) {
    val dao = esClient.crudDao(indexName,
            refreshAllowed = true,
            modelReaderAndWriter = JacksonModelReaderAndWriter(
                    Movie::class,
                    ObjectMapper().findAndRegisterModules())
    )

    val movies = readMovieFile()

    dao.bulk {
        movies.records.forEach { record ->
            index(record.get("movie_id"), Movie(
                    movie_id = record.get("movie_id"),
                    title = record.get("title"),
                    cast = JSONArray(record.get("cast")).toList(),
                    crew = JSONArray(record.get("crew")).toList()
            ))
        }
    }
}


private val resourcePath = "${Paths.get("").toAbsolutePath()}/src/main/resources"
private val file = "${resourcePath}/tmdb_5000_credits.csv"

fun readMovieFile(): CSVParser {
    val bufferedReader = BufferedReader(FileReader(File(file)))

    return CSVParser(bufferedReader, CSVFormat.DEFAULT
            .withFirstRecordAsHeader()
            .withIgnoreHeaderCase()
            .withTrim())
}