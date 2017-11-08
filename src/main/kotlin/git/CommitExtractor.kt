package git

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

class CommitExtractor(val rootPath:String)
{
    val repo : Repository
    init {
        repo = FileRepositoryBuilder().findGitDir(File(rootPath)).build()
    }

    fun parsePersonInfo(personIdent: PersonIdent): PersonInfo {
        val personInfo = PersonInfo(personIdent.name, personIdent.emailAddress, personIdent.`when`.toString())
        return personInfo
    }

    private fun getDiffPatchScript(diffEntry: DiffEntry): String {
        val outputStream = ByteArrayOutputStream()
        val format = DiffFormatter(outputStream)
        format.setRepository(this.repo)

        try {
            format.format(diffEntry)
        } catch (var5: IOException) {
            var5.printStackTrace()
        }

        val result = outputStream.toString()
        outputStream.close()
        return result
    }

    fun parseDiffFiles(commit: RevCommit) : Map<String, List<DiffInfo>>
    {
        val curTree = commit.tree
        val mapChangeInfo : MutableMap<String, List<DiffInfo>> = mutableMapOf()

        for (i in 0 until commit.parentCount) {
            val prevCommit = commit.getParent(i)
            val prevTree = prevCommit.tree
            val objectReader = this.repo.newObjectReader()
            val prevTreeParser = CanonicalTreeParser()
            val currentTreParser = CanonicalTreeParser()
            prevTreeParser.reset(objectReader, prevTree.id)
            currentTreParser.reset(objectReader, curTree.id)
            val git = Git(this.repo)
            val diffCommand = git.diff().setOldTree(prevTreeParser).setNewTree(currentTreParser)
            val listDiffEntry = diffCommand.call()
            val listChangeInfos : MutableList<DiffInfo> = mutableListOf()
            val var15 = listDiffEntry.iterator()

            while (var15.hasNext()) {
                val diffEntry = var15.next() as DiffEntry
                val diffInfo = DiffInfo(diffEntry.changeType.name, diffEntry.oldPath, diffEntry.newPath, getDiffPatchScript(diffEntry))
                listChangeInfos.add(diffInfo)
            }

            mapChangeInfo.put(prevCommit.name, listChangeInfos)
        }

        return mapChangeInfo
    }

    fun extractCommitMetaData(limit:Int = 4000): List<CommitMeta> {
        val git = Git(this.repo)
        val itr = git.log().all().call()
        val listCommitMeta : MutableList<git.CommitMeta>  = mutableListOf()
        val var5 = itr.iterator()

        while (var5.hasNext()) {
            val commit = var5.next() as RevCommit
            val commitMeta = CommitMeta(
                    commit.name(),
                    parsePersonInfo(commit.authorIdent),
                    parsePersonInfo(commit.committerIdent),
                    commit.fullMessage,
                    parseDiffFiles(commit)
                    )
            listCommitMeta.add(commitMeta)

            if(listCommitMeta.size > limit)
                break
        }

        return listCommitMeta
    }
}

data class PersonInfo(val name:String, val email:String, val date:String)
data class DiffInfo(val changeType:String, val oldPath:String, val newPath:String, val patchScript:String)
data class CommitMeta(val sha:String, val author:PersonInfo, val committer:PersonInfo, val message:String,var diffs: Map<String, List<DiffInfo>>)

