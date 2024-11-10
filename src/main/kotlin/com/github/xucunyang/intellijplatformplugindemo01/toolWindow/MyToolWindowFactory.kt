package com.github.xucunyang.intellijplatformplugindemo01.toolWindow

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.content.ContentFactory
import com.github.xucunyang.intellijplatformplugindemo01.MyBundle
import com.github.xucunyang.intellijplatformplugindemo01.services.MyProjectService
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.javadoc.PsiDocComment
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtFile
import javax.swing.JButton


class MyToolWindowFactory : ToolWindowFactory {

    init {
        thisLogger().warn("Don't forget to remove all non-needed sample code files with their corresponding registration entries in `plugin.xml`.")
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val myToolWindow = MyToolWindow(toolWindow)
        val content = ContentFactory.getInstance().createContent(myToolWindow.getContent(), null, false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true

    class MyToolWindow(val toolWindow: ToolWindow) {

        private val service = toolWindow.project.service<MyProjectService>()

        fun getContent() = JBPanel<JBPanel<*>>().apply {
            val label = JBLabel(MyBundle.message("randomLabel", "?"))

            add(label)
            add(JButton(MyBundle.message("shuffle")).apply {
                addActionListener {
                    label.text = MyBundle.message("randomLabel", service.getRandomNumber())

                    val annotationName = "Geet" // 替换为你的注解名称
                    val annotatedMethods = processFilesWithAnnotation(annotationName, toolWindow.project)
                    println("annotatedMethods $annotatedMethods")
                    annotatedMethods.forEach { method ->
                        println("Found annotated method: ${method.name} --> ${getDocComment(method)}  method.docComment：${method.docComment?.text}")
                    }

                }
            })
        }


        fun processFilesWithAnnotation(annotationName: String, project: Project): List<KtFunction> {
            val project = project ?: return emptyList()
            val ktFiles = FileTypeIndex.getFiles(KotlinFileType.INSTANCE, GlobalSearchScope.allScope(project))
                .map { PsiManager.getInstance(project).findFile(it) as? KtFile }
                .filterNotNull()


            return ktFiles.flatMap { file ->
                PsiTreeUtil.findChildrenOfType(file, KtFunction::class.java)
                    .filter { function ->
                        (function as? KtAnnotated)?.annotationEntries?.any { it.text.contains(annotationName) } == true
                    }
            }
        }


        private fun extractMainContent(docComment: KDoc): String {
            val mainContent = StringBuilder()

            // 获取 KDoc 的所有段落
            val sections = docComment.getAllSections()
            for (section in sections) {
                val content = section.getContent().trim()
                if (content.isNotEmpty()) {
                    mainContent.append(content).append("\n")
                }
            }

            return mainContent.toString().trim()
        }

        fun processFilesWithAnnotation(annotationName: String, e: AnActionEvent): List<KtFunction> {
            val project = e.project ?: return emptyList()
            val ktFiles = FileTypeIndex.getFiles(KotlinFileType.INSTANCE, GlobalSearchScope.allScope(project))
                .map {

                    val ktFile = PsiManager.getInstance(project).findFile(it) as? KtFile
                    println("psi is kt ${ktFile is KtFile} ann:${ktFile is PsiAnnotation}  name: ${ktFile?.name}")
                    if (
                        ktFile is KtFile
                        && ktFile.name == "Geet.kt"
                        && isAnnotationClassFile(ktFile)
                    ) {
                        println("psi it ${it} Geet 注释：${getDocComment(ktFile)}")
                    }
                    ktFile
                }
                .filterNotNull()

            return ktFiles.flatMap { file ->
                PsiTreeUtil.findChildrenOfType(file, KtFunction::class.java)
                    .filter { function ->
                        (function as? KtAnnotated)?.annotationEntries?.any { it.text.contains(annotationName) } == true
                    }
            }
        }

        private fun isAnnotationClassFile(file: KtFile): Boolean {
            for (declaration in file.declarations) {
                if (declaration is KtClass && isAnnotationClass(declaration)) {
                    return true
                }
            }
            return false
        }


        private fun isAnnotationClass(ktClass: KtClass): Boolean {
            val lightClass = ktClass.toLightClass() ?: return false
            return lightClass.isAnnotationType
        }

        private fun getDocComment(psiElement: PsiElement): String? {
            val docComment = PsiTreeUtil.getChildOfType(psiElement, PsiDocComment::class.java)
            if (docComment != null) {
                println("Found doc comment: ${docComment.text}")
                return docComment.text
            } else {
                println("No doc comment found for element: ${psiElement.text}")
                return null
            }
        }
    }
}
