import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import common.*
import data.ProjectModel
import data.Repository
import git.Cloner
import org.tartarus.snowball.ext.englishStemmer
import java.io.File

//소스 파일 bag of words
//count of list, count of set
//count of line


fun main(args: Array<String>) {
//    val txt = File("C:\\Research\\Repository\\AndroidBootstrap\\android-bootstrap\\app\\src\\main\\java\\com\\donnfelker\\android\\bootstrap\\util\\ViewUpdater.java").readText()
//
//    println(PreProcessor("").splitLargeStr(1000, txt))
//
//    return
    var totalSrcLen : Long = 0
    var totalComLen : Long = 0
    var totalFileCount : Long = 0

    Projects.getAllProjects().forEach {
        val project = ProjectModel.create(it)

        project.sourceList.forEach{
            totalFileCount++
            totalComLen+= it.comLen
            totalSrcLen += it.srcLen
        }
    }
    println("src Len : $totalSrcLen / com Len : $totalComLen / fileCoutn : $totalFileCount")
    return

    val stemmer = englishStemmer()
    FileUtils.readAllFiles(File(PATH_DATA), NAME_BOW_MAP).forEach({
        val currentMap: HashMap<String, HashMap<String, List<String>>> = Gson().fromJson(it.readText(), object : TypeToken<HashMap<String, HashMap<String, List<String>>>>() {}.type)

        var stemmedMap : HashMap<String, HashMap<String, HashMap<String, Int>>> = hashMapOf()

        currentMap.forEach {
            val filePath = it.key
            var data : HashMap<String, HashMap<String, Int>> = hashMapOf()

            it.value.forEach {
                val key = it.key
                val freqMap : HashMap<String, Int> = hashMapOf()

                it.value.forEach {
                    stemmer.setCurrent(it)
                    stemmer.stem()
                    val word = stemmer.current

                    if(freqMap.containsKey(word))
                        freqMap.put(word, freqMap.get(word)!! + 1)
                    else
                        freqMap.put(word, 1)
                }

                data.put(key, freqMap)
            }

            stemmedMap.put(filePath.replace("\\\\","\\"), data)
        }

        val savePath = it.path.replace(NAME_BOW_MAP, NAME_FREQ_SNOW_MAP)
        File(savePath).printWriter().use {
            it.print(GsonBuilder().setPrettyPrinting().create().toJson(stemmedMap))
        }
        println("saved $savePath")
    })

//    FileUtils.readAllFiles(File("C:/Research/data"), NAME_FREQ_SNOW).forEach {
//
//    }
}

private fun cloneAll() {
    Cloner("project_map.json").cloneAll()
}

data class SearchResponse(var items: List<Repository>, var total_count: Int) {
    class Deserializer : ResponseDeserializable<SearchResponse> {
        override fun deserialize(content: String) = Gson().fromJson(content, SearchResponse::class.java)
    }
}

inline fun <T> MutableList<T>.mapInPlace(mutator: (T) -> T) {
    val iterate = this.listIterator()
    while (iterate.hasNext()) {
        val oldValue = iterate.next()
        val newValue = mutator(oldValue)
        if (newValue !== oldValue) {
            iterate.set(newValue)
        }
    }
}




