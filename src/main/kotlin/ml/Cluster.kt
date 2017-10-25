package ml

import data.SourceFile
import java.util.*

class Cluster(val sourceList:List<SourceFile>, val mergedDoc : HashMap<String, Int> = hashMapOf(), var totalSize:Int=0) {

    init {
        println("[Cluster] init")

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

        println("[Cluster] totalSize : $totalSize")

        sourceList.forEach { src->
            src.wordMap.values.forEach {
                src.tfIdfMap = hashMapOf()
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
    var centroidList : MutableList<SourceFile> = mutableListOf()

    fun clustering(k:Int = Math.sqrt(sourceList.size.toDouble()).toInt() * 2)
    {
        println("[Cluster] start size : ${sourceList.size} k : $k")

        val cIdx = Random().nextInt(sourceList.size)
        var centroid = sourceList.get(cIdx);

        println("[Cluster] first centroid idx : $cIdx")

        centroidList.add(centroid)

        for(i in 1..k)
        {
            var shortestDistance: Double = Double.MAX_VALUE
            var pickedCenteroid : SourceFile? = null

            sourceList.forEach{
                val dis = cosDistance(centroid, it)

                if(shortestDistance == dis && !centroidList.contains(it) && it.tfIdfMap.size > pickedCenteroid!!.tfIdfMap.size) {
                    shortestDistance = dis
                    pickedCenteroid = it
                }
                if(shortestDistance > dis && !centroidList.contains(it)) {
                    shortestDistance = dis
                    pickedCenteroid = it
                }
            }

            println("[Cluster] current  (${centroid.path}) tf-idf map : ${centroid.tfIdfMap}")
            println("[Cluster] selected (${pickedCenteroid?.path}) tf-idf map : ${pickedCenteroid?.tfIdfMap}")
            println("[Cluster] distance : $shortestDistance")
            println("----------------------------------------------------")

            centroid = pickedCenteroid!!
            centroidList.add(pickedCenteroid!!)
        }
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