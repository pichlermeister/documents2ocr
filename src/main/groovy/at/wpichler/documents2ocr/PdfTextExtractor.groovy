package at.wpichler.documents2ocr

import com.itextpdf.text.pdf.PdfReader
import org.slf4j.LoggerFactory

class PdfTextExtractor {
    def log = LoggerFactory.getLogger(this.getClass())

    String extractTextFromPdf(File file) {

        try {
            PdfReader reader = new PdfReader(file.absolutePath)
            int n = reader.getNumberOfPages()

            def builder = new StringBuilder()
            for (int i = 1; i <= n; i++)
                builder.append(com.itextpdf.text.pdf.parser.PdfTextExtractor.getTextFromPage(reader, i))
            reader.close()
            return builder.toString()
        } catch (Exception e) {
            System.out.println(e)
        }

/*
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
            throw new RuntimeException("pdftotext failed failed with exit code ${p.exitValue()}")
        }
        return builder.toString()
        */
    }

}
