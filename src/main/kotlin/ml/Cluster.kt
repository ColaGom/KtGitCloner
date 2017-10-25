package ml

import common.KEY_SOURCE
import data.SourceFile
import java.util.*
import kotlin.collections.HashSet

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
    var centroidList : MutableList<SourceFile> = mutableListOf()

    fun clustering(k:Int = Math.sqrt(sourceList.size.toDouble()).toInt())
    {
        println("[Cluster] start size : ${sourceList.size} k : $k")

        val cIdx = Random().nextInt() % sourceList.size
        var centroid = sourceList.get(cIdx);

        println("[Cluster] first centroid idx : $cIdx")

        centroidList.add(centroid)

        for(i in 1..k)
        {
            var farestDistance : Double = 0.0;
            var pickedCenteroid : SourceFile = centroid

            sourceList.forEach{
                val dis = cosDistance(centroid, it)

                if(farestDistance < dis) {
                    farestDistance = dis
                    pickedCenteroid = it
                }
            }

            println("[Cluster] $i centroid is : ${pickedCenteroid.path} distance : $farestDistance")
            centroid = pickedCenteroid
        }
    }

    fun cosDistance(src: SourceFile, src2 :SourceFile) : Double
    {
        val v1 = src.getMergedMap()
        val v2 = src2.getMergedMap()

        if(v1 != null && v2 != null) {

            val both = src.wordMap.keys.toHashSet()
            both.retainAll(src2.wordMap.keys.toHashSet())
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