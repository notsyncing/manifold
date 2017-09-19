package io.github.notsyncing.manifold.domain

import io.github.lukehutch.fastclasspathscanner.scanner.ClassInfo
import io.github.lukehutch.fastclasspathscanner.scanner.ScanResult
import io.github.lukehutch.fastclasspathscanner.utils.LogNode
import java.io.File

class ScanResultWrapper(private val scanResult: ScanResult,
                        private val classesToRemove: Set<String>,
                        private val classLoader: ClassLoader? = null) {
    fun getClassLoadersForClass(className: String): List<ClassLoader> {
        return scanResult.getClassLoadersForClass(className)
    }

    fun getMatchProcessorExceptions(): List<Throwable> {
        return scanResult.matchProcessorExceptions
    }

    val uniqueClasspathElements: List<File>
        get() = scanResult.uniqueClasspathElements

    val uniqueClasspathElementsAsPathStr: String
        get() = scanResult.uniqueClasspathElementsAsPathStr

    fun classpathContentsModifiedSinceScan(): Boolean {
        return scanResult.classpathContentsModifiedSinceScan()
    }

    fun classpathContentsLastModifiedTime(): Long {
        return scanResult.classpathContentsLastModifiedTime()
    }

    val classNameToClassInfo: Map<String, ClassInfo>
        get() = scanResult.classNameToClassInfo

    val namesOfAllClasses: List<String>
        get() = scanResult.namesOfAllClasses.apply { this.removeAll(classesToRemove) }

    val namesOfAllStandardClasses: List<String>
        get() = scanResult.namesOfAllStandardClasses.apply { this.removeAll(classesToRemove) }

    fun getNamesOfSubclassesOf(superclassName: String): List<String> {
        return scanResult.getNamesOfSubclassesOf(superclassName).apply { this.removeAll(classesToRemove) }
    }

    fun getNamesOfSubclassesOf(superclass: Class<*>): List<String> {
        return scanResult.getNamesOfSubclassesOf(superclass).apply { this.removeAll(classesToRemove) }
    }

    fun getNamesOfSuperclassesOf(subclassName: String): List<String> {
        return scanResult.getNamesOfSuperclassesOf(subclassName).apply { this.removeAll(classesToRemove) }
    }

    fun getNamesOfSuperclassesOf(subclass: Class<*>): List<String> {
        return scanResult.getNamesOfSuperclassesOf(subclass).apply { this.removeAll(classesToRemove) }
    }

    fun getNamesOfClassesWithFieldOfType(fieldTypeName: String): List<String> {
        return scanResult.getNamesOfClassesWithFieldOfType(fieldTypeName).apply { this.removeAll(classesToRemove) }
    }

    fun getNamesOfClassesWithFieldOfType(fieldType: Class<*>): List<String> {
        return scanResult.getNamesOfClassesWithFieldOfType(fieldType).apply { this.removeAll(classesToRemove) }
    }

    fun getNamesOfClassesWithMethodAnnotation(annotationName: String): List<String> {
        return scanResult.getNamesOfClassesWithMethodAnnotation(annotationName).apply { this.removeAll(classesToRemove) }
    }

    fun getNamesOfClassesWithMethodAnnotation(annotation: Class<*>): List<String> {
        return scanResult.getNamesOfClassesWithMethodAnnotation(annotation).apply { this.removeAll(classesToRemove) }
    }

    fun getNamesOfClassesWithFieldAnnotation(annotationName: String): List<String> {
        return scanResult.getNamesOfClassesWithFieldAnnotation(annotationName).apply { this.removeAll(classesToRemove) }
    }

    fun getNamesOfClassesWithFieldAnnotation(annotation: Class<*>): List<String> {
        return scanResult.getNamesOfClassesWithFieldAnnotation(annotation).apply { this.removeAll(classesToRemove) }
    }

    val namesOfAllInterfaceClasses: List<String>
        get() = scanResult.namesOfAllInterfaceClasses.apply { this.removeAll(classesToRemove) }

    fun getNamesOfSubinterfacesOf(interfaceName: String): List<String> {
        return scanResult.getNamesOfSubinterfacesOf(interfaceName).apply { this.removeAll(classesToRemove) }
    }

    fun getNamesOfSubinterfacesOf(superInterface: Class<*>): List<String> {
        return scanResult.getNamesOfSubinterfacesOf(superInterface).apply { this.removeAll(classesToRemove) }
    }

    fun getNamesOfSuperinterfacesOf(subInterfaceName: String): List<String> {
        return scanResult.getNamesOfSuperinterfacesOf(subInterfaceName).apply { this.removeAll(classesToRemove) }
    }

    fun getNamesOfSuperinterfacesOf(subInterface: Class<*>): List<String> {
        return scanResult.getNamesOfSuperinterfacesOf(subInterface).apply { this.removeAll(classesToRemove) }
    }

    fun getNamesOfClassesImplementing(interfaceName: String): List<String> {
        return scanResult.getNamesOfClassesImplementing(interfaceName).apply { this.removeAll(classesToRemove) }
    }

    fun getNamesOfClassesImplementing(implementedInterface: Class<*>): List<String> {
        return scanResult.getNamesOfClassesImplementing(implementedInterface).apply { this.removeAll(classesToRemove) }
    }

    fun getNamesOfClassesImplementingAllOf(vararg implementedInterfaceNames: String): List<String> {
        return scanResult.getNamesOfClassesImplementingAllOf(*implementedInterfaceNames)
                .apply { this.removeAll(classesToRemove) }
    }

    fun getNamesOfClassesImplementingAllOf(vararg implementedInterfaces: Class<*>): List<String> {
        return scanResult.getNamesOfClassesImplementingAllOf(*implementedInterfaces)
                .apply { this.removeAll(classesToRemove) }
    }

    val namesOfAllAnnotationClasses: List<String>
        get() = scanResult.namesOfAllAnnotationClasses

    fun getNamesOfClassesWithAnnotation(annotationName: String): List<String> {
        return scanResult.getNamesOfClassesWithAnnotation(annotationName).apply { this.removeAll(classesToRemove) }
    }

    fun getNamesOfClassesWithAnnotation(annotation: Class<*>): List<String> {
        return scanResult.getNamesOfClassesWithAnnotation(annotation).apply { this.removeAll(classesToRemove) }
    }

    fun getNamesOfClassesWithAnnotationsAllOf(vararg annotationNames: String): List<String> {
        return scanResult.getNamesOfClassesWithAnnotationsAllOf(*annotationNames)
                .apply { this.removeAll(classesToRemove) }
    }

    fun getNamesOfClassesWithAnnotationsAllOf(vararg annotations: Class<*>): List<String> {
        return scanResult.getNamesOfClassesWithAnnotationsAllOf(*annotations).apply { this.removeAll(classesToRemove) }
    }

    fun getNamesOfClassesWithAnnotationsAnyOf(vararg annotationNames: String): List<String> {
        return scanResult.getNamesOfClassesWithAnnotationsAnyOf(*annotationNames)
                .apply { this.removeAll(classesToRemove) }
    }

    fun getNamesOfClassesWithAnnotationsAnyOf(vararg annotations: Class<*>): List<String> {
        return scanResult.getNamesOfClassesWithAnnotationsAnyOf(*annotations).apply { this.removeAll(classesToRemove) }
    }

    fun getNamesOfAnnotationsOnClass(className: String): List<String> {
        return scanResult.getNamesOfAnnotationsOnClass(className)
    }

    fun getNamesOfAnnotationsOnClass(klass: Class<*>): List<String> {
        return scanResult.getNamesOfAnnotationsOnClass(klass)
    }

    fun getNamesOfAnnotationsWithMetaAnnotation(metaAnnotationName: String): List<String> {
        return scanResult.getNamesOfAnnotationsWithMetaAnnotation(metaAnnotationName)
    }

    fun getNamesOfAnnotationsWithMetaAnnotation(metaAnnotation: Class<*>): List<String> {
        return scanResult.getNamesOfAnnotationsWithMetaAnnotation(metaAnnotation)
    }

    fun generateClassGraphDotFile(sizeX: Float, sizeY: Float): String {
        return scanResult.generateClassGraphDotFile(sizeX, sizeY)
    }

    fun classNamesToClassRefs(classNames: List<String>, ignoreExceptions: Boolean = false): List<Class<*>> {
        return scanResult.classNamesToClassRefs(classNames, ignoreExceptions)
    }

    fun classNameToClassRef(className: String?, ignoreExceptions: Boolean = false): Class<*>? {
        return scanResult.classNameToClassRef(className, ignoreExceptions)
    }

    fun freeTempFiles(log: LogNode) {
        return scanResult.freeTempFiles(log)
    }
}
