import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.google.gson.Gson
import common.BagOfWord
import common.Projects
import data.Repository
import git.Cloner
import nlp.PreProcessor
import java.io.File

//소스 파일 bag of words
//count of list, count of set
//count of line


fun main(args : Array<String>)
{
//    val txt = File("C:\\Research\\Repository\\AndroidBootstrap\\android-bootstrap\\app\\src\\main\\java\\com\\donnfelker\\android\\bootstrap\\util\\ViewUpdater.java").readText()
//
//    println(PreProcessor("").splitLargeStr(1000, txt))
//
//    return

    Projects.getAllProjects().forEach({
        BagOfWord(it).generate()
    })
//    FileUtils.readAllFiles(File("C:/Research/data"), NAME_FREQ_SNOW).forEach {
//
//    }
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

inline fun <T> MutableList<T>.mapInPlace(mutator: (T)->T) {
    val iterate = this.listIterator()
    while (iterate.hasNext()) {
        val oldValue = iterate.next()
        val newValue = mutator(oldValue)
        if (newValue !== oldValue) {
            iterate.set(newValue)
        }
    }
}




