package newdata

import com.google.gson.Gson
import common.FileUtils
import common.NAME_PROJECT
import common.PATH_DATA
import java.io.File
import java.nio.file.Paths


data class Project (val root:String, val sourceList:MutableList<SourceFile> = mutableListOf())
{
    companion object {
        fun load(file:File) : Project
        {
            return Gson().fromJson(file.readText(), Project::class.java);
        }

        fun create(root:String)
        {
            val saveFile = Paths.get(PATH_DATA, root.substringAfter("\\Repository"), NAME_PROJECT).toFile();

            if(saveFile.exists())
                return

            if(!saveFile.parentFile.exists())
                saveFile.parentFile.mkdirs()

            val project = Project(root)

            var success = 0
            var falied = 0

            FileUtils.readAllFilesExt(File(root), "java").forEach {
                try{
                    val analyst = SourceAnalyst(it)
                    analyst.analysis()
                    project.sourceList.add(analyst.sourceFile)
                    success++
                }
                catch (e:Exception)
                {
                    falied++
                }
                catch (e:Error)
                {
                    falied++
                }
            }

            saveFile.printWriter().use {
                it.print(Gson().toJson(project))
            }

            println("[create]${saveFile.path} success : $success failed : $falied")
        }
    }
}
