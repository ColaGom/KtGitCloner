package common

import com.google.gson.GsonBuilder
import nlp.PreProcessor
import java.io.File
import java.nio.file.Paths

class BagOfWord(val root:File)
{
    fun generate_map()
    {
        var mapBow = HashMap<String, HashMap<String, List<String>>>()
        val saveFile = root.toSaveFile(NAME_BOW_MAP)
        var success = 0;
        var failed = 0;

        if(!saveFile.exists())
            saveFile.parentFile.mkdirs()

        FileUtils.readAllFilesExt(root, "java").forEach({
            try {
                mapBow.put(it.path, PreProcessor(it.readText()).run2())
                success++
            }catch (e:StackOverflowError) {
                failed++
            }catch (e:Exception) {
                failed++
            }
        })

        println("[BagOfWord]generate ${saveFile.path} success : ${success} failed : ${failed}")

        saveFile.printWriter().use {
            it.print(GsonBuilder().setPrettyPrinting().create().toJson(mapBow))
        }
    }

    fun generate()
    {
        var mapBow = HashMap<String, List<String>>()
        val saveFile = root.toSaveFile()

        if(!saveFile.exists())
            saveFile.parentFile.mkdirs()

        FileUtils.readAllFilesExt(root, "java").forEach({
            try {
                mapBow.put(it.path, PreProcessor(it.readText()).run())
            }catch (e:StackOverflowError) {
                println("Failed readText Error ${it.path}")
            }catch (e:Exception) {
                println("Failed readText Exception ${it.path}")
            }
        })

        println("[BagOfWord]generate ${saveFile.path}")

        saveFile.printWriter().use {
            it.print(GsonBuilder().setPrettyPrinting().create().toJson(mapBow))
        }
    }

    private fun File.toSaveFile(name:String = NAME_BOW) : File
    {
        return Paths.get(path.replace("Research\\Repository","Research\\data"), name).toFile();
    }
}