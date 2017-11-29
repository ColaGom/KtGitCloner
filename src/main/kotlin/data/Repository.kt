package data

import com.github.kittinunf.fuel.core.ResponseDeserializable
import com.google.gson.Gson
import common.Projects
import java.io.File
import java.nio.file.Paths

data class Repository(
        val id : Int,
        val name : String,
        val full_name : String,
        val private : String,
        val html_url : String,
        val description : String,
        val fork : String,
        val url : String,
        val forks_url : String,
        val keys_url : String,
        val collaborators_url : String,
        val teams_url : String,
        val hooks_url : String,
        val issue_events_url : String,
        val events_url : String,
        val assignees_url : String,
        val branches_url : String,
        val tags_url : String,
        val blobs_url : String,
        val git_tags_url : String,
        val git_refs_url : String,
        val trees_url : String,
        val statuses_url : String,
        val languages_url : String,
        val stargazers_url : String,
        val contributors_url : String,
        val subscribers_url : String,
        val subscription_url : String,
        val commits_url : String,
        val git_commits_url : String,
        val comments_url : String,
        val issue_comment_url : String,
        val contents_url : String,
        val compare_url : String,
        val merges_url : String,
        val archive_url : String,
        val downloads_url : String,
        val issues_url : String,
        val pulls_url : String,
        val milestones_url : String,
        val notifications_url : String,
        val labels_url : String,
        val releases_url : String,
        val deployments_url : String,
        val created_at : String,
        val updated_at : String,
        val pushed_at : String,
        val git_url : String,
        val ssh_url : String,
        val clone_url : String,
        val svn_url : String,
        val homepage : String,
        val size : Int,
        val stargazers_count : Int,
        val watchers_count : Int,
        val language : String,
        val has_issues : String,
        val has_projects : String,
        val has_downloads : String,
        val has_wiki : String,
        val has_pages : String,
        val forks_count : String,
        val mirror_url : String,
        val open_issues_count : String,
        val forks : String,
        val open_issues : String,
        val watchers : Int,
        val default_branch : String,
        val score : Float,
        val Owner : Owner
) {
    fun toRoot() : File
    {
        return File(Projects.ROOT2 , full_name);
    }

    class Deserializer : ResponseDeserializable<Repository> {
        override fun deserialize(content: String): Repository? {
            return Gson().fromJson(content, Repository::class.java);
        }
    }
}

data class Owner(
        val login : String,
        val id : Int,
        val avatar_url : String,
        val gravatar_id : String,
        val url : String,
        val html_url : String,
        val followers_url : String,
        val following_url : String,
        val gists_url : String,
        val starred_url : String,
        val subscriptions_url : String,
        val organizations_url : String,
        val repos_url : String,
        val events_url : String,
        val received_events_url : String,
        val type : String,
        val site_admin : String
)
