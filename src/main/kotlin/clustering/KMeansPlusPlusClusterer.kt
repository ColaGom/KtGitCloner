package clustering

import org.apache.commons.math3.exception.ConvergenceException
import org.apache.commons.math3.exception.util.LocalizedFormats
import org.apache.commons.math3.random.JDKRandomGenerator
import org.apache.commons.math3.random.RandomGenerator
import org.apache.commons.math3.stat.descriptive.moment.Variance

enum class EmptyClusterStrategy {
    LARGEST_VARIANCE,
    LARGEST_POINTS_NUMBER,
    FARTHEST_POINT,
    ERROR
}

class KMeansPlusPlusClusterer<T:Clusterable>(val k:Int, val maxIter:Int = Int.MAX_VALUE,measure:DistanceMeasure,
                                             val emptyStratege:EmptyClusterStrategy = EmptyClusterStrategy.LARGEST_VARIANCE,
                                             val random:RandomGenerator = JDKRandomGenerator()) : Clusterer<T>(measure) {
    override fun cluster(nodes: Collection<T>): List<Cluster<T>> {
        var clusters : List<CentroidCluster<T>> = chooseInitialCenters(nodes);
        val assignments = IntArray(nodes.size)

        assignPointsToClusters(clusters, nodes, assignments)

        for(i in 1..maxIter)
        {
            var emptyCluster = false
            val newClusters:MutableList<CentroidCluster<T>> = mutableListOf()

            var newCenter : Clusterable

            clusters.forEachIndexed { index, cluster ->
                if(cluster.nodes.isEmpty())
                {
                    when(emptyStratege)
                    {
                        EmptyClusterStrategy.LARGEST_VARIANCE->
                        {
                            newCenter = getPointFromLargestVariance(clusters)
                        }

                        EmptyClusterStrategy.FARTHEST_POINT->
                        {
                            newCenter = getFarthestPoint(clusters)
                        }

                        EmptyClusterStrategy.LARGEST_POINTS_NUMBER->
                        {
                            newCenter = getPointFromLargestNumberCluster(clusters)
                        }

                        EmptyClusterStrategy.ERROR->
                        {
                            throw ConvergenceException(LocalizedFormats.EMPTY_CLUSTER_IN_K_MEANS)
                        }
                    }
                }
                else {
                    newCenter = centroidOf(cluster.nodes)
                }
                newClusters.add(CentroidCluster(newCenter))
            }

            val change = assignPointsToClusters(newClusters, nodes, assignments)
            clusters = newClusters

            if(change == 0 && ! emptyCluster)
            {
                return clusters
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

    fun chooseInitialCenters(nodes:Collection<T>) : List<CentroidCluster<T>>
    {
        val nodeList = nodes.toList()
        val sizeNodes = nodes.size
        val taken = BooleanArray(sizeNodes)
        val result : MutableList<CentroidCluster<T>>  = mutableListOf()
        val firstNodeIdx = random.nextInt(sizeNodes)
        val firstNode = nodeList.get(firstNodeIdx)

        result.add(CentroidCluster<T>(firstNode))
        taken[firstNodeIdx] = true

        val minDistSquared = DoubleArray(sizeNodes)

        for(i in 0..sizeNodes-1)
        {
            if(i != firstNodeIdx)
            {
                val d = distance(firstNode, nodeList.get(i))
                minDistSquared[i] = d * d
            }
        }

        while(result.size < k)
        {
            var distSqSum = 0.0

            for(i in 0..sizeNodes-1)
            {
                if(!taken[i])
                {
                    distSqSum += minDistSquared[i]
                }
            }

            val r = random.nextDouble() * distSqSum

            var nextNodeIdx = -1
            var sum = 0.0

            for(i in 0..sizeNodes-1)
            {
                if(!taken[i])
                {
                    sum += minDistSquared[i]
                    if(sum >= r) {
                        nextNodeIdx = i
                        break
                    }
                }
            }

            if(nextNodeIdx == -1)
            {
                for(i in sizeNodes-1..0)
                {
                    if(!taken[i])
                    {
                        nextNodeIdx = i
                        break;
                    }
                }
            }

            if(nextNodeIdx >= 0) {
                val p = nodeList.get(nextNodeIdx)

                result.add(CentroidCluster<T>(p))

                taken[nextNodeIdx] = true

                if(result.size < k)
                {
                    for(i in 0..sizeNodes-1)
                    {
                        if(!taken[i])
                        {
                            val d = distance(p, nodeList.get(i))
                            val d2 = d*d
                            if(d2 < minDistSquared[i])
                                minDistSquared[i] = d2
                        }
                    }
                }
            }
            else{
                break
            }
        }

        return result
    }

    fun getPointFromLargestVariance(clusters:Collection<CentroidCluster<T>>) : T
    {
        var maxVariance = Double.NEGATIVE_INFINITY
        var selected : Cluster<T>? = null;

        clusters.forEach { cluster->
            if(!cluster.nodes.isEmpty())
            {
                val stat = Variance()

                cluster.nodes.forEach {
                    stat.increment(distance(it, cluster.center))
                }

                val variance = stat.result

                if(variance > maxVariance){
                    maxVariance = variance
                    selected = cluster
                }
            }
        }

        if(selected == null)
            throw ConvergenceException(LocalizedFormats.EMPTY_CLUSTER_IN_K_MEANS)

        val selectedPoints = selected!!.nodes

        return selectedPoints.removeAt(random.nextInt(selectedPoints.size))
    }

    fun getPointFromLargestNumberCluster(clusters:Collection<CentroidCluster<T>>) : T
    {
        var max = 0
        var selected : Cluster<T>? = null;

        clusters.forEach { cluster->
            val size = cluster.nodes.size
            if(size > max)
            {
                max = size
                selected = cluster
            }
        }

        if(selected == null)
            throw ConvergenceException(LocalizedFormats.EMPTY_CLUSTER_IN_K_MEANS)

        val selectedPoints = selected!!.nodes
        return selectedPoints.removeAt(random.nextInt(selectedPoints.size))
    }

    fun getFarthestPoint(clusters:Collection<CentroidCluster<T>>) : T
    {
        var maxDistnace = Double.NEGATIVE_INFINITY
        var selected : Cluster<T>? = null
        var selectedIdx = -1

        clusters.forEach {
            cluster->
            val center = cluster.center

            cluster.nodes.forEachIndexed {
                index, node ->
                val distance = distance(node, center)

                if(distance > maxDistnace)
                {
                    maxDistnace = distance
                    selected = cluster
                    selectedIdx = index
                }
            }
        }

        if(selected == null)
            throw ConvergenceException(LocalizedFormats.EMPTY_CLUSTER_IN_K_MEANS)

        return selected!!.nodes.removeAt(selectedIdx)
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

    fun centroidOf(nodes:Collection<T>) : Clusterable
    {
        var minDistance = Double.NEGATIVE_INFINITY
        var centroid : Clusterable? = null
        var centroidIdx = 0

        nodes.forEachIndexed { outIdx, node ->
            var sum = 0.0
            nodes.filterIndexed { index, t ->  index != outIdx }.forEach {
                sum += distance(it, node)
            }

            if(minDistance > sum)
            {
                minDistance = sum
                centroid = node
            }
        }

        return centroid!!
    }
}