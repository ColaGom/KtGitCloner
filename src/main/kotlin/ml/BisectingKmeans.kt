package ml

class BisectingKmeans(k:Int, val iter:Int = 3) : Clustering(k)
{
    override fun clustering(nodeList:List<Node>) {
        super.clustering(nodeList)

//        var currentNodeList = nodeList
//
//        while(true)
//        {
//            var smallestSSE = Double.MAX_VALUE
//            var bestClusters : List<Cluster> = listOf()
//            val kmean = Kmeans(2)
//
//            for(i in 1..iter)
//            {
//                kmean.clustering(currentNodeList)
//                val sse = kmean.getSSE()
//
//                if(sse<smallestSSE)
//                {
//                    smallestSSE = sse;
//                    bestClusters = kmean.clusters
//                }
//            }
//
//            clusters.addAll(bestClusters)
//
//            if(clusters.size == k)
//                break
//
//            var maxSize = -1
//            var maxIdx = -1
//
//            clusters.forEachIndexed {
//                index, cluster ->
//
//                if(cluster.size() > maxSize)
//                {
//                    maxSize = cluster.size()
//                    maxIdx = index
//                }
//            }
//
//            currentNodeList = clusters.get(maxIdx).memberList
//            clusters.removeAt(maxIdx)
//        }
//
//        log("Finished clustering")
        printAnalysis()
    }
}