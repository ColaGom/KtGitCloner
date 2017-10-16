import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.google.gson.Gson
import data.Repository
import net.ProjectExtractor
import java.nio.file.Paths

fun main(args : Array<String>)
{
    ProjectExtractor(Paths.get("projects.json")).extract()
}

data class SearchResponse(var items:List<Repository>, var total_count:Int)
{
    class Deserializer : ResponseDeserializable<SearchResponse> {
        override fun deserialize(content: String) = Gson().fromJson(content, SearchResponse::class.java)
    }
}
