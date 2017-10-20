package common

import java.io.File

internal class FileUtils
{
    companion object {
        fun readAllFiles(root: File, filter: String) : List<File>
        {
            return root.walkTopDown().filter { it.path.contains(filter) }.toList()
        }

        fun readAllFilesExt(root: File, ext: String) : List<File>
        {
            return root.walkTopDown().filter { it.extension.equals(ext) }.toList()
        }
    }
}