package data

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import common.FileUtils
import common.KEY_COMMENT
import common.KEY_SOURCE
import nlp.PreProcessor
import java.io.File
import java.nio.file.Paths

class ProjectModel(val rootPath:String, var sourceList : MutableList<SourceFile> = mutableListOf())
{
    companion object {
        fun File.toSaveFile() : File
        {
            return Paths.get(this.path.replace("Research\\Repository","Research\\data"), "list_source.json").toFile()
        }

        fun load(root: File) : ProjectModel
        {
            val saveFile = root.toSaveFile();
            return Gson().fromJson(saveFile.readText(), ProjectModel::class.java);
        }

        fun create(root: File) : ProjectModel
        {
            val project = ProjectModel(root.path)
            val saveFile = Paths.get(root.path.replace("Research\\Repository","Research\\data"), "list_source.json").toFile();
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
            val saveFile = Paths.get(root.path.replace("Research\\Repository","Research\\data"), "list_source.json").toFile();

            if(saveFile.exists())
                return load(root)
            else
                return create(root)
        }
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

data class SourceFile(val path:String, val comLen : Int, val srcLen:Int, val wordMap:HashMap<String,HashMap<String, Int>>)
{
    var tfIdfMap : HashMap<String, Double> = hashMapOf();

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

    fun getMergedMap() : HashMap<String, Int>?
    {
        val result : HashMap<String, Int>? = wordMap.get(KEY_SOURCE)

        wordMap.get(KEY_COMMENT)?.forEach {
            val key = it.key
            if(result!!.containsKey(key))
                result.set(key, result.get(key)!! + it.value)
            else
                result.put(key, it.value)
        }

        return result
    }
}