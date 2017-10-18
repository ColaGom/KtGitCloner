package git

import data.Repository
import java.io.BufferedReader
import java.io.InputStreamReader

class Cloner()
{
    val cloneDir = "C:/Research/Repository";

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

    private fun makeCommmand(target:Repository) : String
    {
        return "git clone ${target.clone_url} $cloneDir/${target.full_name}";
    }
}