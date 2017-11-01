import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import common.*
import data.ProjectModel
import data.Repository
import data.SourceFile
import git.Cloner
import git.CommitExtractor
import javafx.collections.transformation.SortedList
import ml.Cluster
import ml.Searcher
import net.ProjectExtractor
import org.knowm.xchart.BitmapEncoder
import org.knowm.xchart.CategoryChartBuilder
import org.knowm.xchart.XYChartBuilder
import org.knowm.xchart.style.Styler
import java.io.File
import java.nio.file.Paths
import java.util.*
import kotlin.collections.HashMap
import kotlin.streams.asStream


//소스 파일 bag of words
//count of list, count of set
//count of line
fun File.toSaveFile(): File {
    return Paths.get(this.path.replace("Research\\Repository", "Research\\data"), "list_source.json").toFile()
}

fun printAllCloneCommand() {
    val cloneDir = "C:/Research/Repository";
    val map: HashMap<String, Repository> = Gson().fromJson(File(PATH_PROJECT_MAP).readText(), object : TypeToken<HashMap<String, Repository>>() {}.type)

    println("total : ${map.size}")
    map.values.forEach { target ->
        if (target.language != null && target.language.equals("Java") && target.size < 2048000) {
            val path = "$cloneDir/${target.full_name}"
            if (!File(path).exists()) {
                val cmd = "git clone ${target.clone_url} ${path.replace("C:/","D:/")}";
                println(cmd)
            }
        }
    }
}

