package at.wpichler.documents2ocr

import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource

import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZonedDateTime

class FilenamingStrategy {

    def log = LoggerFactory.getLogger(this.getClass())

    def keywords = [
            //FIRMEN
            ["Hellobank", "Hello bank", "Finanzadmin", "HYPO", "FiNUM", "dieFinanzplaner", "Sparkasse", "PayLife", "Mastercard", "Allianz", "Continentale", "EUROPA", "muki", "Roland"],
            ["Valida"],
            ["OOEGKK", "EDER", "Mazda"],
            ["Finanzamt"],
            ["NOTAR", "Neundlinger"],

            //AUTO
            ["JAHRESSERVICE", "UU 867DG"],

            //DOKUMENTEN TYPE
            ["RECHNUNG", "Prämienrechnung", "Antrag", "Polizze", "Versicherungsschein", "Nachtrag", "Finanzamtsbescheinigung", "KESt-Finanzamtsbescheinigung", "KESt-Verlustausgleich", "Bescheinigung", "Nutzungsvereinbarungen", "Infoblatt", "Jahresmitteilung", "Wertanpassung", "Mitteilung", "Information", "Beratungsprotokoll"],
            ["Arbeitsvertrag", "Arbeitnehmer/innen-Veranlagung", "Arbeitnehmerveranlagung"],
            ["Testament", "Beschluss"],

            //OTHER KEYWORDS
            ["Konto", "ecx.io"],
            ["Indexanpassung", "Bonus-Malus—System", "Folgeprämie", "Verbraucherpreisindex", "Baukosten-Index"],
            ["Kontoinformation", "Mitarbeitervorsorge", "Abfertigung"],
            ["Steuern", "Jährlicher", "Jährliche", "Überschussbeteiligung", "Versicherungssteuer"],
            ["Bankverbindung"],
            ["Betreuung"],
            ["Wertpapier", "KESt"],
            ["Beratung", "Empfohlen", "Empfehlung"],
            ["Wertpapieraufstellung", "Aufstellung", "Auszug"],
            ["Passwort", "Fondsbestimmungen", "Änderung", "Änderungen"],
            ["Kundeninformation", "Wichtig", "Rechtsform", "Einlagensicherung"],
            ["Kontoauszug", "Abrechnung", "Saldo"],
            ["Verständigung", "Wechsel", "Konto", "Kartenauftrag", "Privatkonto", "Komfortkonto", "Kontovertrag", "Allgemeine Bedingungen", "Girokonto", "Electronic Banking", "Elba", "Antragsteller"],
            ["Bausparvertrag", "Bausparer"],
            ["Verbraucherpreisindex", "Anpassung", "Kündigung"],
            ["Transaktionsliste", "Depot", "Verkauf", "Kauf", "Wertpapiergeschäft", "Kapitalentnahme", "Haus", "Vermögensübersicht", "Sparkonto"],
            ["George", "netbanking", "Finanzmanager"],
            ["Preiserhöhung", "Kreditkarte", "s Kreditkarte", "Erhöhung", "erhöhen"],
            ["s Fonds Plan Vertrag", "Wertpapierdepot", "Konditionen", "Inflationsrate"],
            ["Depotbestand", "Kundengespräch", "Transaktion"],
            ["Verlassenschaft", "VERLASSENSCHAFTSSACHE", "RECHTSMITTELBELEHRUNG"],
            ["Änderungsmeldung", "Aenderungsmeldung", "Aenderung", "Bestaetigung", "Dienstgeber"],
            ["Anwartschaft", "Selbstständigenvorsorge", "Vorsorgekasse"],
            ["Beiträge"],
            ["VERTRAGSINFORMATIONEN"],
            ["Zusammenführung", "Abfertigungskonten"],
            ["RECHNUNG", "BEGUTACHTUNGSPLAKETTE", "JAHRESSERVICE", "Rückholaktion"],
            ["Steuererklärung", "Steuerinformation", "Gutschrift", "Steuergutschrift"],
            ["Rückholaktion", "Austausch", "vorläufig"],
            ["KFZ"],

            //INSURANCE
            ["Unfallversicherung", "BU", "FLV", "BU-Versicherung", "Berufsunfähigkeits-Versicherung", "Risikolebensversicherung", "Versicherungsvertrag", "Haushaltsversicherung", "KFZ-Versicherung", "Privat-Rechtsschutz"],
            // WORK
            ["Gleitzeit", "Vereinbarung", "Überstündenpauschale"]
            //].collectMany { list -> [list.collect { it.toLowerCase() }] }
    ].collectMany { list -> list.collect { it.toLowerCase() } }.toUnique()

    //TODO: include phrases
    def phrases = ["Julia Tutschek": "julia", "s Fonds Plan": "sFondsPlan"]

    def synonyms = ["Berufsunfähigkeits-Versicherung": "BU", "BU-Versicherung": "BU"]

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
