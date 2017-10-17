import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import data.Repository
import net.ProjectExtractor
import java.io.File
import java.nio.file.Paths

fun main(args : Array<String>)
{
//    var hashMap = HashMap<String, Repository>()
//    var str = File("projects.json").readText()
//    var list : List<SearchResponse> = Gson().fromJson(str, Array<SearchResponse>::class.java).toList()
//
//    list.forEach {
//        it.items.forEach {
//            item ->
//            if(!hashMap.containsKey(item.full_name))
//                hashMap.put(item.full_name, item);
//        }
//    }
//
//    File("projects_map.json").printWriter().use { out
//        ->
//        out.print(GsonBuilder().setPrettyPrinting().create().toJson(hashMap))
//    }
//    "ss".clone()
//    File("dd").printWriter()
//    ProjectExtractor(Paths.get("projects_map.json")).extract(31)

    var map : HashMap<String, Repository> = Gson().fromJson(File("projects_map.json").readText(), object : TypeToken<HashMap<String, Repository>>() {}.type)

    map.values.forEach({
        it.full_name.clone()
    })

}

inline fun String.clone()
{
    println(this)
}

data class SearchResponse(var items:List<Repository>, var total_count:Int)
{
    class Deserializer : ResponseDeserializable<SearchResponse> {
        override fun deserialize(content: String) = Gson().fromJson(content, SearchResponse::class.java)
    }
}




