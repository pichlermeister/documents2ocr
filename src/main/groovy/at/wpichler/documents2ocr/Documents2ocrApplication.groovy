package at.wpichler.documents2ocr

import groovy.io.FileType
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled

import java.nio.file.Files

@SpringBootApplication
@EnableScheduling
@ConfigurationProperties()
class Documents2ocrApplication {

    File inputDir
    File archiveDir
    File outputDir

    @Scheduled(fixedDelay = 10_000l)
    void runOCR() {
        println "scanning for new files"
        inputDir.eachFileRecurse(FileType.FILES, {
            if (it.name.toLowerCase().endsWith(".pdf")) {
                def archiveFile = new File(it.absolutePath.replace(inputDir.absolutePath, archiveDir.absolutePath))
                archiveFile.parentFile.mkdirs()
                if (archiveFile.exists()) {
                    println "archive file already exists. skipping ${it.absolutePath}"
                } else {
                    Files.copy(it.toPath(), archiveFile.toPath())
                }

                def ocrFile = new OCRRunner(inputDir, outputDir).performOCR(it)
                def text = new PdfTextExtractor().extractTextFromPdf(ocrFile)
                def suggestedFileName = new FilenamingStrategy().suggestName(ocrFile, text)

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
            } else {
                println "ignore as it does not seem to be a PDF: ${it.name}"
            }
        })
    }


    static void main(String[] args) {
        SpringApplication.run Documents2ocrApplication, args
    }
}
