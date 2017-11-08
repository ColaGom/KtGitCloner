package ml

import java.util.*

class Kmeans(k: Int, val iter: Int = 100) : Clustering(k) {
    override fun clustering(nodeList: List<Node>) {
        super.clustering(nodeList)

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

        log("initialize centroids : ${clusters.map { it.getCentroid().fileName }}")

        var count = 0

        while (true) {
            log("start step - 1")
            clusters.forEach {
                it.clearMember()
            }

            log("start step - 2")
            val centroidSet = getCentroidSet()

            nodeList.filter { !centroidSet.contains(it.fileName) }.parallelStream().forEach { node ->
                clusters.maxBy { it.getCentroid().distanceTo(node) }!!.addMember(node)
            }

            var flag = false

            log("start step - 3")
            clusters.parallelStream().forEach {
                if (it.updateCentroid())
                    flag = true
            }
            ++count

            if (!flag || count == iter)
                break

            log("updated Centroid ${count} centroids : ${clusters.map { it.getCentroid().fileName }}")
        }

        log("Finished clustering")
        printAnalysis()
    }

    fun getCentroidSet(): HashSet<String> {
        return clusters.map{
            it.getCentroid().fileName
        }.toHashSet()
    }
}