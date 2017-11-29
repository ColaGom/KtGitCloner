package newdata

import Main
import com.github.javaparser.JavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.ConstructorDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.body.VariableDeclarator
import common.Stopwords
import nlp.PreProcessor.Companion.regCamelCase
import nlp.PreProcessor.Companion.regHtml
import nlp.PreProcessor.Companion.regNonAlphanum
import org.tartarus.snowball.ext.englishStemmer
import tfidfDoc
import java.io.File

class SourceAnalyst(val file:File)
{
    val sourceFile = SourceFile(file.path)
    var parser:CompilationUnit
    val stemmer = englishStemmer()

    init {
        parser = JavaParser.parse(file)
    }
    fun analysis()
    {
        extractClassOrInterface()
        extractImports()
        extractMethods()
    }

    fun extractImports()
    {
        parser.imports.forEach {
            it.nameAsString.preprocessing().forEach {
                sourceFile.imports.put(it)
            }
        }
    }

    fun extractMethods()
    {
        parser.findAll(MethodDeclaration::class.java).forEach {
            //method comment
            if(it.comment.isPresent)
                it.comment.get().toString().preprocessing().forEach {
                    sourceFile.commentsMethod.put(it)
                }

            it.nameAsString.preprocessing().forEach {
                sourceFile.nameMethod.put(it)
            }

            it.type.toString().preprocessing().forEach {
                sourceFile.typeMethod.put(it)
            }

            //method parameter
            it.parameters.forEach {
                it.nameAsString.preprocessing().forEach {
                    sourceFile.nameParameter.put(it)
                }

                it.type.toString().preprocessing().forEach {
                    sourceFile.typeParameter.put(it)
                }
            }

            // local variable
            it.findAll(VariableDeclarator::class.java).forEach {
                if(it.comment.isPresent)
                    it.comment.get().toString().preprocessing().forEach {
                        sourceFile.commentsVariable.put(it)
                    }

                it.nameAsString.preprocessing().forEach {
                    sourceFile.nameVariable.put(it)
                }

                it.type.toString().preprocessing().forEach {
                    sourceFile.typeVariable.put(it)
                }
            }
        }
    }

    fun extractClassOrInterface()
    {
        parser.findAll(ClassOrInterfaceDeclaration::class.java).forEach {
            if(it.comment.isPresent)
                it.comment.get().toString().preprocessing().forEach {
                    sourceFile.commentsClassOrInterface.put(it)
                }

            it.nameAsString.preprocessing().forEach {
                sourceFile.nameClassOrInterface.put(it)
            }

            //constructor
            it.findAll(ConstructorDeclaration::class.java).forEach {
                if(it.comment.isPresent)
                    it.comment.get().toString().preprocessing().forEach {
                        sourceFile.commentsClassOrInterface.put(it)
                    }

                it.nameAsString.preprocessing().forEach {
                    sourceFile.nameClassOrInterface.put(it)
                }

                it.parameters.forEach {
                    it.nameAsString.preprocessing().forEach {
                        sourceFile.nameParameter.put(it)
                    }

                    it.type.toString().preprocessing().forEach {
                        sourceFile.typeParameter.put(it)
                    }
                }
            }

            // fields
            it.findAll(VariableDeclarator::class.java).forEach {
                if(it.comment.isPresent)
                    it.comment.get().toString().preprocessing().forEach {
                        sourceFile.commentsVariable.put(it)
                    }

                it.nameAsString.preprocessing().forEach {
                    sourceFile.nameVariable.put(it)
                }

                it.type.toString().preprocessing().forEach {
                    sourceFile.typeVariable.put(it)
                }
            }
        }
    }

    fun String.preprocessing() : List<String>
    {
        var str = regCamelCase.replace(this," ")
        str = regHtml.replace(str, "")
        str = com.sun.deploy.util.StringUtils.trimWhitespace(str)

        return regNonAlphanum.split(str.toLowerCase()).filter { it.length > 2 && it.length < 20 && !Stopwords.instance.contains(it.toLowerCase()) }.map {
            stemmer.setCurrent(it)
            stemmer.stem()
            stemmer.current
        }
    }
}

class SourceFileNode(source:SourceFile)
{
    lateinit var weightMap : HashMap<String, Double>
    init {
    }
}


data class SourceFile (
        val path:String,
        val imports:CountMap = CountMap(),
        val commentsClassOrInterface:CountMap = CountMap(),
        val commentsMethod:CountMap = CountMap(),
        val commentsVariable:CountMap = CountMap(),
        val typeMethod:CountMap = CountMap(),
        val typeVariable:CountMap = CountMap(),
        val typeParameter:CountMap = CountMap(),
        val nameClassOrInterface:CountMap = CountMap(),
        val nameMethod:CountMap = CountMap(),
        val nameVariable:CountMap = CountMap(),
        val nameParameter:CountMap = CountMap()
)
{

    fun mergeMap() : HashMap<String, Int>
    {
        val result : HashMap<String, Int> = hashMapOf()

        excuteAll { result.increase(it) }

        return result;
    }

    fun tfIdfMap() : HashMap<String, Double>
    {
        return mergeMap().tfidfDoc(Main.DF_MAP)
    }



    fun excuteAll(qux : (Map.Entry<String,Int>) -> Unit)
    {
//        imports.forEach(qux)
//        commentsClassOrInterface.forEach(qux)
//        commentsVariable.forEach(qux)
        commentsMethod.forEach(qux)
//        typeVariable.forEach(qux)
//        typeParameter.forEach(qux)
        typeMethod.forEach(qux)
        nameMethod.forEach(qux)
//        nameClassOrInterface.forEach(qux)
//        nameParameter.forEach(qux)
//        nameVariable.forEach(qux)
    }

    fun HashMap<String, Int>.increase(entry:Map.Entry<String, Int>) {
        if (containsKey(entry.key))
            set(entry.key, get(entry.key)!! + entry.value)
        else
            put(entry.key, entry.value)
    }


    fun getAllWords() : HashSet<String>
    {
        val set : HashSet<String> = hashSetOf()

        set.addAll(imports.keys)
        set.addAll(commentsClassOrInterface.keys)
        set.addAll(commentsMethod.keys)
        set.addAll(commentsVariable.keys)
        set.addAll(typeMethod.keys)
        set.addAll(typeVariable.keys)
        set.addAll(typeParameter.keys)
        set.addAll(nameClassOrInterface.keys)
        set.addAll(nameMethod.keys)
        set.addAll(nameMethod.keys)
        set.addAll(nameParameter.keys)

        return set
    }
}

class CountMap : HashMap<String, Int>()
{
    fun put(key:String)
    {
        if(containsKey(key))
            set(key, get(key)!! + 1)
        else
            put(key, 1)
    }
}

