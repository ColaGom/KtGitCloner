package newdata

import com.github.javaparser.JavaParser
import com.github.javaparser.ast.body.MethodDeclaration
import common.Stopwords
import nlp.PreProcessor
import opennlp.tools.cmdline.postag.POSModelLoader
import opennlp.tools.postag.POSTaggerME
import java.io.File

class ProjectDataGenerator(val root:File)
{
    fun generate()
    {

    }
}

class SourceDataGenerator(val source:File)
{
    companion object {
        val model = POSModelLoader().load(File("nlp/en-pos-maxent.bin"))
        val tagger = POSTaggerME(model)
    }

    fun generate()
    {
        val parser = JavaParser.parse(source)

        parser.findAll(MethodDeclaration::class.java).forEach {
            it.nameAsString.preprocessing()
        }

        parser.comments.forEach {
            it.content.lines().forEach {
                it.preprocessing()
            }
        }
    }

    fun lineToPair(line:String)
    {

    }

    val regexAnno = Regex("\\{.*?\\}")
    fun String.preprocessing() : List<String>
    {
        var str = PreProcessor.regCamelCase.replace(this," ")
        str = PreProcessor.regHtml.replace(str, "")
        str = regexAnno.replace(str,"")

        println(str)

        str = com.sun.deploy.util.StringUtils.trimWhitespace(str)

        val result = PreProcessor.regNonAlphanum.split(str.toLowerCase()).filter { !Stopwords.instance.contains(it) && it.length > 2 && it.length < 15 }
        println("$this -> $result")

        return result
    }
}

data class ProjectData(val root:String, val sourceList:List<SourceData>)
data class SourceData(val path:String, val pairs:List<String>)
