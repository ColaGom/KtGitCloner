package common

import java.io.File

internal class FileUtils
{
    companion object {
        fun ReadAllFiles(root: File, ext : String) : List<File>
        {
            return root.walkTopDown().filter { it.extension.equals(ext) }.toList()
        }
    }
}