package net

import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.httpGet

class RequestBuilder(
        val startDate:String,
        val endDate:String,
        var sort:String = "stars",
                     var order:String = "desc",
                     var page:Int = 1)
{
    private lateinit var q:String
    init {
        q="language:java+size:>=1024+created:$startDate..$endDate+stars:>=10"
    }
    val ID = "78396b3b994c44768999"
    val SECRET = "bc394a9573d3c44a77cd4df9f27dde377fc2ed5a"

    fun build() : Request
    {
        return "https://api.github.com/search/repositories?q=$q&sort=$sort&order=$order&page=$page&client_id=$ID&client_secret=$SECRET".httpGet();
    }

    private fun getParams() : List<Pair<String, Any>>
    {
        return listOf(
                Pair("q", q), Pair("sort", sort),
                Pair("order", order), Pair("page", page),
                Pair("client_id", ID) , Pair("client_secret", SECRET)
        );
    }
}
