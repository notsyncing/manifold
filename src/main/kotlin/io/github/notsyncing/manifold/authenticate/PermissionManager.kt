package io.github.notsyncing.manifold.authenticate

import io.github.notsyncing.manifold.utils.removeIf
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger

class PermissionManager {
    companion object {
        @Deprecated("Only for legacy applications still using enums as permissions")
        var useIntegerAsPermissions = false
    }

    private val permissionModules = ConcurrentHashMap<String, MutableList<PermissionInfo>>()
    private val permissionTypes = ConcurrentHashMap<String, MutableList<PermissionInfo>>()

    private val classPermissionModules = ConcurrentHashMap<Class<*>, List<PermissionInfo>>()
    private val classPermissionTypes = ConcurrentHashMap<Class<*>, List<PermissionInfo>>()

    private val customPermissions = ConcurrentHashMap<String, CustomPermissionInfo>()

    private val logger = Logger.getLogger(javaClass.simpleName)

    fun init() {

    }

    fun reset() {
        permissionModules.clear()
        permissionTypes.clear()

        classPermissionModules.clear()
        classPermissionTypes.clear()

        customPermissions.clear()
    }

    fun addPermissionModuleDescriptions(clazz: Class<*>) {
        if (!clazz.isAnnotationPresent(PermissionModuleDescriptionContainer::class.java)) {
            logger.warning("Class $clazz does not has ${PermissionModuleDescriptionContainer::class.java} " +
                    "annotation, skip adding")
            return
        }

        val list = mutableListOf<Pair<PermissionModuleDescription, PermissionInfo>>()

        for (f in clazz.declaredFields) {
            if (!f.isAnnotationPresent(PermissionModuleDescription::class.java)) {
                continue
            }

            val anno = f.getAnnotation(PermissionModuleDescription::class.java)
            val info = PermissionInfo(anno.value, f.get(null).toString(), anno.internal)

            list.add(Pair(anno, info))
        }

        if (list.isEmpty()) {
            logger.warning("Class $clazz does not has any ${PermissionModuleDescription::class.java} annotated fields!")
        } else {
            list.forEach { (desc, value) ->
                if (!permissionModules.containsKey(desc.group)) {
                    permissionModules[desc.group] = mutableListOf()
                }

                permissionModules[desc.group]!!.add(value)
            }

            classPermissionModules.put(clazz, list.map { (_, v) -> v })
        }
    }

    fun addPermissionTypeDescriptions(clazz: Class<*>) {
        if (!clazz.isAnnotationPresent(PermissionTypeDescriptionContainer::class.java)) {
            logger.warning("Class $clazz does not has ${PermissionTypeDescriptionContainer::class.java} " +
                    "annotation, skip adding")
            return
        }

        val list = mutableListOf<Pair<PermissionTypeDescription, PermissionInfo>>()

        for (f in clazz.declaredFields) {
            if (!f.isAnnotationPresent(PermissionTypeDescription::class.java)) {
                continue
            }

            val anno = f.getAnnotation(PermissionTypeDescription::class.java)
            val info = PermissionInfo(anno.value, f.get(null).toString(), anno.internal)

            list.add(Pair(anno, info))
        }

        if (list.isEmpty()) {
            logger.warning("Class $clazz does not has any ${PermissionTypeDescription::class.java} annotated fields!")
        } else {
            list.forEach { (desc, value) ->
                if (!permissionTypes.containsKey(desc.group)) {
                    permissionTypes[desc.group] = mutableListOf()
                }

                permissionTypes[desc.group]!!.add(value)
            }

            classPermissionTypes.put(clazz, list.map { (_, value) -> value })
        }
    }

    fun removePermissionsByClassLoader(cl: ClassLoader) {
        classPermissionModules.filterKeys { it.classLoader == cl }
                .forEach { _, list -> permissionModules.forEach { it.value.removeAll(list) } }

        classPermissionTypes.filterKeys { it.classLoader == cl }
                .forEach { _, list -> permissionTypes.forEach { it.value.removeAll(list) } }

        classPermissionModules.removeIf { it.key.classLoader == cl }
        classPermissionTypes.removeIf { it.key.classLoader == cl }

        permissionModules.removeIf { it.value.isEmpty() }
        permissionTypes.removeIf { it.value.isEmpty() }
    }

    fun getPermissionModules() = permissionModules

    fun getPermissionTypes() = permissionTypes

    fun getCustomPermission(name: String) = customPermissions[name]

    fun getCustomPermissions() = customPermissions.values.toList()

    fun registerCustomPermission(info: CustomPermissionInfo) {
        customPermissions[info.name] = info
    }
}