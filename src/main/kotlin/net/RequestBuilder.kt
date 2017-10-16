package net

import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.httpGet

class RequestBuilder(var language:String = "language:java",
                     var sort:String = "stars",
                     var order:String = "desc",
                     var page:Int = 1)
{
    val ID = "78396b3b994c44768999"
    val SECRET = "bc394a9573d3c44a77cd4df9f27dde377fc2ed5a"

    fun build() : Request
    {
        return "https://api.github.com/search/repositories?".httpGet(getParams());
    }

    private fun getParams() : List<Pair<String, Any>>
    {
        return listOf(
                Pair("q", language), Pair("sort", sort),
                Pair("order", order), Pair("page", page),
                Pair("client_id", ID) , Pair("client_secret", SECRET)
        );
    }
}
