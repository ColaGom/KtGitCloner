package data

import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.google.gson.Gson

data class Repository(val full_name : String,val size : Int, val watchers : Int, val clone_url : String,
                      val fork : Boolean, val url:String, val html_url:String) {

    class Deserializer : ResponseDeserializable<Repository> {
        override fun deserialize(content: String): Repository? {
            return Gson().fromJson(content, Repository::class.java);
        }
    }
}