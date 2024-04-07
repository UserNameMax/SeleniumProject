import org.openqa.selenium.By
import org.openqa.selenium.NoSuchElementException
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import java.io.FileOutputStream
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.Path

class Selenium(private val driver: WebDriver) {
    fun String.getProfessorUrls(): List<ProfessorsUrl> {
        driver.get(this)
        return driver
            .findElement(By.id("pagecontent"))
            .findElements(By.tagName("div"))
            .filter { it.findElements(By.tagName("p")).isNotEmpty() }
            .groupBy { it.findElement(By.tagName("p")).text }
            .map {
                ProfessorsUrl(
                    name = it.key,
                    url = it.value
                        .filter { it.findElements(By.tagName("a")).isNotEmpty() }
                        .run { firstOrNull()?.findElement(By.tagName("a")) }
                        ?.getAttribute("href") ?: ""
                )
            }
    }

    private fun getRincInc(str: String = ""): Int {
        return if (driver.findElement(By.tagName("body")).text.contains(str)) 1 else 0
    }

    private fun getPublicationFromElibrary(url: String): Pair<Int, Int> {
        driver.get(url)
        return withRepeat {
            Pair(
                getRincInc("Входит в РИНЦ: да"),
                getRincInc("Входит в ядро РИНЦ: да")
            )
        }
    }

    private fun getInfoFromOmgtu(): ProfessorsInfo {
        val nameCssSelector =
            "#pagecontent > div.table-responsive"

        val info = ProfessorsInfo(
            name = driver.findElement(By.cssSelector(nameCssSelector)).findElement(By.tagName("div")).text,
            total = 0,
            rinc = 0,
            rincCore = 0
        )
        val divs = driver.findElements(By.tagName("div"))
        return driver.findElements(By.tagName("div"))
            .indexOfFirst { it.text == "Научные публикации и достижения" }
            .let { index -> if (index < 0) null else index }
            ?.run {
                let { index -> divs[index + 2] }
                    .findElements(By.tagName("tr"))
                    .map { it.findElements(By.tagName("a")).firstOrNull()?.getAttribute("href") ?: "" }
                    .fold(info) { curInfo, url ->
                        val (rync, core) = if (url.contains("elibrary")) {
                            getPublicationFromElibrary(url)
                        }
                        else {
                            Pair(0, 0)
                        }
                        curInfo.copy(
                            total = curInfo.total + 1,
                            rinc = curInfo.rinc + rync,
                            rincCore = curInfo.rincCore + core
                        )
                    }
            } ?: info

    }

    fun ProfessorsUrl.getProfessor(): ProfessorsInfo {
        driver.get(url)
        val elabrary = driver.findElements(By.tagName("div")).firstOrNull { it.text.startsWith("SPIN") }
            ?.run { findElement(By.tagName("a")).getAttribute("href") }
        return if (elabrary != null) {
            getInfoFromElabraray(elabrary)
        } else {
            getInfoFromOmgtu()
        }
    }

    private fun getInfoFromElabraray(url: String): ProfessorsInfo {
        fun WebElement.value(index: Int) =
            findElements(By.tagName("tr"))[index].findElements(By.tagName("td"))[2].text.toInt()
        driver.get(url)
        val name =
            driver.findElement(By.cssSelector("#thepage > table:nth-child(1) > tbody:nth-child(1) > tr:nth-child(1) > td:nth-child(1) > table:nth-child(1) > tbody:nth-child(1) > tr:nth-child(1) > td:nth-child(2) > form:nth-child(1) > table:nth-child(2) > tbody:nth-child(1) > tr:nth-child(3) > td:nth-child(1) > table:nth-child(1) > tbody:nth-child(1) > tr:nth-child(1) > td:nth-child(1) > div:nth-child(1) > font:nth-child(1) > b:nth-child(1)"))
                .text
        val table =
            driver.findElements(By.tagName("table")).first { it.text.trim().startsWith("Название показателя Значение") }
        val total = table.value(3)
        val rinc = table.value(4)
        val rincCore = table.value(5)
        return ProfessorsInfo(name = name, total = total, rinc = rinc, rincCore = rincCore)
    }
}

data class ProfessorsUrl(
    val name: String,
    val url: String
)

data class ProfessorsInfo(
    val name: String,
    val total: Int,
    val rinc: Int,
    val rincCore: Int
)

fun main(args: Array<String>) {
    val driverPath = "/home/max/Downloads/kotlin-wasm-examples/attendance/chromedriver"
    val professorListUrl =
        "https://omgtu.ru/general_information/faculties/faculty_of_information_technology_and_computer_systems/department_of_computer_science_and_engineering/employees_kafed.php"
    val webDriver = createWebDriver(driverPath)
    val selenium = Selenium(webDriver)
    with(selenium) {
        professorListUrl.getProfessorUrls().map {
            withRepeat { it.getProfessor() }
        }
    }.toCsv().saveCsv()
}

fun <T> withRepeat(lambda: () -> T): T =
    try {
        lambda()
    } catch (e: NoSuchElementException) {
        println(e.message)
        print("Continue?")
        readln()
        withRepeat(lambda)
    }


fun createWebDriver(chromeDriverPath: String): ChromeDriver {
    System.setProperty("webdriver.chrome.driver", chromeDriverPath)
    return ChromeDriver(ChromeOptions().apply {
        addArguments("--remote-allow-origins=*")
    })
}

fun List<ProfessorsInfo>.toCsv(): String {
    val list = this
    return buildString {
        appendLine("Имя,Общее кол-во публикаций,Публикаций в РИНЦ,Публикаций в ядре РИНЦ")
        list.forEach { professor -> with(professor) { appendLine("$name,$total,$rinc,$rincCore") } }
    }
}

fun String.saveCsv(path: Path = Paths.get("result.csv")) {
    path.toFile().outputStream().use { it.write(toByteArray()) }
}