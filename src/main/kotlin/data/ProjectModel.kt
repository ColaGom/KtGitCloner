package data

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import common.FileUtils
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
}