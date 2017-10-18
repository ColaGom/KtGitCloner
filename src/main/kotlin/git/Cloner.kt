package git

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import data.Repository
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class Cloner(val path:String)
{
    val cloneDir = "C:/Research/Repository";

    fun cloneAll()
    {
        val map : HashMap<String, Repository> = Gson().fromJson(readText(), object : TypeToken<HashMap<String, Repository>>() {}.type)
        val size = map.values.size

        println(size)

        var count = 0;
        map.values.forEach({
            println("${++count} / ${size}")
            clone(it);
        })
    }

    fun clone(target:Repository)
    {
        val command = makeCommmand(target)

        try {
            val builder = ProcessBuilder("cmd.exe", "/c", command)
            builder.redirectErrorStream(true)
            val p = builder.start()
            val r = BufferedReader(InputStreamReader(p.inputStream))
            var line : String?
            while (true) {

                line = r.readLine()

                if (line == null) {
                    break
                }

                println(line)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun readText() : String
    {
        return File(path).readText();
    }


    private fun makeCommmand(target:Repository) : String
    {
        return "git clone ${target.clone_url} $cloneDir/${target.full_name}";
    }
}