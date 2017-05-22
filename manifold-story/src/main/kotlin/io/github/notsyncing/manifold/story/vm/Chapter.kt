package io.github.notsyncing.manifold.story.vm

class Chapter(val name: String,
              val type: ChapterType,
              val content: MutableList<Sentence> = mutableListOf(),
              val labels: MutableMap<String, Sentence> = mutableMapOf(),
              var noPersist: Boolean = false) {
}