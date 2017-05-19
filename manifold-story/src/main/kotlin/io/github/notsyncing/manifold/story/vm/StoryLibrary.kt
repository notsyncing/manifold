package io.github.notsyncing.manifold.story.vm

import java.io.InvalidObjectException
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

object StoryLibrary {
    private val chapters = ConcurrentHashMap<String, Chapter>()

    fun read(storyFile: Path) {
        val parser = StoryParser.parse(Files.newInputStream(storyFile))

        parser.chapters.forEach {
            chapters[it.name] = it
        }
    }

    fun tell(sceneName: String) {
        val chapter = chapters[sceneName]

        if (chapter == null) {
            throw NoSuchMethodException("Scene $sceneName not found!")
        }

        if (chapter.type != ChapterType.Scene) {
            throw InvalidObjectException("Chapter $sceneName is not a scene!")
        }

        val interpreter = StoryInterpreter(chapter.content, chapter.labels)
        interpreter.run()
    }
}