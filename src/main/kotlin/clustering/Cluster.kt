package clustering

open class Cluster<T:Clusterable>(val nodes:MutableList<T>)
{
    fun addNode(node:T)
    {
        nodes.add(node)
    }
}

class CentroidCluster<T:Clusterable>(val center:Clusterable ,nodes:MutableList<T>) : Cluster<T>(nodes)
{

}
