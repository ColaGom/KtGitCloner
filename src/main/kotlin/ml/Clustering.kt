package ml

open class Clustering(val k:Int)
{
    lateinit var name:String

    init {
        name = this.javaClass.simpleName
    }

    var startTime = 0L
    val clusters:MutableList<Cluster> = mutableListOf()

    open fun clustering(nodeList:List<Node>)
    {
        startTime = System.currentTimeMillis()
        log("start clustering size : ${nodeList.size} k : $k")
    }

    fun log(msg:String)
    {
        println("[$name] $msg ${System.currentTimeMillis() - startTime}")
    }

    fun getSSE() : Double
    {
        var sse = 0.0

        clusters.forEach {
            val mean = it.getCentroid().mean(it.getNodes())
            if(!mean.isNaN())
                sse += Math.pow(mean,2.0)
        }

        return sse
    }

    fun getSSE(clusters : List<Cluster>) : Double
    {
        var sse = 0.0

        clusters.forEach {
            val mean = it.getCentroid().mean(it.getNodes())
            if(!mean.isNaN())
                sse += Math.pow(mean,2.0)
        }

        return sse
    }

    fun printAnalysis()
    {
        log("clustering sse : ${getSSE()}")
    }
}
