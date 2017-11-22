import com.github.javaparser.JavaParser
import com.github.javaparser.ast.body.MethodDeclaration
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
import ml.Kmeans
import ml.Node
import net.ProjectExtractor
import newdata.CountMap
import newdata.Project
import newdata.SourceAnalyst
import nlp.PreProcessor
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer
import org.knowm.xchart.BitmapEncoder
import org.knowm.xchart.SwingWrapper
import org.knowm.xchart.XYChartBuilder
import org.knowm.xchart.style.Styler
import java.io.File
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap


//소스 파일 bag of words
//count of list, count of set
//count of line
fun File.toSaveFile(): File {
    return Paths.get(this.path.replace("Research\\Repository", "Research\\data"), NAME_PROJECT_MODEL).toFile()
}

fun printAllCloneCommand() {
    val cloneDir = "E:/Repository";
    val map: HashMap<String, Repository> = Gson().fromJson(File(PATH_PROJECT_MAP).readText(), object : TypeToken<HashMap<String, Repository>>() {}.type)

    println("total : ${map.size}")
    map.values.forEach { target ->
        if (target.language != null && target.language.equals("Java") && target.size < 2048000) {
            val path = "$cloneDir/${target.full_name}"

            if (!File(path).exists()) {
                val cmd = "git clone ${target.clone_url} ${path}";
                println(cmd)
            }
        }
    }
}

fun loadAllProjectModelFile(name: String): Sequence<File> {
    return File(PATH_DATA).walk().filter {
        it.name.equals(name)
    }
}

fun searchTest(query: File, saveFile: File, idfMap: HashMap<String, Long>, k: Int = 1000) {
    val startTime = System.currentTimeMillis()
    val target = PreProcessor(query.readText()).toSourceFile(query.path)
    val node = Node("query", target.getMergedMap()!!.tfidfDoc(idfMap))
    val result: SortedSet<Pair<String, Double>> = sortedSetOf(compareByDescending { it.second })

    loadAllProjectModelFile(NAME_PROJECT_MODEL_CLEAN).forEach {
        val project = ProjectModel.load(it)
        project.sourceList.map { Node(it.path, it.getMergedMap()!!.tfidfDoc(idfMap)) }.forEach {
            if (result.size < k) {
                result.add(Pair(it.fileName, it.distanceTo(node)))
            } else {
                val last = result.last()
                val distance = it.distanceTo(node)
                if (last.second < distance) {
                    result.remove(last)
                    result.add(Pair(it.fileName, distance))
                }
            }
        }
    }

    println("elapsed : ${System.currentTimeMillis() - startTime}")

    saveFile.printWriter().use {
        it.print(GsonBuilder().setPrettyPrinting().create().toJson(result))
    }
}

