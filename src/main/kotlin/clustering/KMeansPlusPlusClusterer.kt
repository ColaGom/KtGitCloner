package clustering

enum class EmptyClusterStrategy {
    LARGEST_VARIANCE,
    LARGEST_POINTS_NUMBER,
    FARTHEST_POINT,
    ERROR
}

class KMeansPlusPlusClusterer<T:Clusterable>(val k:Int, val maxIter:Int = Int.MAX_VALUE,measure:DistanceMeasure, val emptyStratege:EmptyClusterStrategy = EmptyClusterStrategy.LARGEST_VARIANCE) : Clusterer<T>(measure) {
    override fun cluster(nodes: Collection<T>): List<Cluster<T>> {
        val clusters : List<CentroidCluster<T>> = mutableListOf()
        val assignments = IntArray(nodes.size)

        assignPointsToClusters(clusters, nodes, assignments)

        for(i in 1..maxIter)
        {
            var emptyCluster = false
            val newCluster:List<CentroidCluster<T>> = mutableListOf()

            val newCenter : Clusterable
            clusters.forEachIndexed { index, centroidCluster ->
                if(centroidCluster.nodes.isEmpty())
                {
                    when(emptyStratege)
                    {
                        EmptyClusterStrategy.LARGEST_VARIANCE->
                        {

                        }

                        EmptyClusterStrategy.FARTHEST_POINT->
                        {

                        }

                        EmptyClusterStrategy.LARGEST_POINTS_NUMBER->
                        {

                        }

                        EmptyClusterStrategy.ERROR->
                        {

                        }
                    }
                }
            }
        }
        return clusters
    }

    fun assignPointsToClusters(clusters:List<CentroidCluster<T>>, nodes:Collection<T>, assignments:IntArray) : Int
    {
        var assignedDifferently = 0

        var clusterIdx = 0


        nodes.forEachIndexed {
            index, t ->
            clusterIdx = getNearestCluster(clusters, t)

            if(clusterIdx != assignments[index])
                assignedDifferently++

            assignments[index] = clusterIdx
            val cluster = clusters.get(clusterIdx)
            cluster.addNode(t)
        }

        return assignedDifferently
    }

    fun getNearestCluster(clusters:Collection<CentroidCluster<T>>, node:T) : Int
    {
        var minDistance = Double.MAX_VALUE
        var minCluster = 0

        clusters.forEachIndexed {
            index, cluster ->
            val distance = distance(node, cluster.center)

            if(distance < minDistance)
            {
                minDistance = distance
                minCluster = index
            }
        }

        return minCluster
    }
}