package io.github.notsyncing.manifold.story.vm

import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.story.AfterStory
import io.github.notsyncing.manifold.story.AfterStoryOf
import io.github.notsyncing.manifold.story.StoryScene
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.future.future
import java.io.InvalidObjectException
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

object StoryLibrary {
    private val chapters = ConcurrentHashMap<String, Chapter>()
    private val afterStories = ConcurrentHashMap<String, MutableList<Class<AfterStory>>>()

    fun init() {
        Manifold.dependencyProvider!!.getAllSubclasses(AfterStory::class.java) {
            if (!it.isAnnotationPresent(AfterStoryOf::class.java)) {
                return@getAllSubclasses
            }

            val forStories = it.getAnnotation(AfterStoryOf::class.java).value

            for (s in forStories) {
                if (!afterStories.containsKey(s)) {
                    afterStories[s] = mutableListOf()
                }

                afterStories[s]!!.add(it)
            }
        }
    }

    fun reset() {
        chapters.clear()
        afterStories.clear()
    }

    fun read(storyFile: Path) {
        val parser = StoryParser.parse(Files.newInputStream(storyFile))

        parser.chapters.forEach {
            chapters[it.name] = it
        }
    }

    fun get(sceneName: String): Chapter {
        val chapter = chapters[sceneName]

        if (chapter == null) {
            throw NoSuchMethodException("Scene $sceneName not found!")
        }

        if (chapter.type != ChapterType.Scene) {
            throw InvalidObjectException("Chapter $sceneName is not a scene!")
        }

        return chapter
    }

    fun tell(sceneName: String, sessionIdentifier: String?) = future {
        val scene = StoryScene(sceneName)
        val r = Manifold.run(scene, sessionIdentifier).await()

        Pair(scene.taskId ?: "", r)
    }

    fun afterStories(sceneName: String): List<Class<AfterStory>> {
        return afterStories[sceneName] ?: emptyList()
    }
}