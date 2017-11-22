package clustering

interface Clusterable {
    fun getMap():HashMap<String, Double>
}

interface DistanceMeasure
{
    fun compute(v1:HashMap<String, Double>, v2:HashMap<String, Double>) : Double
}

abstract class Clusterer<T:Clusterable> (val measure:DistanceMeasure) {
    abstract fun cluster(var1:Collection<T>) : List<Cluster<T>>

    fun distance(v1:Clusterable, v2:Clusterable) : Double
    {
        return measure.compute(v1.getMap(), v2.getMap())
    }
}