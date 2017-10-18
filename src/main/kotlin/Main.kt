import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.google.gson.Gson
import data.Repository
import git.Cloner

fun main(args : Array<String>)
{
    cloneAll()
}

private fun cloneAll()
{
    Cloner("project_map.json").cloneAll()
}

data class SearchResponse(var items:List<Repository>, var total_count:Int)
{
    class Deserializer : ResponseDeserializable<SearchResponse> {
        override fun deserialize(content: String) = Gson().fromJson(content, SearchResponse::class.java)
    }
}




