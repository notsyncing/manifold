package io.github.notsyncing.manifold.action.interceptors

import io.github.notsyncing.manifold.action.ManifoldAction
import io.github.notsyncing.manifold.action.ManifoldScene
import io.github.notsyncing.manifold.domain.ManifoldDomain
import io.github.notsyncing.manifold.utils.removeIf
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class InterceptorManager {
    private val sceneInterceptors = ConcurrentHashMap.newKeySet<Class<SceneInterceptor>>()
    private val sceneInterceptorMap = ConcurrentHashMap<Class<ManifoldScene<*>>, ArrayList<SceneInterceptorInfo>>()
    private val actionInterceptors = ConcurrentHashMap.newKeySet<Class<ActionInterceptor>>()
    private val actionInterceptorMap = ConcurrentHashMap<Class<ManifoldAction<*>>, ArrayList<ActionInterceptorInfo>>()

    fun init() {
        ManifoldDomain.onClose { _, cl ->
            sceneInterceptors.removeIf { it.classLoader == cl }
            sceneInterceptorMap.removeIf { (clazz, _) -> clazz.classLoader == cl }
            actionInterceptors.removeIf { it.classLoader == cl }
            actionInterceptorMap.removeIf { (clazz, _) -> clazz.classLoader == cl }
        }
    }

    fun reset() {
        sceneInterceptors.clear()
        sceneInterceptorMap.clear()
        actionInterceptors.clear()
        actionInterceptorMap.clear()
    }

    fun addSceneInterceptor(c: Class<SceneInterceptor>) {
        sceneInterceptors.add(c)
    }

    fun addActionInterceptor(c: Class<ActionInterceptor>) {
        actionInterceptors.add(c)
    }

    fun getInterceptorsForScene(sceneClass: Class<ManifoldScene<*>>): List<SceneInterceptorInfo> {
        if (!sceneInterceptorMap.containsKey(sceneClass)) {
            val addToList = { info: SceneInterceptorInfo ->
                var list = sceneInterceptorMap[sceneClass]

                if (list == null) {
                    list = ArrayList()
                    sceneInterceptorMap[sceneClass] = list
                }

                list.add(info)
            }

            sceneInterceptors.forEach {
                if (it.isAnnotationPresent(ForEveryScene::class.java)) {
                    addToList(SceneInterceptorInfo(it, null))
                    return@forEach
                }

                val forScenes = it.getAnnotation(ForScenes::class.java)

                if (forScenes != null) {
                    for (cl in forScenes.value) {
                        if (cl.java == sceneClass) {
                            addToList(SceneInterceptorInfo(it, null))
                            break
                        }
                    }
                }

                val forAnnos = it.getAnnotation(ForScenesAnnotatedWith::class.java)

                if (forAnnos != null) {
                    for (cl in forAnnos.value) {
                        val a = sceneClass.getAnnotation(cl.java as Class<Annotation>)

                        if (a != null) {
                            addToList(SceneInterceptorInfo(it, a))
                        }
                    }
                }
            }

            if (!sceneInterceptorMap.containsKey(sceneClass)) {
                sceneInterceptorMap.put(sceneClass, ArrayList())
            }
        }

        return sceneInterceptorMap[sceneClass]!!
    }

    fun getInterceptorsForAction(actionClass: Class<ManifoldAction<*>>): List<ActionInterceptorInfo> {
        if (!actionInterceptorMap.containsKey(actionClass)) {
            val addToList = { info: ActionInterceptorInfo ->
                var list = actionInterceptorMap[actionClass]

                if (list == null) {
                    list = ArrayList()
                    actionInterceptorMap[actionClass] = list
                }

                list.add(info)
            }

            actionInterceptors.forEach {
                if (it.isAnnotationPresent(ForEveryAction::class.java)) {
                    addToList(ActionInterceptorInfo(it, null))
                    return@forEach
                }

                val forActions = it.getAnnotation(ForActions::class.java)

                if (forActions != null) {
                    for (cl in forActions.value) {
                        if (cl.java == actionClass) {
                            addToList(ActionInterceptorInfo(it, null))
                            break
                        }
                    }
                }

                val forAnnos = it.getAnnotation(ForActionsAnnotatedWith::class.java)

                if (forAnnos != null) {
                    for (cl in forAnnos.value) {
                        val a = actionClass.getAnnotation(cl.java as Class<Annotation>)

                        if (a != null) {
                            addToList(ActionInterceptorInfo(it, a))
                        }
                    }
                }
            }

            if (!actionInterceptorMap.containsKey(actionClass)) {
                actionInterceptorMap.put(actionClass, ArrayList())
            }
        }

        return actionInterceptorMap[actionClass]!!
    }
}