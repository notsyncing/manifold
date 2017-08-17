package io.github.notsyncing.manifold.di

import io.github.lukehutch.fastclasspathscanner.scanner.ClassInfo
import io.github.lukehutch.fastclasspathscanner.scanner.ScanResult
import io.github.lukehutch.fastclasspathscanner.utils.LogNode
import java.io.File

class ScanResultWrapper(private val scanResult: ScanResult,
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
        get() = scanResult.getNamesOfAllClasses(classLoader)

    val namesOfAllStandardClasses: List<String>
        get() = scanResult.getNamesOfAllStandardClasses(classLoader)

    fun getNamesOfSubclassesOf(superclassName: String): List<String> {
        return scanResult.getNamesOfSubclassesOf(superclassName, classLoader)
    }

    fun getNamesOfSubclassesOf(superclass: Class<*>): List<String> {
        return scanResult.getNamesOfSubclassesOf(superclass, classLoader)
    }

    fun getNamesOfSuperclassesOf(subclassName: String): List<String> {
        return scanResult.getNamesOfSuperclassesOf(subclassName, classLoader)
    }

    fun getNamesOfSuperclassesOf(subclass: Class<*>): List<String> {
        return scanResult.getNamesOfSuperclassesOf(subclass, classLoader)
    }

    fun getNamesOfClassesWithFieldOfType(fieldTypeName: String): List<String> {
        return scanResult.getNamesOfClassesWithFieldOfType(fieldTypeName, classLoader)
    }

    fun getNamesOfClassesWithFieldOfType(fieldType: Class<*>): List<String> {
        return scanResult.getNamesOfClassesWithFieldOfType(fieldType, classLoader)
    }

    fun getNamesOfClassesWithMethodAnnotation(annotationName: String): List<String> {
        return scanResult.getNamesOfClassesWithMethodAnnotation(annotationName, classLoader)
    }

    fun getNamesOfClassesWithMethodAnnotation(annotation: Class<*>): List<String> {
        return scanResult.getNamesOfClassesWithMethodAnnotation(annotation, classLoader)
    }

    fun getNamesOfClassesWithFieldAnnotation(annotationName: String): List<String> {
        return scanResult.getNamesOfClassesWithFieldAnnotation(annotationName, classLoader)
    }

    fun getNamesOfClassesWithFieldAnnotation(annotation: Class<*>): List<String> {
        return scanResult.getNamesOfClassesWithFieldAnnotation(annotation, classLoader)
    }

    val namesOfAllInterfaceClasses: List<String>
        get() = scanResult.getNamesOfAllInterfaceClasses(classLoader)

    fun getNamesOfSubinterfacesOf(interfaceName: String): List<String> {
        return scanResult.getNamesOfSubinterfacesOf(interfaceName, classLoader)
    }

    fun getNamesOfSubinterfacesOf(superInterface: Class<*>): List<String> {
        return scanResult.getNamesOfSubinterfacesOf(superInterface, classLoader)
    }

    fun getNamesOfSuperinterfacesOf(subInterfaceName: String): List<String> {
        return scanResult.getNamesOfSuperinterfacesOf(subInterfaceName, classLoader)
    }

    fun getNamesOfSuperinterfacesOf(subInterface: Class<*>): List<String> {
        return scanResult.getNamesOfSuperinterfacesOf(subInterface, classLoader)
    }

    fun getNamesOfClassesImplementing(interfaceName: String): List<String> {
        return scanResult.getNamesOfClassesImplementing(interfaceName, classLoader)
    }

    fun getNamesOfClassesImplementing(implementedInterface: Class<*>): List<String> {
        return scanResult.getNamesOfClassesImplementing(implementedInterface, classLoader)
    }

    fun getNamesOfClassesImplementingAllOf(vararg implementedInterfaceNames: String): List<String> {
        return scanResult.getNamesOfClassesImplementingAllOf(classLoader, *implementedInterfaceNames)
    }

    fun getNamesOfClassesImplementingAllOf(vararg implementedInterfaces: Class<*>): List<String> {
        return scanResult.getNamesOfClassesImplementingAllOf(classLoader, *implementedInterfaces)
    }

    val namesOfAllAnnotationClasses: List<String>
        get() = scanResult.namesOfAllAnnotationClasses

    fun getNamesOfClassesWithAnnotation(annotationName: String): List<String> {
        return scanResult.getNamesOfClassesWithAnnotation(annotationName, classLoader)
    }

    fun getNamesOfClassesWithAnnotation(annotation: Class<*>): List<String> {
        return scanResult.getNamesOfClassesWithAnnotation(annotation, classLoader)
    }

    fun getNamesOfClassesWithAnnotationsAllOf(vararg annotationNames: String): List<String> {
        return scanResult.getNamesOfClassesWithAnnotationsAllOf(classLoader, *annotationNames)
    }

    fun getNamesOfClassesWithAnnotationsAllOf(vararg annotations: Class<*>): List<String> {
        return scanResult.getNamesOfClassesWithAnnotationsAllOf(classLoader, *annotations)
    }

    fun getNamesOfClassesWithAnnotationsAnyOf(vararg annotationNames: String): List<String> {
        return scanResult.getNamesOfClassesWithAnnotationsAnyOf(classLoader, *annotationNames)
    }

    fun getNamesOfClassesWithAnnotationsAnyOf(vararg annotations: Class<*>): List<String> {
        return scanResult.getNamesOfClassesWithAnnotationsAnyOf(classLoader, *annotations)
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
