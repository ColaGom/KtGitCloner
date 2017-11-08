package nlp

import common.KEY_COMMENT
import common.KEY_SOURCE
import common.Stopwords
import data.SourceFile
import org.tartarus.snowball.ext.englishStemmer

class PreProcessor(var raw: String) {
    companion object {
        val regComment = Regex("(?:/\\*(?:[^*]|(?:\\*+[^*/]))*\\*+/)|(?://.*)")
        val regComment2 = Regex("/\\*(?:.|[\\r\\n])*?\\*/")
        val regAnnotate = Regex("@.*\\b")
        val regNonAlphanum = Regex("[^a-zA-Z]")
        val regCamelCase = Regex(String.format("%s|%s|%s",
                "(?<=[A-Z])(?=[A-Z][a-z])(\\B)",
                "(?<=[^A-Z])(?=[A-Z])(\\B)",
                "(?<=[A-Za-z])(?=[^A-Za-z])(\\B)"
        ))

        val regHtml = Regex("<[^>]*>")
    }

    fun toSourceFile(path : String): SourceFile {
        var wordMap: HashMap<String, HashMap<String, Int>> = hashMapOf()
        var srcLen = 0
        var comLen = 0

        var strComment = ""
        var strSource = ""

        val matcher = regComment.toPattern().matcher(raw)

        while (matcher.find()) {
            val str = matcher.group();

            if (!str.contains("license", true) && !str.contains("copyright", true)) {
                strComment += str + " ";
            }
        }

        strSource = regComment.replace(raw, "");

        srcLen = strSource.length
        comLen = strComment.length

        strSource = regAnnotate.replace(strSource, "")
        strSource = regNonAlphanum.replace(strSource, " ")
        strSource = regCamelCase.replace(strSource, " ")

        strComment = regAnnotate.replace(strComment, "")
        strComment = regHtml.replace(strComment, "")
        strComment = regNonAlphanum.replace(strComment, " ")
        strComment = regCamelCase.replace(strComment, " ")

        val stemmer = englishStemmer()
        val srcFreqMap : HashMap<String, Int> = hashMapOf()
        val comFreqMap : HashMap<String, Int> = hashMapOf()

        strSource.toLowerCase().split(" ").filter { it.length > 2 && it.length < 20 && !Stopwords.instance.contains(it) }.toList().forEach {
            stemmer.setCurrent(it)
            stemmer.stem()
            val key = stemmer.current
            if(srcFreqMap.containsKey(key))
                srcFreqMap.set(key, srcFreqMap.get(key)!! + 1)
            else
                srcFreqMap.put(key, 1)
        }

        strComment.toLowerCase().split(" ").filter { it.length > 2 && it.length < 20 && !Stopwords.instance.contains(it) }.toList().forEach {
            stemmer.setCurrent(it)
            stemmer.stem()
            val key = stemmer.current
            if(comFreqMap.containsKey(key))
                comFreqMap.set(key, comFreqMap.get(key)!! + 1)
            else
                comFreqMap.put(key, 1)
        }

        wordMap.put(KEY_SOURCE, srcFreqMap)
        wordMap.put(KEY_COMMENT, comFreqMap)

        return SourceFile(path, comLen, srcLen, wordMap)
    }


    fun step1() {
        // avoid to overflow
        if (raw.length > 2000) {
            val sub: String = raw.substring(0, 1000);
            if (regComment.find(sub)?.value?.toLowerCase()?.contains("license") == true)
                raw = regComment.replaceFirst(sub, "") + raw.substring(1000)
        } else {
            if (regComment.find(raw)?.value?.toLowerCase()?.contains("license") == true)
                raw = regComment.replaceFirst(raw, "")
        }
    }

    fun splitLargeStr(splen: Int, str: String): List<String> {
        var result: MutableList<String> = mutableListOf()
        var current = 0

        while (true) {
            var find = false

            for (i in splen + current downTo current + 1) {
                if (str[i - 1] == '\r' && str[i] == '\n') {
                    result.add(str.substring(current, i))
                    current = i
                    find = true
                    break
                }
            }

            if (!find && current + splen < str.length) {
                result.add(str.substring(current, current + splen))
                current += splen
            }

            if (str.length - current <= splen) {
                result.add(str.substring(current))
                break
            }
        }

        return result
    }

    fun run2(): HashMap<String, List<String>> {
        var result: HashMap<String, List<String>> = hashMapOf()
        var strComment = ""
        var strSource = ""

        val matcher = regComment.toPattern().matcher(raw)

        while (matcher.find()) {
            val str = matcher.group();

            if (!str.contains("license", true) && !str.contains("copyright", true)) {
                strComment += str + " ";
            }
        }

        strSource = regComment.replace(raw, "");

        strSource = regAnnotate.replace(strSource, "")
        strSource = regNonAlphanum.replace(strSource, " ")
        strSource = regCamelCase.replace(strSource, " ")

        strComment = regAnnotate.replace(strComment, "")
        strComment = regHtml.replace(strComment, "")
        strComment = regNonAlphanum.replace(strComment, " ")
        strComment = regCamelCase.replace(strComment, " ")

        result.put(KEY_SOURCE, strSource.toLowerCase().split(" ").filter { it.length > 2 && !Stopwords.instance.contains(it) }.toMutableList())
        result.put(KEY_COMMENT, strComment.toLowerCase().split(" ").filter { it.length > 2 && !Stopwords.instance.contains(it) }.toMutableList())

        return result;
    }

    fun run(): List<String> {
        var result: MutableList<String> = mutableListOf()
        var strComment = ""
        var strSource = ""

        step1();

//        regComment.findAll(raw).forEach {
//            strComment += it.value
//        }
//        strSource = regComment.replace(raw, "")

//        if (raw.length < 10000) {
//            regComment.findAll(raw).forEach {
//                strComment += it.value
//            }
//            strSource = regComment.replace(raw, "")
//        } else {
//            splitLargeStr(3000, raw).forEach({
//
//                regComment.findAll(it).forEach {
//                    strComment += it.value
//                }
//
//                strSource += regComment.replace(it, "") + " "
//            })
//        }


//        val mr = regComment2.find(raw)?.value
//        do {
//            val mr = regComment2.find(raw)?.value
//
//            if (mr == null)
//                break
//            else {
//                strComment += mr
//                raw = regComment2.replace(raw, "")
//            }
//        } while (true)

//


        raw = regAnnotate.replace(raw, "")
        raw = regHtml.replace(raw, "")
        raw = regNonAlphanum.replace(raw, " ")
        raw = regCamelCase.replace(raw, " ")

        return raw.toLowerCase().split(" ").filter { it.length > 2 && !Stopwords.instance.contains(it) }.toMutableList()
    }
}