fun analysis1(limit: Int = 0, thr: Int = 0, suffix: String) {
    val srcLenList: MutableList<Double> = mutableListOf()
    val wordLenList: MutableList<Double> = mutableListOf()
    val wordSet: HashMap<String, Int> = hashMapOf()

    val srcWordLenList: MutableList<Double> = mutableListOf()
    val srcWordSet: HashMap<String, Int> = hashMapOf()

    val comWordLenList: MutableList<Double> = mutableListOf()
    val comWordSet: HashMap<String, Int> = hashMapOf()

    val addSrcLenList: MutableList<Double> = mutableListOf()
    val addComLenList: MutableList<Double> = mutableListOf()

    var srcLen = 0.0;
    var pSrcLen = 0.0;

    File(PATH_DATA).walkTopDown().filter{
        it.name.equals(NAME_PROJECT_MODEL)
    }.forEach {
        val project = ProjectModel.load(it)
        val projectWordSet : MutableSet<String> = mutableSetOf()
        val projectSrcWordSet : MutableSet<String> = mutableSetOf()
        val projectComWordSet : MutableSet<String> = mutableSetOf()

        projectWordSet.clear()
        projectSrcWordSet.clear()
        projectComWordSet.clear()

        project.sourceList.forEach {
            it.wordMap.get(KEY_COMMENT)?.forEach {
                if (it.value > thr) {
                    val key = it.key

                    if(!projectWordSet.contains(key))
                        projectWordSet.add(key)

                    if(!projectComWordSet.contains(key))
                        projectComWordSet.add(key)
                }
            }

            it.wordMap.get(KEY_SOURCE)?.forEach {
                if (it.value > thr) {
                    val key = it.key

                    if(!projectWordSet.contains(key))
                        projectWordSet.add(key)

                    if(!projectSrcWordSet.contains(key))
                        projectSrcWordSet.add(key)
                }
            }
            srcLen += it.wordMapSize() / 1000000.0
        }

        projectComWordSet.forEach {
            if(comWordSet.containsKey(it))
                comWordSet.set(it, comWordSet.get(it)!! + 1)
            else
                comWordSet.put(it, 1)
        }

        projectSrcWordSet.forEach {
            if(srcWordSet.containsKey(it))
                srcWordSet.set(it, srcWordSet.get(it)!! + 1)
            else
                srcWordSet.put(it, 1)
        }

        projectWordSet.forEach {
            if(wordSet.containsKey(it))
                wordSet.set(it, wordSet.get(it)!! + 1)
            else
                wordSet.put(it, 1)
        }

        if (srcLen - pSrcLen >= 100) {
            println("add ${srcLen}")
            val preSrcLen = if(srcWordLenList.isEmpty()) 0.0 else srcWordLenList.last()
            val preComLen = if(comWordLenList.isEmpty()) 0.0 else comWordLenList.last()

            pSrcLen = srcLen
            srcLenList.add(srcLen)
            wordLenList.add(wordSet.filter { it.value >= limit }.size / 1000.0)
            srcWordLenList.add(srcWordSet.filter { it.value >= limit }.size / 1000.0)
            comWordLenList.add(comWordSet.filter { it.value >= limit }.size / 1000.0)

            addSrcLenList.add(srcWordLenList.last() - preSrcLen)
            addComLenList.add(comWordLenList.last() - preComLen)
        }
    }

    println("last ${srcLen.toInt()}")
    val preSrcLen = srcWordLenList.last()
    val preComLen = comWordLenList.last()

    pSrcLen = srcLen
    srcLenList.add(srcLen)
    wordLenList.add(wordSet.filter { it.value >= limit }.size / 1000.0)
    srcWordLenList.add(srcWordSet.filter { it.value >= limit }.size / 1000.0)
    comWordLenList.add(comWordSet.filter { it.value >= limit }.size / 1000.0)
    addSrcLenList.add(srcWordLenList.last() - preSrcLen)
    addComLenList.add(comWordLenList.last() - preComLen)
//
    val chart = XYChartBuilder().width(800).height(600).title("Title").xAxisTitle("length of words in source code (m)").yAxisTitle("size of wordset (k)").theme(Styler.ChartTheme.Matlab).build()
    chart.styler.setMarkerSize(5)
    chart.styler.setLegendPosition(Styler.LegendPosition.InsideNW)
    chart.styler.setChartTitleVisible(false)

    chart.addSeries("src+com", srcLenList.toDoubleArray(), wordLenList.toDoubleArray())
    chart.addSeries("src", srcLenList.toDoubleArray(), srcWordLenList.toDoubleArray())
    chart.addSeries("com", srcLenList.toDoubleArray(), comWordLenList.toDoubleArray())

//    SwingWrapper(chart).displayChart()
    val resultDir = "C:\\Research\\Result"

    BitmapEncoder.saveBitmapWithDPI(chart, "$resultDir\\${limit}_${suffix}.png", BitmapEncoder.BitmapFormat.PNG, 200);

    File(resultDir,"${limit}_srcWordSet_${suffix}.json").printWriter().use {
        it.print(GsonBuilder().setPrettyPrinting().create().toJson(srcWordSet.filter { it.value > limit }.toList().sortedBy { it.second }.reversed()))
    }
    File(resultDir,"${limit}_comWordSet_${suffix}.json").printWriter().use {
        it.print(GsonBuilder().setPrettyPrinting().create().toJson(comWordSet.filter { it.value > limit }.toList().sortedBy { it.second }.reversed()))
    }

    val chart2 = XYChartBuilder().width(800).height(600).title("Title").xAxisTitle("length of words in source code (m)").yAxisTitle("size of wordset (k)").theme(Styler.ChartTheme.Matlab).build()
    chart.styler.setMarkerSize(5)
    chart.styler.setLegendPosition(Styler.LegendPosition.InsideNW)
    chart.styler.setChartTitleVisible(false)

    chart2.addSeries("add-src", srcLenList.toDoubleArray(), addSrcLenList.toDoubleArray())
    chart2.addSeries("add-com", srcLenList.toDoubleArray(), addComLenList.toDoubleArray())

    BitmapEncoder.saveBitmapWithDPI(chart2, "$resultDir\\${limit}_${suffix}_add.png", BitmapEncoder.BitmapFormat.PNG, 200);
}

fun cleanSourceFile(source:SourceFile)
{
    source.wordMap.values.forEach{
        val iter = it.iterator()
        while(iter.hasNext())
        {
            val value = iter.next()
            val key = value.key

            if(key.length <= 2 || key.length >= 20)
            {
                iter.remove()
            }
        }
    }
}

