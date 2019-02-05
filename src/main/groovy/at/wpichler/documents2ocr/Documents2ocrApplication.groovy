package at.wpichler.documents2ocr

import groovy.io.FileType
import lombok.extern.slf4j.Slf4j
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled

import java.nio.file.Files

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties
@ConfigurationProperties("app")
@groovy.util.logging.Slf4j
class Documents2ocrApplication {

    File inputDir
    File archiveDir
    File outputDir
    File errorDir

    @Scheduled(fixedDelay = 10_000l)
    void runOCR() {
        println "scanning for new files"
        inputDir.eachFileRecurse(FileType.FILES, {

            println "detected file ${it.absolutePath}"

            if (it.name.toLowerCase().endsWith(".pdf")) {
                try {
                    def archiveFile = new File(it.absolutePath.replace(inputDir.absolutePath, archiveDir.absolutePath))
                    archiveFile.parentFile.mkdirs()
                    if (archiveFile.exists()) {
                        println "archive file already exists. skipping ${it.absolutePath}"
                    } else {
                        Files.copy(it.toPath(), archiveFile.toPath())
                    }

                    def ocrFile = new OCRRunner(inputDir, outputDir).performOCR(it)
                    println "ocrFile = ${ocrFile}"
                    def text = new PdfTextExtractor().extractTextFromPdf(ocrFile)
                    println "text =${text}"
                    def suggestedFileName = new FilenamingStrategy().suggestName(ocrFile, text)
                    println "suggestedFilename=${suggestedFileName}"

                    def targetDir = new File(it.parentFile.absolutePath.replace(inputDir.absolutePath, outputDir.absolutePath))
                    if (new File(targetDir, suggestedFileName).exists()) {
                        suggestedFileName = suggestedFileName + "_nonunique_" + it.name
                    }
                    def targetFile = new File(targetDir, suggestedFileName)
                    targetFile.parentFile.mkdirs()
                    println "renaming to: ${targetFile.absolutePath}"
                    ocrFile.renameTo(targetFile)
                    if (!it.delete()) {
                        println "cannot delete ${it.absolutePath}"
                    }
                } catch (Exception e) {
                    log.error("couldn't process ${it.absolutePath}", e)
                    def errorFile = new File(it.absolutePath.replace(inputDir.absolutePath, errorDir.absolutePath))
                    errorFile.parentFile.mkdirs()
                    Files.move(it.toPath(), errorFile.toPath())

                }
            } else {
                println "ignore as it does not seem to be a PDF: ${it.name}"
            }
        }

        )
        println "finished!"
    }


    static void main(String[] args) {
        SpringApplication.run Documents2ocrApplication, args
    }

}
