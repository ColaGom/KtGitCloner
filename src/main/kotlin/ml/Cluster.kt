package ml

import data.SourceFile

class Cluster(val sourceList:List<SourceFile>, val mergedDoc : HashMap<String, Int> = hashMapOf(), var totalSize:Int=0) {

    init {
        sourceList.forEach {
            it.wordMap.forEach {
                it.value.forEach {
                    val key = it.key
                    totalSize += it.value
                    if(mergedDoc.containsKey(key))
                        mergedDoc.set(key, mergedDoc.get(key)!! + it.value)
                    else
                        mergedDoc.put(key, it.value)
                }
            }
        }

        sourceList.forEach { src->
            src.wordMap.values.forEach {
                it.forEach{
                    src.tfIdfMap.put(it.key, tfIdf(it.key, src));
                }
            }
        }
    }

    /**
     * k means ++
     * 1. pick randomly centroid
     * 2. pick next centroid that has farest distance from current centroid
     * 3. k loop step .2 count of k
     * 4. clustering and assign
     *
     * size k is root n (n is doc size)
     */
    fun clustering(k:Int)
    {
        println("[Cluster] start size : ${sourceList.size} k : $k")

    }

    fun cosDistance(src: SourceFile, src2 :SourceFile)
    {

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