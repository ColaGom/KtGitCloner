package ml

import java.util.*

class Kmeans(val k: Int, val iter: Int = 100) : Clustering(k) {
    override fun clustering(nodeList: List<Node>) {

        val start = System.currentTimeMillis()
        println("[Kmeans]start Clustering size : ${nodeList.size} at : ${start}")

        for (i in 1..k) {
            val cluster = Cluster()
            var picked:Int

            do{
                picked = Random().nextInt(nodeList.size)
            }
            while(getCentroidSet().contains(nodeList.get(picked).fileName))

            cluster.setCentroid(nodeList.get(picked))
            clusters.add(cluster)
        }

        println("[Kmeans]initialize centroids : ${clusters.map { it.getCentroid().fileName }}")

        var count = 0

        while (true) {
            println("start step - 1 ${System.currentTimeMillis() - start}")
            clusters.forEach {
                it.clearMember()
            }

            println("start step - 2 ${System.currentTimeMillis() - start}")

            val centroidSet = getCentroidSet()

            nodeList.filter { !centroidSet.contains(it.fileName) }.parallelStream().forEach { node ->
                clusters.maxBy { it.getCentroid().distanceTo(node) }!!.addMember(node)
            }

            var flag = false

            println("start step - 3 ${System.currentTimeMillis() - start}")
            clusters.parallelStream().forEach {
                if (it.updateCentroid())
                    flag = true
            }
            ++count

            if (!flag || count == iter)
                break

            println("updated Centroid ${count} centroids : ${clusters.map { it.getCentroid().fileName }} ${System.currentTimeMillis() - start}")
        }

        println("-------------------Finisehd clustering ${System.currentTimeMillis() - start}")
    }

    fun getCentroidSet(): HashSet<String> {
        return clusters.map{
            it.getCentroid().fileName
        }.toHashSet()
    }
}