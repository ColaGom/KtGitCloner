import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import common.*
import data.ProjectModel
import data.Repository
import git.Cloner
import javafx.scene.chart.XYChart
import net.ProjectExtractor
import org.knowm.xchart.*
import org.knowm.xchart.style.CategoryStyler
import org.tartarus.snowball.ext.englishStemmer
import java.io.File
import org.knowm.xchart.style.Styler
import java.nio.file.Paths


//소스 파일 bag of words
//count of list, count of set
//count of line
fun File.toSaveFile() : File
{
    return Paths.get(this.path.replace("Research\\Repository","Research\\data"), "list_source.json").toFile()
}

fun printAllCloneCommand()
{
    val cloneDir = "C:/Research/Repository";
    val map : HashMap<String, Repository> = Gson().fromJson(File(PATH_PROJECT_MAP).readText(), object : TypeToken<HashMap<String, Repository>>() {}.type)

    map.values.forEach { target->
        if(target.language.equals("Java") && target.size < 2048000)
        {
            val cmd = "git clone ${target.clone_url} $cloneDir/${target.full_name}";
            println(cmd)
        }
    }
}


fun main(args: Array<String>){
    val srcLenList : MutableList<Int> = mutableListOf()
    val wordLenList : MutableList<Int> = mutableListOf()
    val wordSet : HashSet<String> = hashSetOf()

    val srcWordLenList : MutableList<Int> = mutableListOf()
    val srcWordSet : HashSet<String> = hashSetOf()

    val comWordLenList : MutableList<Int> = mutableListOf()
    val comWordSet : HashSet<String> = hashSetOf()

    var srcLen = 0.0;
    var pSrcLen = 0.0;

    Projects.getAllProjects().filter { it.toSaveFile().exists() }.forEach{
        val project = ProjectModel.load(it)

        project.sourceList.forEach{
            it.wordMap.get(KEY_COMMENT)?.keys?.forEach{
                if(!wordSet.contains(it))
                    wordSet.add(it)

                if(!srcWordSet.contains(it))
                    srcWordSet.add(it)
            }

            it.wordMap.get(KEY_SOURCE)?.keys?.forEach{
                if(!wordSet.contains(it))
                    wordSet.add(it)

                if(!comWordSet.contains(it))
                    comWordSet.add(it)
            }
            srcLen += (it.srcLen + it.comLen) / 1000000.0

            if(srcLen - pSrcLen > 200)
            {
                println("add ${srcLen.toInt()}")
                pSrcLen = srcLen
                srcLenList.add(srcLen.toInt())
                wordLenList.add(wordSet.size/1000)
                srcWordLenList.add(srcWordSet.size/1000)
                comWordLenList.add(comWordSet.size/1000)
            }
        }
    }


//
    val chart = XYChartBuilder().width(800).height(600).title("Title").xAxisTitle("length of source code (m)").yAxisTitle("size of wordset (k)").theme(Styler.ChartTheme.Matlab).build()
    chart.styler.setMarkerSize(5)
    chart.styler.setLegendPosition(Styler.LegendPosition.InsideNW)
    chart.styler.setChartTitleVisible(false)

    chart.addSeries("src+com", srcLenList.toIntArray(), wordLenList.toIntArray())
    chart.addSeries("src", srcLenList.toIntArray(), srcWordLenList.toIntArray())
    chart.addSeries("com", srcLenList.toIntArray(), comWordLenList.toIntArray())

    SwingWrapper(chart).displayChart()

    BitmapEncoder.saveBitmapWithDPI(chart, "./chart_300", BitmapEncoder.BitmapFormat.PNG, 300);

    return

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
    println("src Len : $totalSrcLen / com Len : $totalComLen / fileCount : $totalFileCount")
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
    Cloner(PATH_PROJECT_MAP).cloneAll()
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




