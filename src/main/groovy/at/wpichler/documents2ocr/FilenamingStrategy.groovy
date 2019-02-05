package at.wpichler.documents2ocr

import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource

import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZonedDateTime

class FilenamingStrategy {

    def log = LoggerFactory.getLogger(this.getClass())


    def keywords = new ClassPathResource("keywords").getFile()
            .listFiles()
            .findAll { it.name.endsWith(".txt") }
            .collectMany { it.readLines() }
            .findAll { it.length() > 1 } //ignore empty lines
            .collect { it.toLowerCase() }
            .toUnique()

    //TODO: include synonyms
    def synonyms = [
            "Max Mustermann" : "max"
            , "Kraftfahrzeug": "KFZ"
    ]

    String suggestName(File file, String text) {
        def words = findRelevantWords(text)
        def dates = findDates(text)
        log.debug "words=${words}"
        log.debug "dates=${dates.collect { new SimpleDateFormat("yyyy-MM-dd").format(it) }}"

        // certain documents may have many dates in them (e.g. insurance policy).
        // try to restrict them to date where document was written / policy starts being effective
        // idea: order dates desc: because we want it ordered by date when something got affective (chronologically by document), but some documents may refer to events that happened in the very past
        def filenameDates = dates.findAll {
            it.toInstant().isAfter(ZonedDateTime.now().minusYears(7).toInstant()) && it.toInstant().isBefore(ZonedDateTime.now().plusMonths(2).toInstant())
        }
        if (filenameDates.isEmpty()) {
            filenameDates = dates
        }
        def name = filenameDates.sort().reverse().toUnique().take(3).collect {
            new SimpleDateFormat("yyyy-MM-dd").format(it)
        }.join("__") ?:
                text.findAll(/[0-9]{4}/).toUnique().collect { it.toInteger() }.sort().reverse().findAll {
                    it >= LocalDate.now().year - 7 && it <= LocalDate.now().year
                }.take(3).join("__") ?:
                        "NODATE"
        //name += "." +  keywords.collectMany { words.toList().intersect(it) }.toUnique().take(10).join(" ")
        name += " - " + words.findAll { keywords.contains(it) }.toUnique().take(10).join(" ").take(75)
        name += file.name.substring(file.name.lastIndexOf("."))

        return name
    }

    String[] findRelevantWords(String text) {
        //println "text=${text}\n\n"
        def stopwords = new ClassPathResource("stopwords.txt").getFile().collect()
        def words = text.toLowerCase().tokenize().collect().toUnique().findAll {
            it.matches(/[\wÄÖÜäöü\- ]+/) && !stopwords.contains(it)
        }.collectMany { //enrich words with stemming (transform to its base-form)
            [it, new org.apache.lucene.analysis.PorterStemmer().stem(it)]
        }.toUnique()
        return words
    }

    private static Date[] findDates(String text) {
        def dates = text.findAll(/\d{2}\.\d{2}\.\d{4}/).toSet().collect {
            new SimpleDateFormat("dd.MM.yyyy").parse(it)
        }
        def dates2 = text.findAll(/\d{2}\. (Januar|Jänner|Februar|März|April|Mai|Juni|Juli|August|September|Oktober|November|Dezember) \d{4}/).toSet().collect {
            new SimpleDateFormat("dd. MMM yyyy", Locale.GERMAN).parse(it.replace("Jänner", "Januar"))
        }
        if (dates2)
            dates.addAll(dates2)
        return dates
    }
}
