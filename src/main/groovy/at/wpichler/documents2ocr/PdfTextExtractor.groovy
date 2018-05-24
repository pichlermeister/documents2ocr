package at.wpichler.documents2ocr

import org.slf4j.LoggerFactory

class PdfTextExtractor {
    def log = LoggerFactory.getLogger(this.getClass())

    String extractTextFromPdf(File file) {
        def cmd = [
                "pdftotext",
                "-raw", //preserve text direction
                "${file.absolutePath}",
                "-" //print output to stdout
        ]
        log.debug cmd.join(" ")
        def p = cmd.execute()
        def builder = new StringBuilder()
        p.waitForProcessOutput(builder, System.err)
        if (p.exitValue() != 0) {
            throw new RuntimeException("pdftotext failed failed with exit code " + p.exitValue())
        }
        return builder.toString()
    }

}