fun printBaseInfo()
{
    var totalSrcLen: Long = 0
    var totalComLen: Long = 0
    var totalFileCount: Long = 0
    var projectCount = 0

    File(PATH_DATA).walkTopDown().asStream().parallel().filter{
        it.name.equals(NAME_PROJECT_MODEL)
    }.forEach {
        val project = ProjectModel.load(it)
        projectCount++

        project.sourceList.forEach {
            totalFileCount++
            totalComLen += it.comLen
            totalSrcLen += it.srcLen
        }
    }

    println("src Len : $totalSrcLen / com Len : $totalComLen / fileCount : $totalFileCount / project Count : $projectCount")
}

fun cleanProject(root:File)
{
    println("[cleanProject]${root.path}")
    root.walkTopDown().filter {
        it.isFile && !it.path.contains(".git") && !it.extension.equals("java") && !it.extension.equals("md")
    }.forEach {
        it.delete()
    }
}


fun main(args: Array<String>) {

    val file = Paths.get(PATH_RESULT, "cluster", "cluster_547.json").toFile();
    val clusterList : HashMap<String, SortedMap<String, Double>> = Gson().fromJson(file.readText(), object : TypeToken<HashMap<String, SortedMap<String, Double>>>() {}.type)
    val newList : HashMap<String, Map<String, Double>> = hashMapOf()

    clusterList.forEach{
        current->
        newList.put(current.key, current.value.toList().sortedBy { it.second }.reversed().toMap())
    }

    Paths.get(PATH_RESULT, "cluster", "cluster_547_test.json").toFile().printWriter().use { out->
        out.print(
                GsonBuilder().setPrettyPrinting().create().toJson(newList)
        )
    }
    return

    val target: MutableList<SourceFile> = mutableListOf()

    run outer@ {
        File(PATH_DATA).walkTopDown().filter{
            it.name.equals(NAME_PROJECT_MODEL)
        }.forEach {
            val rnd = Random()
            val project = ProjectModel.load(it)
            val sourceList = project.sourceList.filter { it.wordMapSize() > 20 }

            if(sourceList.size == 0)
                return@forEach

            if (sourceList.size > 600)
                target.addAll(sourceList.subList(0, rnd.nextInt(600)))
            else
                target.addAll(sourceList.subList(0, rnd.nextInt(sourceList.size)))

            if (target.size > 300000)
                return@outer
        }
    }

    val c = Cluster(target)
    c.clustering(iter = 10)
    return


    analysis1(0,suffix = "new2")
    analysis1(10,suffix = "new2")
    return
    printBaseInfo()
    return
    println(CommitExtractor("C:\\Research\\Repository\\apache\\incubator-pirk").extractCommitMetaData().size)
    return
    printAllCloneCommand()
    return
    for(i in 2008..2017)
        ProjectExtractor(Paths.get(PATH_PROJECT_MAP), i).extract()
    return
    analysis1(10, suffix = "modify2")
    return


    Projects.getAllProjects().forEach {
        cleanProject(it)
    }
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
//    val target: MutableList<SourceFile> = mutableListOf()
//
//    run outer@ {
//        Projects.getAllProjects().filter { it.toSaveFile().exists() }.forEach {
//            val project = ProjectModel.load(it)
//
//            if(project.sourceList.size > 100)
//                target.addAll(project.sourceList.subList(0, 100))
//            else
//                target.addAll(project.sourceList)
//        }
//    }
//
//    val query = ProjectModel.load(File("C:\\Research\\Repository\\h2database\\h2database"))
//            .sourceList.find { it.path.contains("MathUtils.java") }
//
//    println(GsonBuilder().setPrettyPrinting().create().toJson(query))
//
//    println(Searcher(query!!, target).run().toList().filter { !it.second.isNaN() }.sortedBy { pair -> pair.second }.reversed().subList(0, 30))
//    return




    return
    printAllCloneCommand()
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




