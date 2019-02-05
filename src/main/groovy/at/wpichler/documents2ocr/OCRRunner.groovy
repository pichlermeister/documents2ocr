package at.wpichler.documents2ocr

import com.itextpdf.text.pdf.PdfReader

import java.nio.file.Files

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

            def cmd = []
            def inputFile = file.absolutePath
            def outputFile = targetFile.absolutePath
            if (System.properties['os.name'].toLowerCase().contains('windows')) {
                println "it's Windows - using docker for OCRmyPDF"
                cmd = [
                        "docker",
                        "run",
                        "--rm", //it's a 1-time job, so we remove the container afterwards

                        /*
                        on Windows 10 with docker for windows, there only 2GB available

                        docker@573eae029f8e:~$ df -h
                        Filesystem      Size  Used Avail Use% Mounted on
                        overlay         2.0G  1.7G  227M  89% /

                        when OCR-ing PDFs with some optimization options specified (deskew, rotate-pages, etc.) that will use up some more disk space
                        and an exception is thrown "OSError: [Errno 28] No space left on device"

                        to come around this, we mount "/tmp" to a memory-backed filesytsem
                         */
                        "--mount", "type=tmpfs,destination=/tmp",

                        "-v", "${'/' + inputDir.absolutePath.replace(':', '').replace('\\', '/')}:/home/docker/input",
                        "-v", "${'/' + outputDir.absolutePath.replace(':', '').replace('\\', '/')}:/home/docker/output",
                        "jbarlow83/ocrmypdf"
                ]
                inputFile = file.absolutePath.replace(inputDir.absolutePath, "/home/docker/input").replace('\\', '/')
                outputFile = targetFile.absolutePath.replace(outputDir.absolutePath, "/home/docker/output").replace('\\', '/')

            } else {
                println "it's NOT Windows" //assuming ocrmypdf was installed manually on Linux
                cmd = ["ocrmypdf"]
            }

            //get existing metadata. they will be added as keywords later, so we don't loose them
            PdfReader reader = new PdfReader(file.getAbsolutePath());
            println "METADATA of ${file.getAbsolutePath()}: ${reader.getInfo()}"
            def metadata = reader.getInfo().collect { k, v -> "${k}='${v}'" }.join(" ")
            reader.close()

            // for possible arguments see: https://ocrmypdf.readthedocs.io/en/latest/cookbook.html#image-processing
            // and via ocrmypdf help: docker run --rm jbarlow83/ocrmypdf -h
            cmd.addAll([
                    "--deskew",            //slightly fix rotation of pages
                    "--rotate-pages",      //determine correct page orientation
                    "--remove-background", //detect and remove noisy background from grayscale or color images
                    "--clean",             //cleanup pages before OCR - makes it less likely to find OCR in background noise
                    "-l", "deu+eng",       //use german and english language
                    "--mask-barcodes",     //will “cover up” any barcodes detected in the image of a page. Barcodes are known to confuse Tesseract OCR and interfere with the recognition of text on the same baseline as a barcode. The output file will contain the unaltered image of the barcod
                    "--sidecar", "${outputFile}.txt", //saves recognized text in own file
                    //"-g", "--pdf-renderer=hocr", //add debug page with OCRed text, needs hocr to render those pages
                    "-v", "1",              //set verbosity level


                    "--title", "${file.name}",
                    "--author", "Digitalized by ${System.getProperty("user.name")}",
                    "--subject", "${new java.util.zip.CRC32().with { update file.bytes; value }}", //set CRC32 checksum of original file, so we can match them later
                    "--keywords", "${file.name} ${metadata}",
                    "${inputFile}", "${outputFile}"
            ])

            def logFile = new File(targetFile.absolutePath + ".log")
            //logFile.createNewFile()
            def outBuffer = new FileOutputStream(logFile) //System.out

            def errLogFile = new File(targetFile.absolutePath + ".err.log")
            //logFile.createNewFile()
            def errBuffer = new FileOutputStream(errLogFile) //System.err

            println "running OCR: ${cmd.join(' ')}"
            def p = cmd.execute()
            p.waitForProcessOutput(outBuffer, errBuffer)

            outBuffer.close()
            errBuffer.close()

            //write logfile
//            if (outBuffer.toString().length() > 0) {
//                println "writing log file"
//                new File(outputFile + ".log").text = outBuffer.toString()
//            }
//            if (errBuffer.toString().length() > 0) {
//                println "writing error log file"
//                new File(outputFile + ".err.log").text = errBuffer.toString()
//            }

            if (p.exitValue() != 0) {
                throw new OCRException("OCR failed with exit code ${p.exitValue()}", file)
            }
        }

        return targetFile
    }

}
