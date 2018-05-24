package at.wpichler.documents2ocr

import org.junit.Test
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.core.io.ClassPathResource

import java.text.SimpleDateFormat

@ConfigurationProperties
class FilenamingStrategyTest {

    File inputDir
    File archiveDir
    File outputDir

    @Test
    void test1() {

        //def text = new ClassPathResource("test1.txt").getFile().text
        def dir = new ClassPathResource("filenamestrategytestfiles").getFile()
        println dir
        dir.eachFile { file ->
            println "\n" + file.name
            if (file.name.toLowerCase().endsWith(".pdf.txt"))
                println "suggested name=" + new FilenamingStrategy().suggestName(file, file.text)
        }

    }

    @Test
    void testList() {
        def integers = [1, 2, 3]
        println "${integers.take(1)}"
        println "${integers.take(2)}"
        println "${integers.take(9)}"
        println integers
    }

    @Test
    void testSimpleDateFormat() {
        println new SimpleDateFormat("dd. MMM yyyy", Locale.GERMAN).parse("27. März 2018").format("yyyy-MM-ddd")
    }

    @Test
    void testSimpleDateFormat2() {
        def sdf = new SimpleDateFormat("dd. MMM yyyy", Locale.GERMANY)
        sdf.parse("26. Jänner 2018".replace("Jänner", "Januar")).format("yyyy-MM-dd")
        sdf.parse("26. Februar 2018").format("yyyy-MM-dd")
        sdf.parse("26. März 2018").format("yyyy-MM-dd")
        sdf.parse("26. April 2018").format("yyyy-MM-dd")
        sdf.parse("26. Mai 2018").format("yyyy-MM-dd")
        sdf.parse("26. Juni 2018").format("yyyy-MM-dd")
        sdf.parse("26. Juli 2018").format("yyyy-MM-dd")
        sdf.parse("26. August 2018").format("yyyy-MM-dd")
        sdf.parse("26. September 2018").format("yyyy-MM-dd")
        sdf.parse("26. Oktober 2018").format("yyyy-MM-dd")
        sdf.parse("26. November 2018").format("yyyy-MM-dd")
        sdf.parse("26. Dezember 2018").format("yyyy-MM-dd")

    }

}
