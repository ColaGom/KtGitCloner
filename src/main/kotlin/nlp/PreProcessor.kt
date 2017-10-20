package nlp

import common.Stopwords
import mapInPlace
import org.tartarus.snowball.ext.englishStemmer

class PreProcessor(var raw:String)
{
    companion object {
        val regComment = Regex(	"(?:/\\*(?:[^*]|(?:\\*+[^*/]))*\\*+/)|(?://.*)")
        val regAnnotate = Regex("@.*\\b");
        val regNonAlphanum = Regex("[^a-zA-Z]");
        val regCamelCase = Regex(String.format("%s|%s|%s",
                "(?<=[A-Z])(?=[A-Z][a-z])",
                "(?<=[^A-Z])(?=[A-Z])",
                "(?<=[A-Za-z])(?=[^A-Za-z])"
        ));
    }

    fun step1()
    {
        if(raw.length > 2000) {
            val sub : String = raw.substring(0, 1000);
            if(regComment.find(sub)?.value?.toLowerCase()?.contains("license") == true)
                raw = regComment.replaceFirst(sub, "") + raw.substring(1000)
        }
        else {
            if(regComment.find(raw)?.value?.toLowerCase()?.contains("license") == true)
                raw = regComment.replaceFirst(raw, "")
        }
    }

    fun run() : List<String>
    {
        val stemmer = englishStemmer()

        step1();

        raw = regAnnotate.replace(raw, "")
        raw = regNonAlphanum.replace(raw, " ")

        var result : MutableList<String> = raw.split(" ").filter { it.length > 2 && !Stopwords.instance.contains(it.toLowerCase()) }.toMutableList()
        result.mapInPlace { regCamelCase.replace(it, " ").toLowerCase()  }

        return result .filter { it.length > 2 && !Stopwords.instance.contains(it) }
    }
}
