package io.github.notsyncing.manifold.docgen

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.serializer.SerializerFeature
import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner
import io.github.notsyncing.manifold.Manifold
import io.github.notsyncing.manifold.action.ManifoldScene
import io.github.notsyncing.manifold.action.metadata.SceneGroup
import io.github.notsyncing.manifold.docgen.models.SceneInfo
import io.github.notsyncing.manifold.docgen.models.StatefulSceneInfo
import io.github.notsyncing.manifold.story.stateful.StatefulScene
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.lang.reflect.Modifier
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

object ManifoldDocumentGenerator {
    fun checkAndGenerate() {
        if (System.getProperty("manifold.docgen") != "true") {
            return
        }

        println("Generating document...")

        val scenes = Manifold.dependencyProvider!!.getAllSubclasses(ManifoldScene::class.java)

        val data = scenes.groupBy { it.getAnnotation(SceneGroup::class.java)?.value ?: "未定义分组" }
                .mapValues {
                    it.value.map {
                        if ((it == StatefulScene::class.java) || (it == ManifoldScene::class.java) || (Modifier.isAbstract(it.modifiers))) {
                            return@map null
                        }

                        if (StatefulScene::class.java.isAssignableFrom(it)) {
                            StatefulSceneInfo(it as Class<StatefulScene<*>>)
                        } else if (ManifoldScene::class.java.isAssignableFrom(it)) {
                            SceneInfo(it)
                        } else {
                            SceneInfo("未知场景", it.name, "", emptyList())
                        }
                    }.filterNotNull()
                }

        val docPath = Paths.get("./doc")

        if (!Files.exists(docPath)) {
            Files.createDirectories(docPath)
        }

        val dataScript = "function sceneData() { return " + JSON.toJSONString(data, SerializerFeature.PrettyFormat) + "; }"

        Files.write(docPath.resolve("data.js"), dataScript.toByteArray())

        println("Copying resources...")

        copyResources("/manifold-docgen", Paths.get("./doc"))

        println("Document generated.")

        System.exit(0)
    }

    private fun copyResources(dir: String, to: Path) {
        if (!Files.exists(to)) {
            Files.createDirectories(to)
        }

        FastClasspathScanner()
                .matchFilenamePattern("^${dir.removePrefix("/")}/(.*?)$") { relativePath: String?, inputStream: InputStream?, lengthBytes: Long ->
                    if (relativePath != null) {
                        exportResource(dir.removePrefix("/"), relativePath, to)
                    }
                }
                .scan()
    }

    // from https://stackoverflow.com/a/19776960/6643564
    private fun exportResource(parentPath: String, resourceName: String, to: Path): String {
        var stream: InputStream? = null
        var resStreamOut: OutputStream? = null
        val jarFolder: String
        try {
            stream = javaClass.getResourceAsStream("/$resourceName")//note that each / is a directory down in the "jar tree" been the jar the root of the tree
            if (stream == null) {
                throw Exception("Cannot get resource \"$resourceName\" from Jar file.")
            }

            var readBytes: Int
            val buffer = ByteArray(4096)
            jarFolder = File(javaClass.protectionDomain.codeSource.location.toURI().path).parentFile.path.replace('\\', '/')

            val outPath = to.resolve(resourceName.removePrefix("$parentPath/"))

            if (!Files.exists(outPath.parent)) {
                Files.createDirectories(outPath.parent)
            }

            resStreamOut = Files.newOutputStream(outPath)

            do {
                readBytes = stream.read(buffer)

                if (readBytes > 0) {
                    resStreamOut.write(buffer, 0, readBytes)
                }
            } while (readBytes > 0)
        } catch (ex: Exception) {
            throw ex
        } finally {
            if (stream != null) {
                stream.close()
            }

            if (resStreamOut != null) {
                resStreamOut.close()
            }
        }

        return jarFolder + resourceName
    }
}