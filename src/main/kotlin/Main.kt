import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import common.*
import data.ProjectModel
import data.Repository
import data.SourceFile
import git.Cloner
import javafx.scene.chart.XYChart
import ml.Cluster
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
fun File.toSaveFile(): File {
    return Paths.get(this.path.replace("Research\\Repository", "Research\\data"), "list_source.json").toFile()
}

fun printAllCloneCommand() {
    val cloneDir = "C:/Research/Repository";
    val map: HashMap<String, Repository> = Gson().fromJson(File(PATH_PROJECT_MAP).readText(), object : TypeToken<HashMap<String, Repository>>() {}.type)

    map.values.forEach { target ->
        if (target.language.equals("Java") && target.size < 2048000) {
            val path = "$cloneDir/${target.full_name}"
            if (!File(path).exists()) {
                val cmd = "git clone ${target.clone_url} $path";
                println(cmd)
            }
        }
    }
}

fun analysis1() {
    val srcLenList: MutableList<Int> = mutableListOf()
    val wordLenList: MutableList<Int> = mutableListOf()
    val wordSet: HashSet<String> = hashSetOf()

    val srcWordLenList: MutableList<Int> = mutableListOf()
    val srcWordSet: HashSet<String> = hashSetOf()

    val comWordLenList: MutableList<Int> = mutableListOf()
    val comWordSet: HashSet<String> = hashSetOf()

    var srcLen = 0.0;
    var pSrcLen = 0.0;

    Projects.getAllProjects().filter { it.toSaveFile().exists() }.forEach {
        val project = ProjectModel.load(it)

        project.sourceList.forEach {
            it.wordMap.get(KEY_COMMENT)?.forEach {
                if (it.value > 2) {
                    val key = it.key
                    if (!wordSet.contains(key))
                        wordSet.add(key)

                    if (!comWordSet.contains(key))
                        comWordSet.add(key)
                }
            }

            it.wordMap.get(KEY_SOURCE)?.forEach {
                if (it.value > 2) {
                    val key = it.key
                    if (!wordSet.contains(key))
                        wordSet.add(key)

                    if (!srcWordSet.contains(key))
                        srcWordSet.add(key)
                }
            }

            srcLen += (it.srcLen + it.comLen) / 1000000.0

            if (srcLen - pSrcLen >= 500) {
                println("add ${srcLen.toInt()}")
                pSrcLen = srcLen
                srcLenList.add(srcLen.toInt())
                wordLenList.add(wordSet.size / 1000)
                srcWordLenList.add(srcWordSet.size / 1000)
                comWordLenList.add(comWordSet.size / 1000)
            }
        }
    }

    println("last ${srcLen.toInt()}")
    pSrcLen = srcLen
    srcLenList.add(srcLen.toInt())
    wordLenList.add(wordSet.size / 1000)
    srcWordLenList.add(srcWordSet.size / 1000)
    comWordLenList.add(comWordSet.size / 1000)

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
}


fun main(args: Array<String>) {

    val target: MutableList<SourceFile> = mutableListOf()

    run outer@ {
        Projects.getAllProjects().forEach {
            val project = ProjectModel.load(it)

            target.addAll(project.sourceList)

            if (target.size > 500)
                return@outer
        }
    }

    val c = Cluster(target)
    c.clustering()
    return
    printAllCloneCommand()
    return
    ProjectExtractor(Paths.get(PATH_PROJECT_MAP)).extract()
    return

    analysis1()

    return
    var totalSrcLen: Long = 0
    var totalComLen: Long = 0
    var totalFileCount: Long = 0

    Projects.getAllProjects().forEach {
        val project = ProjectModel.createOrLoad(it)

        project.sourceList.forEach {
            totalFileCount++
            totalComLen += it.comLen
            totalSrcLen += it.srcLen
        }

        println("src Len : $totalSrcLen / com Len : $totalComLen / fileCount : $totalFileCount")
    }
    return


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




