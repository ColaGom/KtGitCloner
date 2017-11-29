package vop

import com.github.javaparser.JavaParser
import com.github.javaparser.ast.body.MethodDeclaration
import com.google.gson.GsonBuilder
import common.FileUtils
import common.PATH_DATA
import common.Stopwords
import nlp.PreProcessor
import opennlp.tools.cmdline.postag.POSModelLoader
import opennlp.tools.postag.POSTaggerME
import org.tartarus.snowball.ext.englishStemmer
import vop.DataManager.Companion.toPairList
import java.io.File
import java.nio.file.Paths

class DataManager {
    companion object {
        val model = POSModelLoader().load(File("nlp/en-pos-maxent.bin"))
        val tagger = POSTaggerME(model)
        val pairBound = 2
        val stemmer = englishStemmer()

        fun create(root: File) {
            val saveFile = Paths.get(PATH_DATA, "vop",
                    root.path.substringAfter("\\Repository").replace("\\", "_") + ".json").toFile();

            val srcList: MutableList<Source> = mutableListOf()

            FileUtils.readAllFilesExt(root, "java").forEach {
                try {
                    srcList.add(Source(it.path, it.toPairList()))
                } catch (e: Exception) {
                    print(e)
                }
            }

            val p = Project(root.path, srcList)

            saveFile.printWriter().use {
                it.print(GsonBuilder().setPrettyPrinting().create().toJson(p))
            }
        }

        fun load() {

        }

        fun String.preprocessing() : Array<String>
        {
            var str = PreProcessor.regCamelCase.replace(this," ")
            str = PreProcessor.regHtml.replace(str, "")
            str = com.sun.deploy.util.StringUtils.trimWhitespace(str)

            return PreProcessor.regNonAlphanum.split(str.toLowerCase()).filter { it.length > 2 && it.length < 20 && !Stopwords.instance.contains(it.toLowerCase()) }.toTypedArray()
        }

        fun String.toPairList(): List<Pair> {
            val list: MutableList<Pair> = mutableListOf()

            val words = this.preprocessing()
            val tags = tagger.tag(words)
            val probs = tagger.probs()

            var vIdx = -1
            var maxProbs = Double.MIN_VALUE

            tags.forEachIndexed { index, s ->
                if(s.startsWith("VB")) {
                    if(probs[index] > maxProbs)
                    {
                        maxProbs = probs[index]
                        vIdx = index
                    }
                }
            }

            if(vIdx != -1)
            {
                val index = vIdx
                val s = words[vIdx]

                if(index - 2 >= 0)
                {
                    list.add(Pair(s, tags[index-2] +" "+tags[index-1]))
                    list.add(Pair(s, tags[index-1]))
                    list.add(Pair(s, tags[index-2]))
                }
                else
                {
                    list.add(Pair(s, tags[index-1]))
                }

                if(index + 2 < tags.size)
                {
                    list.add(Pair(s, tags[index+2] +" "+tags[index+1]))
                    list.add(Pair(s, tags[index+1]))
                    list.add(Pair(s, tags[index+2]))
                }
                else
                {
                    list.add(Pair(s, tags[index+1]))
                }
            }

            return list
        }
    }


}

data class Pair(val verb: String, val obj: String)
data class Source(val path: String, val vdoList: List<Pair>)
data class Project(val root: String, val sourceList: List<Source>) {
    fun save() {

    }
}


fun File.toPairList() : List<Pair>
{
    val parser = JavaParser.parse(this)
    val pairList : MutableList<Pair> = mutableListOf()

    parser.findAll(MethodDeclaration::class.java).forEach {
        if(it.nameAsString.isEmpty())
            return@forEach

        val pairs = it.nameAsString.toPairList()

        if(pairs.isNotEmpty())
            pairList.addAll(it.nameAsString.toPairList())

        if(it.comment.isPresent)
            it.comment.get().toString().lines()
                    .map{ it.toPairList() }
                    .filter { it.isNotEmpty() }
                    .forEach {
                        pairList.addAll(it)
                    }
    }

    return pairList
}