package io.github.notsyncing.manifold.story

import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.domain.ManifoldDomain
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.future.future
import java.util.concurrent.ConcurrentHashMap

object StoryLibrary {
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

        ManifoldDomain.afterScan {
            it.inAllClassScanResults { s, cl ->
                s?.getNamesOfClassesWithAnnotation(AfterStoryOf::class.java)?.forEach { className ->
                    val clazz = cl.loadClass(className) as Class<AfterStory>
                    val forStories = clazz.getAnnotation(AfterStoryOf::class.java).value

                    for (forStory in forStories) {
                        if (!afterStories.containsKey(forStory)) {
                            afterStories[forStory] = mutableListOf()
                        }

                        afterStories[forStory]!!.add(clazz)
                    }
                }
            }
        }

        ManifoldDomain.onClose { _, cl ->
            afterStories.forEachValue(1) {
                it.removeIf { it.classLoader == cl }
            }
        }
    }

    fun reset() {
        afterStories.clear()
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