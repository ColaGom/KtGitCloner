import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import common.KEY_COMMENT
import common.KEY_SOURCE
import common.PATH_PROJECT_MAP
import common.Projects
import data.ProjectModel
import data.Repository
import data.SourceFile
import git.Cloner
import ml.Cluster
import ml.Searcher
import net.ProjectExtractor
import org.knowm.xchart.BitmapEncoder
import org.knowm.xchart.XYChartBuilder
import org.knowm.xchart.style.Styler
import java.io.File
import java.nio.file.Paths
import java.util.*


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

fun analysis1(limit: Int = 0, thr: Int = 1, suffix: String) {
    val srcLenList: MutableList<Int> = mutableListOf()
    val wordLenList: MutableList<Int> = mutableListOf()
    val wordSet: HashMap<String, Int> = hashMapOf()

    val srcWordLenList: MutableList<Int> = mutableListOf()
    val srcWordSet: HashMap<String, Int> = hashMapOf()

    val comWordLenList: MutableList<Int> = mutableListOf()
    val comWordSet: HashMap<String, Int> = hashMapOf()

    var srcLen = 0.0;
    var pSrcLen = 0.0;

    Projects.getAllProjects().filter { it.toSaveFile().exists() }.forEach {
        val project = ProjectModel.load(it)

        project.sourceList.forEach {
            it.wordMap.get(KEY_COMMENT)?.forEach {
                if (it.value > thr) {
                    val key = it.key
                    if (!wordSet.contains(key))
                        wordSet.put(key, 1)
                    else
                        wordSet.set(key, wordSet.get(key)!! + 1)

                    if (!comWordSet.contains(key))
                        comWordSet.put(key, 1)
                    else
                        comWordSet.put(key, comWordSet.get(key)!! + 1)
                }
            }

            it.wordMap.get(KEY_SOURCE)?.forEach {
                if (it.value > thr) {
                    val key = it.key
                    if (!wordSet.contains(key))
                        wordSet.put(key, 1)
                    else
                        wordSet.set(key, wordSet.get(key)!! + 1)

                    if (!srcWordSet.contains(key))
                        srcWordSet.put(key, 1)
                    else
                        srcWordSet.set(key, srcWordSet.get(key)!! + 1)
                }
            }

            srcLen += it.wordMapSize() / 1000000.0

            if (srcLen - pSrcLen >= 35) {
                println("add ${srcLen.toInt()}")
                pSrcLen = srcLen
                srcLenList.add(srcLen.toInt())
                wordLenList.add(wordSet.filter { it.value > limit }.size / 1000)
                srcWordLenList.add(srcWordSet.filter { it.value > limit }.size / 1000)
                comWordLenList.add(comWordSet.filter { it.value > limit }.size / 1000)
            }
        }
    }

    println("last ${srcLen.toInt()}")
    pSrcLen = srcLen
    srcLenList.add(srcLen.toInt())
    wordLenList.add(wordSet.filter { it.value > limit }.size / 1000)
    srcWordLenList.add(srcWordSet.filter { it.value > limit }.size / 1000)
    comWordLenList.add(comWordSet.filter { it.value > limit }.size / 1000)

//
    val chart = XYChartBuilder().width(800).height(600).title("Title").xAxisTitle("length of words in source code (m)").yAxisTitle("size of wordset (k)").theme(Styler.ChartTheme.Matlab).build()
    chart.styler.setMarkerSize(5)
    chart.styler.setLegendPosition(Styler.LegendPosition.InsideNW)
    chart.styler.setChartTitleVisible(false)

    chart.addSeries("src+com", srcLenList.toIntArray(), wordLenList.toIntArray())
    chart.addSeries("src", srcLenList.toIntArray(), srcWordLenList.toIntArray())
    chart.addSeries("com", srcLenList.toIntArray(), comWordLenList.toIntArray())

//    SwingWrapper(chart).displayChart()

    BitmapEncoder.saveBitmapWithDPI(chart, "./${limit}_${suffix}.png", BitmapEncoder.BitmapFormat.PNG, 200);
    File("./${limit}_srcWordSet_${suffix}.json").printWriter().use {
        it.print(GsonBuilder().setPrettyPrinting().create().toJson(srcWordSet.filter { it.value > limit }))
    }
    File("./${limit}_comWordSet_${suffix}.json").printWriter().use {
        it.print(GsonBuilder().setPrettyPrinting().create().toJson(comWordSet.filter { it.value > limit }))
    }
}

fun printBaseInfo()
{
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
}

fun main(args: Array<String>) {
    printBaseInfo()
    return

    analysis1(0, suffix = "2")
    analysis1(10, suffix = "2")
    analysis1(100, suffix = "2")
    return
    Projects.getAllProjects().forEach {
        val project = ProjectModel.load(it)
    }
    return
    Projects.getAllProjects().forEach {
        val name = it.path.replace("C:\\Research\\Repository\\" ,"").replace("\\","_") +".json"
        val savePath = Paths.get("D:\\gitExp", name);
        println("gitExplorer.jar ${it.path} $savePath")
    }
    return
    Projects.getAllProjects().filter { !it.toSaveFile().exists() }.forEach {
        ProjectModel.create(it)
    }
    return
    val target: MutableList<SourceFile> = mutableListOf()

    run outer@ {
        Projects.getAllProjects().filter { it.toSaveFile().exists() }.forEach {
            val project = ProjectModel.load(it)

            if(project.sourceList.size > 100)
                target.addAll(project.sourceList.subList(0, 100))
            else
                target.addAll(project.sourceList)
        }
    }

    val query = ProjectModel.load(File("C:\\Research\\Repository\\h2database\\h2database"))
            .sourceList.find { it.path.contains("MathUtils.java") }

    println(GsonBuilder().setPrettyPrinting().create().toJson(query))

    println(Searcher(query!!, target).run().toList().filter { !it.second.isNaN() }.sortedBy { pair -> pair.second }.reversed().subList(0, 30))
    return

//    val target: MutableList<SourceFile> = mutableListOf()
//
//    run outer@ {
//        Projects.getAllProjects().filter { it.toSaveFile().exists() }.forEach {
//            val project = ProjectModel.load(it)
//
//            if (project.sourceList.size > 20)
//                target.addAll(project.sourceList.subList(0, Random().nextInt(20)))
//            else
//                target.addAll(project.sourceList.subList(0, Random().nextInt(project.sourceList.size)))
//
//            if (target.size > 10000)
//                return@outer
//        }
//    }
//
//    val c = Cluster(target)
//    c.clustering(iter = 10)
    return


    return
    printAllCloneCommand()
    return
    ProjectExtractor(Paths.get(PATH_PROJECT_MAP)).extract()
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