fun analysis() {
    val dfMap: HashMap<String, Long> = Gson().fromJson(File("df_map.json").readText(), object : TypeToken<HashMap<String, Long>>() {}.type)
    val pfMap: HashMap<String, Long> = Gson().fromJson(File("pf_map.json").readText(), object : TypeToken<HashMap<String, Long>>() {}.type)

    println(dfMap.filter { it.value > 20 && it.value < 600000 }.size)

    return

    println("${dfMap.size} / ${pfMap.size}")
    val fileCountLimit = (4000000 * 0.15).toInt() // 10%
    val diff = (4000000 * 0.01).toInt() // 1%
    val projectCount = 28000

    val thresholdList = diff..fileCountLimit step diff
    val countList: MutableList<Int> = mutableListOf()
    thresholdList.forEach { limit ->
        val size = dfMap.filter { it.value > limit }.size
        countList.add(size)
        println("$limit : $size")
    }

    val chart = XYChartBuilder().width(800).height(600).title("Title").xAxisTitle("threshold").yAxisTitle("number of word").theme(Styler.ChartTheme.Matlab).build()
    chart.styler.setMarkerSize(5)
    chart.styler.setLegendVisible(false)
    chart.styler.setLegendPosition(Styler.LegendPosition.InsideNW)
    chart.styler.setChartTitleVisible(false)

    chart.addSeries("com", thresholdList.toList(), countList)

    SwingWrapper(chart).displayChart()
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

    File(PATH_DATA).walkTopDown().filter {
        it.name.equals(NAME_PROJECT_MODEL_CLEAN)
    }.forEach {
        val project = ProjectModel.load(it)
        val projectWordSet: MutableSet<String> = mutableSetOf()
        val projectSrcWordSet: MutableSet<String> = mutableSetOf()
        val projectComWordSet: MutableSet<String> = mutableSetOf()

        projectWordSet.clear()
        projectSrcWordSet.clear()
        projectComWordSet.clear()

        project.sourceList.forEach {
            it.wordMap.get(KEY_COMMENT)?.forEach {
                if (it.value > thr) {
                    val key = it.key

                    if (!projectWordSet.contains(key))
                        projectWordSet.add(key)

                    if (!projectComWordSet.contains(key))
                        projectComWordSet.add(key)
                }
            }

            it.wordMap.get(KEY_SOURCE)?.forEach {
                if (it.value > thr) {
                    val key = it.key

                    if (!projectWordSet.contains(key))
                        projectWordSet.add(key)

                    if (!projectSrcWordSet.contains(key))
                        projectSrcWordSet.add(key)
                }
            }
            srcLen += it.wordMapSize() / 1000000.0
        }

        projectComWordSet.forEach {
            if (comWordSet.containsKey(it))
                comWordSet.set(it, comWordSet.get(it)!! + 1)
            else
                comWordSet.put(it, 1)
        }

        projectSrcWordSet.forEach {
            if (srcWordSet.containsKey(it))
                srcWordSet.set(it, srcWordSet.get(it)!! + 1)
            else
                srcWordSet.put(it, 1)
        }

        projectWordSet.forEach {
            if (wordSet.containsKey(it))
                wordSet.set(it, wordSet.get(it)!! + 1)
            else
                wordSet.put(it, 1)
        }

        if (srcLen - pSrcLen >= 100) {
            println("add ${srcLen}")
            val preSrcLen = if (srcWordLenList.isEmpty()) 0.0 else srcWordLenList.last()
            val preComLen = if (comWordLenList.isEmpty()) 0.0 else comWordLenList.last()

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

    File(resultDir, "${limit}_srcWordSet_${suffix}.json").printWriter().use {
        it.print(GsonBuilder().setPrettyPrinting().create().toJson(srcWordSet.filter { it.value > limit }.toList().sortedBy { it.second }.reversed()))
    }
    File(resultDir, "${limit}_comWordSet_${suffix}.json").printWriter().use {
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

fun cleanSourceFile(source: SourceFile) {
    source.wordMap.values.forEach {
        val iter = it.iterator()
        while (iter.hasNext()) {
            val value = iter.next()
            val key = value.key

            if (key.length <= 2 || key.length >= 20) {
                iter.remove()
            }
        }
    }
}

fun printBaseInfo() {
    var totalSrcLen: Long = 0
    var totalComLen: Long = 0
    var totalFileCount: Long = 0
    var projectCount = 0
    var totalWordCount: Long = 0

    loadAllProjectModelFile(NAME_PROJECT_MODEL_CLEAN).forEach {
        val project = ProjectModel.load(it);

        project.sourceList.forEach {
            totalFileCount++
            totalComLen += it.comLen
            totalSrcLen += it.srcLen
            totalWordCount += it.wordMapSize()
        }

        ++projectCount
    }

    println("src Len : $totalSrcLen / com Len : $totalComLen / fileCount : $totalFileCount / project Count : $projectCount / wordCount : $totalWordCount")
}

fun cleanProject(root: File) {
    println("[cleanProject]${root.path}")
    root.walkTopDown().filter {
        it.isFile && (it.nameWithoutExtension.length == 1 || (it.nameWithoutExtension.toLowerCase().matches(Regex("r|package-info"))) || (!it.path.contains(".git") && !it.extension.equals("java") && !it.extension.equals("md")))
    }.forEach {
        //        println(it.path)
        it.delete()
    }
}

fun getAllDirectories(root: File): List<File> {
    return root.walk().filter { it.isDirectory }.toList()
}

fun getAllFilesExt(root: File, ext: String): List<File> {
    return root.walk().filter { it.extension.equals(ext) }.toList()
}

data class Source(val path: String, val imports: MutableList<String> = mutableListOf(), val methodCalls: MutableList<String> = mutableListOf())
data class Project(val projectName: String, val sourceList: MutableList<Source> = mutableListOf())

fun normValue(map: HashMap<String, Int>, idfMap: HashMap<String, Long>): Double {
    var norm = 0.0;

    map.filter { idfMap.containsKey(it.key) }.forEach {
        norm += Math.pow(tfidf(it, idfMap), 2.0)
    }

    return 1 / Math.sqrt(norm)
}

fun tfidf(entry: Map.Entry<String, Int>, idfMap: HashMap<String, Long>): Double {
    return (1 + Math.log(entry.value.toDouble())) * Math.log(1 + idfMap.size / idfMap.get(entry.key)!!.toDouble())
}


fun HashMap<String, Int>.tfidfDoc(idfMap: HashMap<String, Long>): HashMap<String, Double> {
    val result: HashMap<String, Double> = hashMapOf()

    this.filter { idfMap.containsKey(it.key) }.forEach {
        result.put(it.key, tfidf(it, idfMap))
    }

    return result;
}

fun createIdfMap(size: Int): HashMap<String, Double> {
    val result: HashMap<String, Double> = hashMapOf()

    run extract@ {
        loadAllProjectModelFile(NAME_PROJECT_MODEL_CLEAN).forEach {
            val project = ProjectModel.load(it);
            var count = 0;
            project.sourceList.filter { it.wordCount() > 20 }.forEach {
                it.getMergedMap()!!.keys.forEach {
                    if (result.containsKey(it))
                        result.set(it, result.get(it)!! + 1)
                    else
                        result.put(it, 1.0);
                }
                count++;
            }

            if (count > size)
                return@extract
        }
    }

    return result
}

fun getNodeList(size: Int, idfMap: HashMap<String, Long>): List<Node> {
    val nodeList: MutableList<Node> = mutableListOf()

    run extract@ {
        loadAllProjectModelFile(NAME_PROJECT_MODEL_CLEAN).forEach {
            val project = ProjectModel.load(it);

            nodeList.addAll(project.sourceList.filter { it.wordCount() > 10 }.map { Node(it.path, it.getMergedMap()!!.tfidfDoc(idfMap)) })

            if (nodeList.size > size)
                return@extract
        }
    }

    println("getNode ${nodeList.size}")

    return nodeList
}


fun kmeanClustering(nodeList: List<Node>) {
    val kmean = Kmeans(Math.sqrt(nodeList.size.toDouble()).toInt())
    kmean.clustering(nodeList)
    Paths.get(PATH_RESULT, "cluster", "kmean_${nodeList.size}.json").toFile().printWriter().use {
        it.print(Gson().toJson(kmean.clusters))
    }
}

fun searchTopK(k: Int = 300, query: File): SortedSet<Pair<String, Double>> {
    val idfMap: HashMap<String, Long> = Gson().fromJson(File("map_idf.json").readText(), object : TypeToken<HashMap<String, Long>>() {}.type);

    val startTime = System.currentTimeMillis()
    println("start Search $startTime / ${idfMap.size}")

    val target = PreProcessor(query.readText()).toSourceFile(query.path)

    val node = Node("query", target.getMergedMap()!!.tfidfDoc(idfMap))

    val result: SortedSet<Pair<String, Double>> = sortedSetOf(compareByDescending { it.second })

    loadAllProjectModelFile(NAME_PROJECT_MODEL_CLEAN).forEach {
        val project = ProjectModel.load(it);

        project.sourceList.map { Node(it.path, it.getMergedMap()!!.tfidfDoc(idfMap)) }.forEach {
            if (result.size < k) {
                result.add(Pair(it.fileName, it.distanceTo(node)))
            } else {
                val last = result.last()
                val distance = it.distanceTo(node)
                if (last.second < distance) {
                    result.remove(last)
                    result.add(Pair(it.fileName, distance))
                }
            }
        }
    }

    println("elapsed milli sec ${System.currentTimeMillis() - startTime}")

    return result
}

fun extractTargetCommit() {
    val target: HashMap<String, Repository> = Gson().fromJson(File("target_map.json").readText(), object : TypeToken<HashMap<String, Repository>>() {}.type)

    target.values.forEach {
        println("extract ${it.toRoot().path}")
        val saveFile = File("D:\\gitExp_gameengine\\${it.full_name.replace("/", "_")}.json")

        if (saveFile.exists())
            return@forEach

        val data = CommitExtractor(it.toRoot().path).extractCommitMetaData(500)

        saveFile.printWriter().use {
            it.print(Gson().toJson(data))
        }
    }
}

fun searchInClusters(clusters: List<Cluster>, node: Node) {
    var matchDis = Double.MIN_VALUE
    var matchIdx = 0
    val startTime = System.currentTimeMillis()

    clusters.forEachIndexed { index, cluster ->
        val dis = node.distanceTo(cluster.getCentroid())

        if (dis > matchDis) {
            matchDis = dis;
            matchIdx = index
        }
    }

    val result: SortedSet<Pair<String, Double>> = sortedSetOf(compareByDescending { it.second })

    val selectedCluster = clusters.get(matchIdx)

    selectedCluster.memberList.forEach {
        val current = selectedCluster.getNodes().get(it)
        if (result.size < 100) {
            result.add(Pair(current.fileName, current.distanceTo(node)))
        } else {
            val last = result.last()
            val distance = current.distanceTo(node)
            if (last.second < distance) {
                result.remove(last)
                result.add(Pair(current.fileName, distance))
            }
        }
    }

    println("elapsed : ${System.currentTimeMillis() - startTime}")

    Paths.get(PATH_RESULT, "searchClusters_${node.fileName}.json").toFile().printWriter().use {
        it.print(GsonBuilder().setPrettyPrinting().create().toJson(result))
    }
}

fun searchInNode(nodes: List<Node>, query: Node) {
    val result: SortedSet<Pair<String, Double>> = sortedSetOf(compareByDescending { it.second })
    val startTime = System.currentTimeMillis()

    nodes.forEach {
        if (result.size < 100) {
            result.add(Pair(it.fileName, it.distanceTo(query)))
        } else {
            val last = result.last()
            val distance = it.distanceTo(query)
            if (last.second < distance) {
                result.remove(last)
                result.add(Pair(it.fileName, distance))
            }
        }
    }

    println("elapsed : ${System.currentTimeMillis() - startTime}")

    Paths.get(PATH_RESULT, "searchSeq_${query.fileName}.json").toFile().printWriter().use {
        it.print(GsonBuilder().setPrettyPrinting().create().toJson(result))
    }
}

fun mbToKB(mb: Int): Int = mb * 1024;

fun cleanAllProjectModels() {
    loadAllProjectModelFile(NAME_PROJECT_MODEL).forEach {
        val p = ProjectModel.load(it)

        p.sourceList.parallelStream().forEach {
            cleanSourceFile(it)
        }

        val regex = Regex("r.java|package-info.java|test")
        p.sourceList = p.sourceList.filter { it.wordCount() > 10 && !regex.containsMatchIn(it.path.toLowerCase()) }.toMutableList()

        val saveFile = File(it.path.replace(NAME_PROJECT_MODEL, NAME_PROJECT_MODEL_CLEAN))

        if (saveFile.exists())
            saveFile.delete()

        if (p.sourceList.size > 0) {
            println("saved ${saveFile.path}")
            saveFile.printWriter().use {
                it.print(Gson().toJson(p))
            }
        }
    }
}


val regCamelCase = Regex(String.format("%s|%s|%s",
        "(?<=[A-Z])(?=[A-Z][a-z])(\\B)",
        "(?<=[^A-Z])(?=[A-Z])(\\B)",
        "(?<=[A-Za-z])(?=[^A-Za-z])(\\B)"
))

fun similarity(a: String, b: String): Double {
    val aSet = regCamelCase.replace(a, " ").toLowerCase().split(" ").toHashSet()
    val bSet = regCamelCase.replace(b, " ").toLowerCase().split(" ").toHashSet()

    val union: HashSet<String> = hashSetOf()
    union.addAll(aSet)
    union.addAll(bSet)

    aSet.retainAll(bSet) // 교집합

    val inter = aSet.size

    return inter.toDouble() / union.size
}

fun expandableScore(querySet: List<String>, resultSet: List<String>, threshold: Double = 0.35): Int {
    var count = 0

    resultSet.forEach { current ->
        var max = Double.MIN_VALUE

        querySet.forEach {
            val sim = similarity(current, it)

            if (sim > max)
                max = sim;
        }

        if (max <= threshold)
            count++
    }

    return count
}

fun methodSim(querySet: List<String>, resultSet: List<String>): Double {
    var totalMax = 0.0

    querySet.forEach { current ->
        var max = 0.0

        resultSet.forEach {
            val sim = similarity(current, it)

//            println("$current / $it = $sim")

            if (sim > max)
                max = sim;
        }

        totalMax += max;
    }

    return totalMax / querySet.size.toDouble()
}

fun analysisNew2(filterSet:Set<String>) {
    val importsMap: HashMap<String, Int> = hashMapOf()
    val cmtClassMap: HashMap<String, Int> = hashMapOf()
    val cmtMethodMap: HashMap<String, Int> = hashMapOf()
    val cmtVarMap: HashMap<String, Int> = hashMapOf()
    val nameParamMap: HashMap<String, Int> = hashMapOf()
    val nameMethodMap: HashMap<String, Int> = hashMapOf()
    val nameClassMap: HashMap<String, Int> = hashMapOf()
    val nameVarMap: HashMap<String, Int> = hashMapOf()
    val typeMethodMap: HashMap<String, Int> = hashMapOf()
    val typeParamMap: HashMap<String, Int> = hashMapOf()
    val typeVarMap: HashMap<String, Int> = hashMapOf()

    loadAllProjectModelFile(NAME_PROJECT).forEach {
        val project = Project.load(it)
        project.sourceList.forEach {

            it.commentsClassOrInterface.filterKeys { filterSet.contains(it) }.forEach {
                cmtClassMap.increase(it.key, it.value)
            }

            it.commentsMethod.filterKeys { filterSet.contains(it) }.forEach {
                cmtMethodMap.increase(it.key, it.value)
            }

            it.commentsVariable.filterKeys { filterSet.contains(it) }.forEach {
                cmtVarMap.increase(it.key, it.value)
            }

            it.imports.filterKeys { filterSet.contains(it) }.forEach {
                importsMap.increase(it.key, it.value)
            }

            it.nameClassOrInterface.filterKeys { filterSet.contains(it) }.forEach {
                nameClassMap.increase(it.key, it.value)
            }

            it.nameMethod.filterKeys { filterSet.contains(it) }.forEach {
                nameMethodMap.increase(it.key, it.value)
            }

            it.nameVariable.filterKeys { filterSet.contains(it) }.forEach {
                nameVarMap.increase(it.key, it.value)
            }

            it.nameParameter.filterKeys { filterSet.contains(it) }.forEach {
                nameParamMap.increase(it.key, it.value)
            }

            it.typeMethod.filterKeys { filterSet.contains(it) }.forEach {
                typeMethodMap.increase(it.key, it.value)
            }

            it.typeParameter.filterKeys { filterSet.contains(it) }.forEach {
                typeParamMap.increase(it.key, it.value)
            }

            it.typeVariable.filterKeys { filterSet.contains(it) }.forEach {
                typeVarMap.increase(it.key, it.value)
            }
        }
    }

    println("${importsMap.size}/${importsMap.values.sum()},${cmtClassMap.size}/${cmtClassMap.values.sum()}, ${cmtVarMap.size}/${cmtVarMap.values.sum()}, ${cmtMethodMap.size}/${cmtMethodMap.values.sum()}")
    println("${nameClassMap.size}/${nameClassMap.values.sum()}, ${nameMethodMap.size}/${nameMethodMap.values.sum()}, ${nameParamMap.size}/${nameParamMap.values.sum()}, ${nameVarMap.size}/${nameVarMap.values.sum()}")
    println("${typeMethodMap.size}/${typeMethodMap.values.sum()}, ${typeParamMap.size}/${typeParamMap.values.sum()}, ${typeVarMap.size}/${typeVarMap.values.sum()}")
}

fun test() {
    var count = 0
    var idfMap = CountMap()
    var dfMap = CountMap()
    var ndfMap = CountMap()
    var tdfMap = CountMap()

    loadAllProjectModelFile(NAME_PROJECT).forEach {
        val project = Project.load(it)
        val iset : MutableSet<String> = mutableSetOf()
        val set: MutableSet<String> = mutableSetOf()
        val nset: MutableSet<String> = mutableSetOf()
        val tset: MutableSet<String> = mutableSetOf()

        project.sourceList.forEach {
            iset.addAll(it.imports.keys)

            set.addAll(it.commentsClassOrInterface.keys)
            set.addAll(it.commentsMethod.keys)
            set.addAll(it.commentsVariable.keys)

            nset.addAll(it.nameVariable.keys)
            nset.addAll(it.nameParameter.keys)
            nset.addAll(it.nameMethod.keys)
            nset.addAll(it.nameClassOrInterface.keys)
//
            tset.addAll(it.typeMethod.keys)
            tset.addAll(it.typeParameter.keys)
            tset.addAll(it.typeVariable.keys)
        }

        ++count


        iset.forEach {
            idfMap.put(it)
        }

        set.forEach {
            dfMap.put(it)
        }

        nset.forEach {
            ndfMap.put(it)
        }

        tset.forEach {
            tdfMap.put(it)
        }
    }

    println("$count / ${idfMap.size} / ${dfMap.size} / ${tdfMap.size} / ${ndfMap.size}")

    File("res_imports.json").printWriter().use {
        it.print(GsonBuilder().setPrettyPrinting().create().toJson(idfMap.toList().sortedByDescending { it.second }))
    }

    File("res_comment.json").printWriter().use {
        it.print(GsonBuilder().setPrettyPrinting().create().toJson(dfMap.toList().sortedByDescending { it.second }))
    }

    File("res_type.json").printWriter().use {
        it.print(GsonBuilder().setPrettyPrinting().create().toJson(tdfMap.toList().sortedByDescending { it.second }))
    }

    File("res_name.json").printWriter().use {
        it.print(GsonBuilder().setPrettyPrinting().create().toJson(ndfMap.toList().sortedByDescending { it.second }))
    }
}

fun analysisNew3() {
    val importsMap= CountMap()
    val cmtClassMap= CountMap()
    val cmtMethodMap= CountMap()
    val cmtVarMap= CountMap()
    val nameParamMap= CountMap()
    val nameMethodMap= CountMap()
    val nameClassMap= CountMap()
    val nameVarMap= CountMap()
    val typeMethodMap= CountMap()
    val typeParamMap= CountMap()
    val typeVarMap= CountMap()

    val srcLenList: MutableList<Int> = mutableListOf()
    val wsCmtList1: MutableList<Double> = mutableListOf()
    val wsCmtList2: MutableList<Double> = mutableListOf()
    val wsCmtList3: MutableList<Double> = mutableListOf()
    val wsNameList1: MutableList<Double> = mutableListOf()
    val wsNameList2: MutableList<Double> = mutableListOf()
    val wsNameList3: MutableList<Double> = mutableListOf()
    val wsNameList4: MutableList<Double> = mutableListOf()
    val wsTypeList1: MutableList<Double> = mutableListOf()
    val wsTypeList2: MutableList<Double> = mutableListOf()
    val wsTypeList3: MutableList<Double> = mutableListOf()
    val wsImportList: MutableList<Double> = mutableListOf()

    var pre = 0
    var current = 0
    val step = 400000
    val limit = 100
    var count = 0

    val pfMap = CountMap()

    loadAllProjectModelFile(NAME_PROJECT).forEach {
        val project = Project.load(it)
        val importsSet : MutableSet<String> = mutableSetOf()
        val cmtClassSet : MutableSet<String> = mutableSetOf()
        val cmtVarSet : MutableSet<String> = mutableSetOf()
        val cmtMethodSet : MutableSet<String> = mutableSetOf()
        val nameClassSet : MutableSet<String> = mutableSetOf()
        val nameParamSet : MutableSet<String> = mutableSetOf()
        val nameMethodSet : MutableSet<String> = mutableSetOf()
        val nameVarSet : MutableSet<String> = mutableSetOf()
        val typeMethodSet : MutableSet<String> = mutableSetOf()
        val typeParamSet : MutableSet<String> = mutableSetOf()
        val typeVarSet : MutableSet<String> = mutableSetOf()

        ++count

        project.sourceList.forEach {
            importsSet.addAll(it.imports.keys)
            cmtClassSet.addAll(it.commentsClassOrInterface.keys)
            cmtVarSet.addAll(it.commentsVariable.keys)
            cmtMethodSet.addAll(it.commentsMethod.keys)
            nameParamSet.addAll(it.nameParameter.keys)
            nameMethodSet.addAll(it.nameMethod.keys)
            nameVarSet.addAll(it.nameVariable.keys)
            nameClassSet.addAll(it.nameClassOrInterface.keys)
            typeMethodSet.addAll(it.typeMethod.keys)
            typeParamSet.addAll(it.typeParameter.keys)
            typeVarSet.addAll(it.typeVariable.keys)

            ++current

            if (current - pre > step) {
                pre = current
                srcLenList.add(current / 1000)
                wsCmtList1.add(cmtClassMap.filter { it.value > limit }.size / 1000.0)
                wsCmtList2.add(cmtMethodMap.filter { it.value  > limit }.size / 1000.0)
                wsCmtList3.add(cmtVarMap.filter { it.value  > limit }.size / 1000.0)
                wsNameList1.add(nameClassMap.filter { it.value  > limit }.size / 1000.0)
                wsNameList2.add(nameMethodMap.filter { it.value  > limit }.size / 1000.0)
                wsNameList3.add(nameParamMap.filter { it.value  > limit }.size / 1000.0)
                wsNameList4.add(nameVarMap.filter { it.value  > limit }.size / 1000.0)
                wsTypeList1.add(typeMethodMap.filter { it.value  > limit }.size / 1000.0)
                wsTypeList2.add(typeParamMap.filter { it.value  > limit }.size / 1000.0)
                wsTypeList3.add(typeVarMap.filter { it.value  > limit }.size / 1000.0)
                wsImportList.add(importsMap.filter { it.value  > limit }.size / 1000.0)
            }
        }
        importsSet.forEach {
            importsMap.put(it)
        }
        cmtClassSet.forEach {
            cmtClassMap.put(it)
        }
        cmtVarSet.forEach {
            cmtVarMap.put(it)
        }
        cmtMethodSet.forEach {
            cmtMethodMap.put(it)
        }
        nameParamSet.forEach {
            nameParamMap.put(it)
        }
        nameMethodSet.forEach {
            nameMethodMap.put(it)
        }
        nameVarSet.forEach {
            nameVarMap.put(it)
        }
        nameClassSet.forEach {
            nameClassMap.put(it)
        }
        typeMethodSet.forEach {
            typeMethodMap.put(it)
        }
        typeParamSet.forEach {
            typeParamMap.put(it)
        }
        typeVarSet.forEach {
            typeVarMap.put(it)
        }
    }

    println(count)

    if (current != pre) {
        pre = current
        srcLenList.add(current / 1000)

        wsCmtList1.add(cmtClassMap.filter { it.value > limit }.size / 1000.0)
        wsCmtList2.add(cmtMethodMap.filter { it.value  > limit }.size / 1000.0)
        wsCmtList3.add(cmtVarMap.filter { it.value  > limit }.size / 1000.0)
        wsNameList1.add(nameClassMap.filter { it.value  > limit }.size / 1000.0)
        wsNameList2.add(nameMethodMap.filter { it.value  > limit }.size / 1000.0)
        wsNameList3.add(nameParamMap.filter { it.value  > limit }.size / 1000.0)
        wsNameList4.add(nameVarMap.filter { it.value  > limit }.size / 1000.0)
        wsTypeList1.add(typeMethodMap.filter { it.value  > limit }.size / 1000.0)
        wsTypeList2.add(typeParamMap.filter { it.value  > limit }.size / 1000.0)
        wsTypeList3.add(typeVarMap.filter { it.value  > limit }.size / 1000.0)
        wsImportList.add(importsMap.filter { it.value  > limit }.size / 1000.0)
    }

    val chart = XYChartBuilder().width(800).height(600).title("Title")
            .xAxisTitle("#source file (k)")
            .yAxisTitle("size of wordset (k)").theme(Styler.ChartTheme.Matlab).build()
    chart.styler.setMarkerSize(5)
    chart.styler.setLegendPosition(Styler.LegendPosition.InsideNW)
    chart.styler.setChartTitleVisible(false)

    chart.addSeries("comment_class", srcLenList, wsCmtList1)
    chart.addSeries("comment_method", srcLenList, wsCmtList2)
    chart.addSeries("comment_var", srcLenList, wsCmtList3)

    SwingWrapper(chart).displayChart()

    BitmapEncoder.saveBitmapWithDPI(chart, Paths.get(PATH_RESULT, "new_analysis_1.png").toString(), BitmapEncoder.BitmapFormat.PNG, 200);

    val chart2 = XYChartBuilder().width(800).height(600).title("Title")
            .xAxisTitle("#source file (k)")
            .yAxisTitle("size of wordset (k)").theme(Styler.ChartTheme.Matlab).build()
    chart2.styler.setMarkerSize(5)
    chart2.styler.setLegendPosition(Styler.LegendPosition.InsideNW)
    chart2.styler.setChartTitleVisible(false)

    chart2.addSeries("name_class", srcLenList, wsNameList1)
    chart2.addSeries("name_method", srcLenList, wsNameList2)
    chart2.addSeries("name_param", srcLenList, wsNameList3)
    chart2.addSeries("name_value", srcLenList, wsNameList4)
    SwingWrapper(chart2).displayChart()

    BitmapEncoder.saveBitmapWithDPI(chart2, Paths.get(PATH_RESULT, "new_analysis_2.png").toString(), BitmapEncoder.BitmapFormat.PNG, 200);

    val chart3 = XYChartBuilder().width(800).height(600).title("Title")
            .xAxisTitle("#source file (k)")
            .yAxisTitle("size of wordset (k)").theme(Styler.ChartTheme.Matlab).build()
    chart3.styler.setMarkerSize(5)
    chart3.styler.setLegendPosition(Styler.LegendPosition.InsideNW)
    chart3.styler.setChartTitleVisible(false)

    chart3.addSeries("type_method", srcLenList, wsTypeList1)
    chart3.addSeries("type_param", srcLenList, wsTypeList2)
    chart3.addSeries("type_value", srcLenList, wsTypeList3)

    SwingWrapper(chart3).displayChart()

    BitmapEncoder.saveBitmapWithDPI(chart, Paths.get(PATH_RESULT, "new_analysis_3.png").toString(), BitmapEncoder.BitmapFormat.PNG, 200);

    val chart4 = XYChartBuilder().width(800).height(600).title("Title")
            .xAxisTitle("#source file (k)")
            .yAxisTitle("size of wordset (k)").theme(Styler.ChartTheme.Matlab).build()
    chart4.styler.setMarkerSize(5)
    chart4.styler.setLegendPosition(Styler.LegendPosition.InsideNW)
    chart4.styler.setChartTitleVisible(false)

    chart4.addSeries("import", srcLenList, wsImportList)

    SwingWrapper(chart4).displayChart()

    BitmapEncoder.saveBitmapWithDPI(chart, Paths.get(PATH_RESULT, "new_analysis_3.png").toString(), BitmapEncoder.BitmapFormat.PNG, 200);
}

fun analysisNew() {
    val importsMap: HashMap<String, Int> = hashMapOf()
    val cmtClassMap: HashMap<String, Int> = hashMapOf()
    val cmtMethodMap: HashMap<String, Int> = hashMapOf()
    val cmtVarMap: HashMap<String, Int> = hashMapOf()
    val nameParamMap: HashMap<String, Int> = hashMapOf()
    val nameMethodMap: HashMap<String, Int> = hashMapOf()
    val nameClassMap: HashMap<String, Int> = hashMapOf()
    val nameVarMap: HashMap<String, Int> = hashMapOf()
    val typeMethodMap: HashMap<String, Int> = hashMapOf()
    val typeParamMap: HashMap<String, Int> = hashMapOf()
    val typeVarMap: HashMap<String, Int> = hashMapOf()

    val srcLenList: MutableList<Int> = mutableListOf()
    val wsCmtList: MutableList<Double> = mutableListOf()
    val wsNameList: MutableList<Double> = mutableListOf()
    val wsTypeList: MutableList<Double> = mutableListOf()
    val wsImportList: MutableList<Double> = mutableListOf()

    var pre = 0
    var current = 0
    val step = 450000
    val limit = 0
    val dfMap = CountMap()

    loadAllProjectModelFile(NAME_PROJECT).forEach {
        val project = Project.load(it)
        val set: MutableSet<String> = mutableSetOf()

        project.sourceList.forEach {
            set.addAll(it.commentsClassOrInterface.keys)
            it.commentsClassOrInterface.forEach {
                cmtClassMap.increase(it.key, it.value)
            }
            set.addAll(it.commentsMethod.keys)
            it.commentsMethod.forEach {
                cmtMethodMap.increase(it.key, it.value)
            }
            set.addAll(it.commentsVariable.keys)
            it.commentsVariable.forEach {
                cmtVarMap.increase(it.key, it.value)
            }
            set.addAll(it.imports.keys)
            it.imports.forEach {
                importsMap.increase(it.key, it.value)
            }
            set.addAll(it.nameClassOrInterface.keys)
            it.nameClassOrInterface.forEach {
                nameClassMap.increase(it.key, it.value)
            }
            set.addAll(it.nameMethod.keys)
            it.nameMethod.forEach {
                nameMethodMap.increase(it.key, it.value)
            }
            set.addAll(it.nameVariable.keys)
            it.nameVariable.forEach {
                nameVarMap.increase(it.key, it.value)
            }
            set.addAll(it.nameParameter.keys)
            it.nameParameter.forEach {
                nameParamMap.increase(it.key, it.value)
            }
            set.addAll(it.typeMethod.keys)
            it.typeMethod.forEach {
                typeMethodMap.increase(it.key, it.value)
            }
            set.addAll(it.typeParameter.keys)
            it.typeParameter.forEach {
                typeParamMap.increase(it.key, it.value)
            }
            set.addAll(it.typeVariable.keys)
            it.typeVariable.forEach {
                typeVarMap.increase(it.key, it.value)
            }

            ++current

            if (current - pre > step) {
                pre = current
                srcLenList.add(current / 1000)
                wsCmtList.add((cmtClassMap.filter { dfMap.getOrDefault(it.key, 0) > limit }.size + cmtMethodMap.filter { dfMap.getOrDefault(it.key, 0) > limit }.size + cmtVarMap.filter { dfMap.getOrDefault(it.key, 0) > limit }.size) / 1000.0)
                wsNameList.add((nameClassMap.filter { dfMap.getOrDefault(it.key, 0) > limit }.size + nameMethodMap.size + nameParamMap.filter { dfMap.getOrDefault(it.key, 0) > limit }.size + nameVarMap.filter { dfMap.getOrDefault(it.key, 0) > limit }.size) / 1000.0)
                wsTypeList.add((typeMethodMap.filter { dfMap.getOrDefault(it.key, 0) > limit }.size + typeParamMap.filter { dfMap.getOrDefault(it.key, 0) > limit }.size + typeVarMap.filter { dfMap.getOrDefault(it.key, 0) > limit }.size) / 1000.0)
                wsImportList.add(importsMap.filter { dfMap.getOrDefault(it.key, 0) > limit }.size / 1000.0)
            }
        }

        println(set.size)
        set.forEach {
            dfMap.put(it)
        }
    }

    if (current != pre) {
        pre = current
        srcLenList.add(current / 1000)

        wsCmtList.add((cmtClassMap.filter { dfMap.getOrDefault(it.key, 0) > limit }.size + cmtMethodMap.filter { dfMap.getOrDefault(it.key, 0) > limit }.size + cmtVarMap.filter { dfMap.getOrDefault(it.key, 0) > limit }.size) / 1000.0)
        wsNameList.add((nameClassMap.filter { dfMap.getOrDefault(it.key, 0) > limit }.size + nameMethodMap.size + nameParamMap.filter { dfMap.getOrDefault(it.key, 0) > limit }.size + nameVarMap.filter { dfMap.getOrDefault(it.key, 0) > limit }.size) / 1000.0)
        wsTypeList.add((typeMethodMap.filter { dfMap.getOrDefault(it.key, 0) > limit }.size + typeParamMap.filter { dfMap.getOrDefault(it.key, 0) > limit }.size + typeVarMap.filter { dfMap.getOrDefault(it.key, 0) > limit }.size) / 1000.0)
        wsImportList.add(importsMap.filter { dfMap.getOrDefault(it.key, 0) > limit }.size / 1000.0)
    }

    val chart = XYChartBuilder().width(800).height(600).title("Title")
            .xAxisTitle("#source file (k)")
            .yAxisTitle("size of wordset (k)").theme(Styler.ChartTheme.Matlab).build()
    chart.styler.setMarkerSize(5)
    chart.styler.setLegendPosition(Styler.LegendPosition.InsideNW)
    chart.styler.setChartTitleVisible(false)

    chart.addSeries("comment", srcLenList, wsCmtList)
    chart.addSeries("type", srcLenList, wsTypeList)
    chart.addSeries("name", srcLenList, wsNameList)
    chart.addSeries("import", srcLenList, wsImportList)

    SwingWrapper(chart).displayChart()

    BitmapEncoder.saveBitmapWithDPI(chart, Paths.get(PATH_RESULT, "new_analysis.png").toString(), BitmapEncoder.BitmapFormat.PNG, 200);
}


val T1 = "E:\\Repository\\spring-projects\\spring-framework\\spring-core\\src\\main\\java\\org\\springframework\\util\\StringUtils.java" // 51
val T2 = "E:\\Repository\\zxing\\zxing\\core\\src\\main\\java\\com\\google\\zxing\\common\\detector\\MathUtils.java"
val T3 = "E:\\Repository\\zyuanming\\MusicMan\\src\\org\\ming\\util\\FileUtils.java" // 20
val T4 = "E:\\Repository\\spring-projects\\spring-framework\\spring-core\\src\\main\\java\\org\\springframework\\util\\Base64Utils.java"
val T5 = "E:\\Repository\\spring-projects\\spring-framework\\spring-core\\src\\main\\java\\org\\springframework\\util\\SocketUtils.java"
val T6 = "E:\\Repository\\google\\guava\\guava\\src\\com\\google\\common\\base\\CharMatcher.java"

fun HashMap<String, Int>.increase(key: String, value: Int) {
    if (containsKey(key))
        set(key, get(key)!! + value)
    else
        put(key, value)
}

fun main(args: Array<String>) {

    return

    var dfMap : Map<String, Int> = Gson().fromJson(File("new_df_map.json").readText(), object:TypeToken<Map<String, Double>>() {}.type)
    val filterSet = dfMap.filter { it.value > 29 && it.value < 600000 }.keys
    loadAllProjectModelFile(NAME_PROJECT).forEach {
        try {
            val project = Project.load(it);

            if(project.sourceList.isEmpty())
            {
                it.delete()
                return
            }

            project.sourceList.forEach {
                it.imports.keys.removeIf{ !filterSet.contains(it) }
                it.typeParameter.keys.removeIf{ !filterSet.contains(it) }
                it.typeVariable.keys.removeIf{ !filterSet.contains(it) }
                it.typeMethod.keys.removeIf{ !filterSet.contains(it) }
                it.nameVariable.keys.removeIf{ !filterSet.contains(it) }
                it.nameParameter.keys.removeIf{ !filterSet.contains(it) }
                it.nameMethod.keys.removeIf{ !filterSet.contains(it) }
                it.nameClassOrInterface.keys.removeIf{ !filterSet.contains(it) }
                it.commentsMethod.keys.removeIf{ !filterSet.contains(it) }
                it.commentsClassOrInterface.keys.removeIf{ !filterSet.contains(it) }
                it.commentsVariable.keys.removeIf{ !filterSet.contains(it) }
            }

            File(it.path.replace(NAME_PROJECT, NAME_PROJECT_FILTERED)).printWriter().use {
                it.print(Gson().toJson(project))
            }
        }
        catch (e:Exception)
        {
            println("delete ${it.path}")
        }
    }
    return

//    dfMap.filterValues { it > 19 && it < 500000}

//    val analyst = SourceAnalyst(File(T2))
//    analyst.analysis()
//    println(GsonBuilder().setPrettyPrinting().create().toJson( analyst.sourceFile))
//    return
//    analysisNew3()

    return


//    println(GsonBuilder().setPrettyPrinting().create().toJson(newdata.SourceFile.create(File(T6))))


//    val dfMap : HashMap<String, Long> = Gson().fromJson(File("df_map.json").readText(), object:TypeToken<HashMap<String, Double>>() {}.type)
//    val idfMap : HashMap<String, Long> = hashMapOf()
//    idfMap.putAll(dfMap.filter { it.value > 19 && it.value < 600000 })
//
//    searchTest(File(T6), Paths.get(PATH_RESULT,"t6_org.json").toFile(), dfMap);
//    searchTest(File(T6), Paths.get(PATH_RESULT,"t6_filter.json").toFile(), idfMap);
//    val qp = JavaParser.parse(File(T6))
////
////    qp.findAll(MethodDeclaration::class.java).forEach {
////        println(it.body)
////    }
//
//    qp.findAll(ClassOrInterfaceDeclaration::class.java).forEach {
//        println(it.comment.get())
//    }
//
//    qp.findAll(ConstructorDeclaration::class.java).forEach {
//        println("${it.name} : ${it.parameters}")
//    }
//
//    qp.findAll(MethodDeclaration::class.java).forEach {
//        println("${it.name} : ${it.parameters}")
//    }

    return
    val target = T6
    val searchMap: Set<Pair<String, Double>> = Gson().fromJson(Paths.get(PATH_RESULT, "t5_filter.json").toFile().readText(), object : TypeToken<Set<Pair<String, Double>>>() {}.type)
//    val origin : Set<Pair<String, Double>> =  Gson().fromJson(Paths.get(PATH_RESULT, "t1_search_org_result.json").toFile().readText(), object:TypeToken<Set<Pair<String,Double>>> () {}.type)

    val queryParser = JavaParser.parse(File(target))
    val set = queryParser.findAll(MethodDeclaration::class.java).map { it.name.toString() }.toHashSet()

    var maxES = Int.MIN_VALUE
    var minES = Int.MAX_VALUE
    var totalES = 0

    var maxMas = Double.MIN_VALUE
    var minMas = Double.MAX_VALUE
    var totalMas = 0.0
    var size = 50;

    println(set.size)
    println(set)
    return

    searchMap.filter { !it.first.equals(target) }.take(50).forEach {
        try {
            val parser = JavaParser.parse(File(it.first))
            val currentSet = parser.findAll(MethodDeclaration::class.java).map { it.name.toString() }.toSet()

            var es = expandableScore(set.toList(), currentSet.toList())
            val ms = methodSim(set.toList(), currentSet.toList())

            println("${it.first} es : $es")

            if (es > maxES)
                maxES = es

            if (es < minES)
                minES = es

            if (ms > maxMas)
                maxMas = ms

            if (ms < minMas)
                minMas = ms

            totalMas += ms
            totalES += es
        } catch (e: Exception) {
            size--
            println("[error]${it.first}")
            return@forEach
        }

    }

    println("$minES / $maxES / ${totalES / size.toDouble()} / $minMas / $maxMas / ${totalMas / size}")
    return
    val pfMap: HashMap<String, Long> = Gson().fromJson(File("pf_map.json").readText(), object : TypeToken<HashMap<String, Long>>() {}.type)




    return
    analysis()
    return
    printBaseInfo()
    return
    analysis1(limit = 0, suffix = "new")
    analysis1(limit = 10, suffix = "new")
    analysis1(limit = 100, suffix = "new")
    return
    printBaseInfo()


    return
    File("E:/Repository").listFiles().map { it.listFiles() }.forEach {
        it.forEach {
            val project = ProjectModel.loadOrCreate(it);
        }
    }
    return
    printAllCloneCommand()
    return
    val c = Calendar.getInstance()
    val format = SimpleDateFormat("yyyy-MM-dd")

    for (year in 2008..2017) {
        c.set(Calendar.YEAR, year)

        for (day in 1..358 step 7) {
            c.set(Calendar.DAY_OF_YEAR, day)
            val start = format.format(c.time)
            c.add(Calendar.DAY_OF_YEAR, +7)
            val end = format.format(c.time)

            ProjectExtractor(Paths.get(PATH_PROJECT_MAP), start, end).extract();
        }

//        for(month in 1..12)
//        {
//            for(week in 1..5) {
//                val ws = (week-1)*7 + 1;
//                val we = week*7 + 1;
//                val c = Calendar.getInstance()
//                c.set(Calendar.YEAR, 2014)
//                c.set(Calendar.MONTH, 1)
//                c.set(Calendar.DAY_OF_YEAR, i)
//                ProjectExtractor(Paths.get(PATH_PROJECT_MAP), "$year-$month-$ws", "$year-$month-$we").extract();
//            }
//        }
    }
    return

    File("E:/Repository").listFiles().map { it.listFiles() }.forEach {
        it.forEach {
            cleanProject(it)
        }
    }
    return

    printAllCloneCommand()
    return

//    printBaseInfo()
//    return
//
//    val idfMap : HashMap<String, Double> = Gson().fromJson(File("map_idf.json").readText(), object:TypeToken<HashMap<String, Double>>() {}.type)
//    val size = 50000
//    kmeanClustering(300000, idfMap, getNodeList(size, idfMap))

//    val idfMap : HashMap<String, Double> = Gson().fromJson(File("map_idf.json").readText(), object:TypeToken<HashMap<String, Double>>() {}.type);
//    val query = File("C:\\Research\\Repository\\zxing\\zxing\\core\\src\\main\\java\\com\\google\\zxing\\common\\detector\\MathUtils.java")
//    val target = PreProcessor(query.readText()).toSourceFile(query.path)
//
//    val node = Node("query" , target.getMergedMap()!!.tfidfDoc(idfMap))
//
//    val clusters : List<Cluster> = Gson().fromJson(Paths.get(PATH_RESULT, "cluster", "kmean_50319.json").toFile().readText(), object:TypeToken<List<Cluster>>() {}.type);
//    val nodeList  = getNodeList(50000, idfMap)
//
//    searchInClusters(clusters, node)
//    searchInNode(nodeList, node)
//    return

//    val size = 100000
//    kmeanClustering(getNodeList(300000, idfMap))
//    kmeanClustering(getNodeList(500000, idfMap))
//    return

//    val clusters : List<Cluster> = Gson().fromJson(Paths.get(PATH_RESULT, "cluster", "kmean_101418.json").toFile().readText(), object:TypeToken<List<Cluster>>() {}.type);
//    val nodeList  = getNodeList(size, idfMap)
//
//    val query = File(T1)
//    val target = PreProcessor(query.readText()).toSourceFile(query.path)
//    val node = Node(query.nameWithoutExtension , target.getMergedMap()!!.tfidfDoc(idfMap))
//
//    searchInClusters(clusters, node)
//    searchInNode(nodeList, node)
//
//    return

//    kmeanClustering(50000, idfMap);
//    kmeanClustering(100000, idfMap);
//    kmeanClustering(150000, idfMap);
    return
//
//    val s = searchTopK(query = File("C:\\Research\\Repository\\spring-projects\\spring-framework\\spring-core\\src\\main\\java\\org\\springframework\\util\\StringUtils.java"))
//    Paths.get(PATH_RESULT, "search_top300_tfidf.json").toFile().printWriter().use {
//        it.print(GsonBuilder().setPrettyPrinting().create().toJson(s))
//    }
    return
//    val idfMap:HashMap<String, Double> = hashMapOf()
//
//    loadAllProjectModelFile(NAME_PROJECT_MODEL_CLEAN).forEach {
//        val project = ProjectModel.load(it)
//
//        project.sourceList.forEach {
//            it.getMergedMap()!!.keys.forEach {
//                if(idfMap.containsKey(it))
//                    idfMap.set(it, idfMap.get(it)!! + 1)
//                else
//                    idfMap.put(it, 1.0)
//            }
//        }
//    }
//
//    File("map_idf.json").printWriter().use {
//        it.print(GsonBuilder().setPrettyPrinting().create().toJson(idfMap))
//    }
//
//    return
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




