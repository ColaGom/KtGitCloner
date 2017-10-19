package common

import java.io.File

class Projects {
    companion object {
        val ROOT : File = File("C:\\Research\\Repository")

        fun getAllProjects() : List<File>
        {
            var result : MutableList<File> = mutableListOf();

            ROOT.listFiles().forEach { owner -> owner.listFiles().filter { it.isDirectory }.forEach { result.add(it) } }

            return result;
        }
    }
}