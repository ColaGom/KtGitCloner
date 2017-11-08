package common

import com.google.gson.GsonBuilder
import nlp.PreProcessor
import org.tartarus.snowball.ext.englishStemmer
import java.io.File
import java.nio.file.Paths

class BagOfWord(val root: File) {

    fun generate_snow_map() {
        val mapBow = generate_map()
        if (mapBow != null) {
            val stemmer = englishStemmer()

            var stemmedMap: HashMap<String, HashMap<String, HashMap<String, Int>>> = hashMapOf()

            mapBow.forEach {
                val filePath = it.key
                var data: HashMap<String, HashMap<String, Int>> = hashMapOf()

                it.value.forEach {
                    val key = it.key
                    val freqMap: HashMap<String, Int> = hashMapOf()

                    it.value.forEach {
                        stemmer.setCurrent(it)
                        stemmer.stem()
                        val word = stemmer.current

                        if (freqMap.containsKey(word))
                            freqMap.put(word, freqMap.get(word)!! + 1)
                        else
                            freqMap.put(word, 1)
                    }

                    data.put(key, freqMap)
                }

                stemmedMap.put(filePath.replace("\\\\", "\\"), data)
            }

            val saveFile = root.toSaveFile(NAME_FREQ_SNOW_MAP)
            saveFile.printWriter().use {
                it.print(GsonBuilder().setPrettyPrinting().create().toJson(stemmedMap))
            }
            println("saved ${saveFile.path}")
        }
    }

    fun generate_map(): HashMap<String, HashMap<String, List<String>>>? {
        var mapBow = HashMap<String, HashMap<String, List<String>>>()
        val saveFile = root.toSaveFile(NAME_BOW_MAP)
        var success = 0;
        var failed = 0;

        if (!saveFile.exists())
            saveFile.parentFile.mkdirs()
        else
            return null

        FileUtils.readAllFilesExt(root, "java").forEach({
            try {
                mapBow.put(it.path, PreProcessor(it.readText()).run2())
                success++
            } catch (e: StackOverflowError) {
                failed++
            } catch (e: Exception) {
                failed++
            }
        })

        println("[BagOfWord]generate ${saveFile.path} success : ${success} failed : ${failed}")

        saveFile.printWriter().use {
            it.print(GsonBuilder().setPrettyPrinting().create().toJson(mapBow))
        }

        println("saved ${saveFile.path}")

        return mapBow
    }

    fun generate() {
        var mapBow = HashMap<String, List<String>>()
        val saveFile = root.toSaveFile()

        if (!saveFile.exists())
            saveFile.parentFile.mkdirs()

        FileUtils.readAllFilesExt(root, "java").forEach({
            try {
                mapBow.put(it.path, PreProcessor(it.readText()).run())
            } catch (e: StackOverflowError) {
                println("Failed readText Error ${it.path}")
            } catch (e: Exception) {
                println("Failed readText Exception ${it.path}")
            }
        })

        println("[BagOfWord]generate ${saveFile.path}")

        saveFile.printWriter().use {
            it.print(GsonBuilder().setPrettyPrinting().create().toJson(mapBow))
        }
    }

    private fun File.toSaveFile(name: String = NAME_BOW): File {
        return Paths.get(path.replace("D:", "C:").replace("Research\\Repository", "Research\\data"), name).toFile();
    }
}