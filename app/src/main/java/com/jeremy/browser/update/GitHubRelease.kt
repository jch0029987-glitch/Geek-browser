package com.jeremy.browser.update

data class GitHubRelease(
    val tagName: String,
    val releaseNotes: String,
    val downloadUrl: String
)
