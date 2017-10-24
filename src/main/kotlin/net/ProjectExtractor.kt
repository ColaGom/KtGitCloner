package net

import SearchResponse
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import data.Repository
import java.io.PrintWriter
import java.nio.file.Path

class ProjectExtractor(val savePath: Path)
{
    fun extract(page : Int = 0)
    {
        var results = HashMap<String, Repository>()
        var totalCount = 0
        val requestBuilder = RequestBuilder()

        if(savePath.toFile().exists())
            results = Gson().fromJson(savePath.toFile().readText(), object : TypeToken<HashMap<String, Repository>>() {}.type)

        requestBuilder.page = page;

        while(true)
        {
            val (request, response, result) = requestBuilder.build().responseObject(SearchResponse.Deserializer());
            val (search, err) = result

            println(request.url)

            search?.let {
                search.items.forEach({
                    if(!results.containsKey(it.full_name)) {
                        results.put(it.full_name, it);
                        println("added ${it.full_name}")
                    }
                })
                totalCount += search.items.size;
            }

            if(requestBuilder.page > 34)
                break
            else if(err != null) {
                Thread.sleep(10000)
            }
            else
                requestBuilder.page++;
        }

        savePath.toFile().printWriter().use { out: PrintWriter ->
            out.print(GsonBuilder().setPrettyPrinting().create().toJson(results))
        }
    }
}

