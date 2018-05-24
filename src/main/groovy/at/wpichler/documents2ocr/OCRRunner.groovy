package at.wpichler.documents2ocr

class OCRRunner {

    File inputDir
    File outputDir

    OCRRunner(File inputDir, File outputDir) {
        this.inputDir = inputDir
        this.outputDir = outputDir
    }

    File performOCR(File file) {
        println file.path
        def targetName = file.name.lastIndexOf('.').with { idx -> idx == -1 ? file.name + "-ocr" : file.name[0..<idx] + ".ocr" + file.name[idx..file.name.length() - 1] }
        def targetFile = new File(file.parentFile.absolutePath.replace(inputDir.absolutePath, outputDir.absolutePath), targetName)

        if (!targetFile.exists()) {
            println "OCR: ${targetFile}"

            targetFile.parentFile.mkdirs()

            def cmd = [
                    "ocrmypdf",
                    "--deskew",            //slightly fix rotation of pages
                    "--rotate-pages",      //determine correct page orientation
                    "--remove-background", //detect and remove noisy background from grayscale or color images
                    "--clean",       //cleanup pages before OCR - makes it less likely to find OCR in background noise
                    "-l", "deu+eng",        //use german and english language
                    //"-g", "--pdf-renderer=hocr", //add debug page with OCRed text, needs hocr to render those pages
                    "${file.absolutePath}", "${targetFile.absolutePath}"
            ]
            //    println cmd
            def p = cmd.execute()
            p.waitForProcessOutput(System.out, System.err)
            if (p.exitValue() != 0) {
                throw new OCRException("OCR failed with exit code " + p.exitValue())
            }
        }
        return targetFile
    }
}
