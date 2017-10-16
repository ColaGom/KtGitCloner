package net

import SearchResponse
import com.google.gson.GsonBuilder
import java.io.PrintWriter
import java.nio.file.Path

class ProjectExtractor(val savePath: Path, val limit:Int = 5000)
{
    fun extract()
    {
        var results = ArrayList<SearchResponse>()
        var totalCount = 0
        val requestBuilder = RequestBuilder()

        while(true)
        {
            val (request, response, result) = requestBuilder.build().responseObject(SearchResponse.Deserializer());
            val(search, err) = result

            search?.let {
                results.add(search);
                totalCount += search.items.size;
                println("Added current : $totalCount / $limit page : ${requestBuilder.page}")
            }

            if (err != null || totalCount > limit)
                break
            else
                requestBuilder.page++;
        }

        savePath.toFile().printWriter().use { out: PrintWriter ->
            out.print(GsonBuilder().setPrettyPrinting().create().toJson(results))
        }
    }
}

