package ml

import com.google.gson.*
import data.SourceFile
import java.lang.reflect.Type

class Cluster() {
    companion object {
        fun save() {

        }

        fun load() {

        }
    }

    var centroidIdx:Int = 0
    private val memberList: MutableList<Node> = mutableListOf()

    fun getCentroid() = memberList.get(centroidIdx)

    fun setCentroid(centroid:Node)
    {
        memberList.add(centroid)
        centroidIdx = memberList.lastIndex
    }

    fun addMember(member: Node) {
        memberList.add(member)
    }

    fun getMemberList() : List<Node> {
        return memberList.filterIndexed { index, node -> index != centroidIdx }
    }

    fun mean(): Double {
        var sum = 0.0

        memberList.parallelStream().forEach {
            sum += getCentroid().distanceTo(it)
        }

        return sum / size()
    }

    fun updateCentroid() : Boolean {
        var maxValue = getCentroid().mean(memberList)
        var maxIdx = centroidIdx

        memberList.forEachIndexed { index, node ->
            val current = node.mean(memberList)

            if(current > maxValue)
            {
                maxValue = current
                maxIdx = index
            }
        }

        if(centroidIdx != maxIdx) {
            centroidIdx = maxIdx
            return true
        }
        return false
    }

    fun clearMember() {
        memberList.removeIf{
            !it.equals(getCentroid())
        }

        centroidIdx = 0
    }

    fun size() = memberList.size
}

class SimpleSerializer : JsonSerializer<Any>
{
    override fun serialize(src: Any?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        if(src is Node)
        {
            val json = JsonObject()
            json.addProperty("fileName", src.fileName);
            return json
        }
        return Gson().toJsonTree(src)
    }
}

data class Node(val fileName: String, val v: HashMap<String, Double>) {
    override fun equals(other: Any?): Boolean {
        if (other is Node)
            return other.fileName.equals(fileName)

        return super.equals(other)
    }

    fun distanceTo(dst: Node): Double {
//        val v1 = this.v
//        val v2 = dst.v
//
//        var union = 0
//        var inter = 0
//
//
//        if(v1 != null && v2 != null)
//        {
//            v1.values.forEach { union += it }
//            v2.values.forEach { union += it }
//
//            val both = v1.keys.toHashSet()
//            both.retainAll(v2.keys.toHashSet())
//
//            both.forEach {
//                inter += v1.get(it)!!
//                inter += v2.get(it)!!
//            }
//
//            return inter / union.toDouble()
//        }
//
//        return 0.0

//Using Cosine measure (just tf)
        val v1 = this.v
        val v2 = dst.v

        if (v1 != null && v2 != null) {

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
        } else
            return 0.0
    }

    fun mean(nodes: List<Node>): Double {
        var sum = 0.0

        nodes.forEach {
            sum += distanceTo(it)
        }

        return sum / nodes.size
    }
}