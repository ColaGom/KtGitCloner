package ml

import data.SourceFile

class Searcher(val query:SourceFile, val sourceList:List<SourceFile>)
{
    val mergedDoc : HashMap<String, Int> = hashMapOf()
    var totalSize = 0

    init {
        println("[Searcher] init")

        sourceList.forEach {
            totalSize += 1
            it.wordMap.forEach {
                it.value.forEach {
                    val key = it.key
                    if(mergedDoc.containsKey(key))
                        mergedDoc.set(key, mergedDoc.get(key)!! + 1)
                    else
                        mergedDoc.put(key, 1)
                }
            }
        }

        totalSize += 1

        query.wordMap.forEach{
            it.value.forEach {
                val key = it.key
                if(mergedDoc.containsKey(key))
                    mergedDoc.set(key, mergedDoc.get(key)!! + 1)
                else
                    mergedDoc.put(key, 1)
            }
        }

        println("[Searcher] totalSize : $totalSize")

        query.tfIdfMap = hashMapOf()

        query.wordMap.values.forEach {
            it.forEach{
                query.tfIdfMap.put(it.key, tfIdf(it.key, query));
            }
        }

        sourceList.forEach { src->
            src.tfIdfMap = hashMapOf()
            src.wordMap.values.forEach {
                it.forEach{
                    src.tfIdfMap.put(it.key, tfIdf(it.key, src));
                }
            }
        }
    }

    fun run() : HashMap<String, Double>
    {
        val result :HashMap<String, Double> = hashMapOf()

        sourceList.forEach {
            result.put(it.path ,cosDistance(query, it))
        }

        return result
    }

    fun cosDistance(src: SourceFile, src2 :SourceFile) : Double
    {
        val v1 = src.tfIdfMap
        val v2 = src2.tfIdfMap

        if(v1 != null && v2 != null) {

            val both = v1.keys.toHashSet()
            both.retainAll(v2.keys.toHashSet())
            var sclar = 0.0
            var norm1 = 0.0
            var norm2 = 0.0
            for (k in both)
                sclar += v1.get(k)!! * v2.get(k)!!
            for (k in v1.keys)
                norm1 += v1.get(k)!! * v1.get(k)!!
            for (k in v2.keys)
                norm2 += v2.get(k)!! * v2.get(k)!!

            return sclar / Math.sqrt(norm1 * norm2)
        }
        else
            return 0.0
    }

    fun tfIdf(term:String, src : SourceFile) : Double
    {
        return tf(term,src) * idf(term)
    }

    fun tf(term:String, src : SourceFile) : Double
    {
        var tf = 0;

        src.wordMap.forEach{
            if(it.value.containsKey(term))
                tf += it.value.get(term)!!
        }

        return tf / src.wordMapSize().toDouble()
    }

    fun idf(term:String) : Double
    {
        return Math.log(totalSize.toDouble() / mergedDoc.get(term)!!)
    }
}