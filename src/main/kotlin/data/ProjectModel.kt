package data

import com.google.gson.*
import common.*
import nlp.PreProcessor
import java.io.File
import java.lang.reflect.Type
import java.nio.file.Paths

class ProjectModel(val rootPath:String, var sourceList : MutableList<SourceFile> = mutableListOf())
{
    companion object {
        fun getAll()
        {

        }
        fun File.toSaveFile() : File
        {
            return Paths.get(this.path.replace("Research\\Repository","Research\\data"), "list_source.json").toFile()
        }

        fun loadSourceFile(root:File, sourcePath:String) : SourceFile
        {
            return Gson().fromJson(root.readText(), ProjectModel::class.java).sourceList.find { it.path.equals(sourcePath) }!!
        }

        fun load(root: File) : ProjectModel
        {
            if(root.name.equals(NAME_PROJECT_MODEL) || root.name.equals(NAME_PROJECT_MODEL_CLEAN))
                return Gson().fromJson(root.readText(), ProjectModel::class.java);

            val saveFile = root.toSaveFile();
            return Gson().fromJson(saveFile.readText(), ProjectModel::class.java);
        }

        fun create(root: File) : ProjectModel
        {
            val project = ProjectModel(root.path)
            val savePath = "C:\\Research\\data"
            val saveFile = Paths.get(savePath, root.path.substringAfter("\\Repository"),"list_source.json").toFile();
            var success = 0
            var failed = 0

            if(!saveFile.parentFile.exists())
                saveFile.parentFile.mkdirs()

            FileUtils.readAllFilesExt(root, "java").forEach({
                try {
                    project.add(PreProcessor(it.readText()).toSourceFile(it.path))
                    success++
                }catch (e:StackOverflowError) {
                    failed++
                }catch (e:Exception) {
                    failed++
                }
            })

            println("[ProjectModel]create ${saveFile.path} success : ${success} failed : ${failed}")

            saveFile.printWriter().use {
                it.print(GsonBuilder().setPrettyPrinting().create().toJson(project))
            }

            return project
        }

        fun createOrLoad(root: File): ProjectModel {
            val saveFile = Paths.get(root.path.replace("D:", "C:").replace("Research\\Repository","Research\\data"), "list_source.json").toFile();

            if(saveFile.exists())
                return load(root)
            else
                return create(root)
        }
    }

    fun save(saveFile:File)
    {
        saveFile.printWriter().use {
            it.print(GsonBuilder().setPrettyPrinting().create().toJson(this))
        }

        println("saved ${saveFile.path}")
    }

    fun add(path:String, comLen:Int, srcLen:Int, wordMap:HashMap<String, HashMap<String, Int>>)
    {
        sourceList.add(SourceFile(path, comLen, srcLen, wordMap))
    }

    fun add(sourceFile:SourceFile)
    {
        sourceList.add(sourceFile)
    }
}

class SimplifySerializer : JsonSerializer<SourceFile>
{
    override fun serialize(src: SourceFile?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        val json = JsonObject()
        json.addProperty("path", src!!.path);
        return json
    }
}

data class SourceFile(val path:String, val comLen : Int, val srcLen:Int, val wordMap:HashMap<String,HashMap<String, Int>>)
{
    override fun toString(): String {
        return path
    }

    fun wordMapSize() : Int
    {
        var result = 0;

        wordMap.forEach{
            it.value.forEach{
                result += it.value
            }
        }

        return result
    }

    fun wordCount() : Int
    {
        var result = 0;

        wordMap.forEach{
            result  += it.value.size
        }

        return result
    }

    fun getMergedMap() : HashMap<String, Int>?
    {
        val result : HashMap<String, Int> = hashMapOf()
        result.putAll(wordMap.get(KEY_SOURCE)!!)

        wordMap.get(KEY_COMMENT)?.forEach {
            val key = it.key
            if(result.containsKey(key))
                result.set(key, result.get(key)!! + it.value)
            else
                result.put(key, it.value)
        }

        wordMap.values.forEach {  }

        return result
    }
}

