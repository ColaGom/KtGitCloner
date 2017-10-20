package common

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import nlp.PreProcessor
import java.io.File
import java.nio.file.Paths

class BagOfWord(val root:File)
{
    fun generate()
    {
        var mapBow = HashMap<String, List<String>>()
        val saveFile = root.toSaveFile()

        if(!saveFile.exists())
            saveFile.parentFile.mkdirs()

        FileUtils.readAllFilesExt(root, "java").forEach({
            mapBow.put(it.path, PreProcessor(it.readText()).run())
        })

        println("[BagOfWord]generate ${saveFile.path}")

        saveFile.printWriter().use {
            it.print(GsonBuilder().setPrettyPrinting().create().toJson(mapBow))
        }
    }

    private fun File.toSaveFile() : File
    {
        return Paths.get(path.replace("Research\\Repository","Research\\data"), NAME_BOW).toFile();
    }
}