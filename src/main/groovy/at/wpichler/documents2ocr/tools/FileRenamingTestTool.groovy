package at.wpichler.documents2ocr.tools

import at.wpichler.documents2ocr.FilenamingStrategy
import at.wpichler.documents2ocr.PdfTextExtractor
import groovy.io.FileType
import org.springframework.boot.Banner
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationProperties


@ConfigurationProperties("app")
class FileRenamingTestTool implements CommandLineRunner {
    File outputDir


    static void main(String[] args) {
        System.setProperty("spring.main.web-environment", "false")
        SpringApplication app = new SpringApplication(FileRenamingTestTool.class)
        app.setBannerMode(Banner.Mode.OFF)
        app.run(args)
    }

    @Override
    void run(String... args) throws Exception {
        println "Hello World! " + args.join("|")

        outputDir.eachFileRecurse(FileType.FILES, { pdf ->
            if (pdf.name.toLowerCase().endsWith(".pdf")) {
                def text = new PdfTextExtractor().extractTextFromPdf(pdf)
                println "${pdf.name.padRight(150)} -> ${new FilenamingStrategy().suggestName(pdf, text)}"
            }
        })

        System.exit(0)
    }
}
