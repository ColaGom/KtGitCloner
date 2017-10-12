package data

import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.google.gson.Gson

data class Repository(var size : Int, var watchers : Int, var clone_url : String) {

    class Deserializer : ResponseDeserializable<Repository> {
        override fun deserialize(content: String): Repository? {
            return Gson().fromJson(content, Repository::class.java);
        }
    }
}