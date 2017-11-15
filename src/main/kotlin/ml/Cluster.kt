package ml

import com.google.gson.*
import java.lang.reflect.Type

class Cluster(val nodeList:List<Node>) {
    var centroid:Int = 0
    set(value) {
        memberList.add(value)
    }

    val memberList : MutableSet<Int> = mutableSetOf()

    fun addMember(member: Int) {
        memberList.add(member)
    }

    fun addMember(list: Set<Int>)
    {
        memberList.addAll(list)
    }

    fun mean(): Double {
        var sum = 0.0

        memberList.parallelStream().forEach {
            sum += nodeList.get(centroid).distanceTo(nodeList.get(it))
        }

        return sum / size()
    }

    fun getNodes() : List<Node>
    {
        return nodeList.filterIndexed { index, node -> memberList.contains(index) }
    }

    fun getCentroid() : Node
    {
        return nodeList.get(centroid)
    }

    fun updateCentroid() : Boolean {
        val nodes = getNodes()
        var maxValue = Double.MIN_VALUE
        var maxIdx = 0

        nodes.forEachIndexed { index, node ->
            val current = node.mean(nodes)

            if(current > maxValue)
            {
                maxValue = current
                maxIdx = index
            }
        }

        if(centroid != maxIdx) {
            centroid = maxIdx
            return true
        }
        return false
    }

    fun clearMember() {
        memberList.removeIf{
            !it.equals(centroid)
        }
        centroid = 0
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

