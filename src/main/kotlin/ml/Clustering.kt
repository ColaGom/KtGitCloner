package ml

open abstract class Clustering(k:Int)
{
    val clusters:MutableList<Cluster> = mutableListOf()

    abstract fun clustering(nodeList:List<Node>);
}
