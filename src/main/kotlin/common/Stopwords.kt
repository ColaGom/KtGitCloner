package common

import java.io.File


class Stopwords private constructor() {
    var stopSet : HashSet<String> = hashSetOf()

    init {
        File("stop_words_def.txt").readLines().forEach {
                stopSet.add(it)
        }
    }

    private object Holder {
        val INSTANCE = Stopwords()
    }

    companion object {
        val instance : Stopwords by lazy {
            Holder.INSTANCE
        }
    }

    fun contains(word : String) : Boolean
    {
        return stopSet.contains(word)
    }
}
