import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions

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
}

data class ProfessorsUrl(
    val name: String,
    val url: String
)

fun main(args: Array<String>) {
    val driverPath = "/home/max/Downloads/kotlin-wasm-examples/attendance/chromedriver"
    val professorListUrl =
        "https://omgtu.ru/general_information/faculties/faculty_of_information_technology_and_computer_systems/department_of_computer_science_and_engineering/employees_kafed.php"
    with(Selenium(createWebDriver(driverPath))) {
        println(professorListUrl.getProfessorUrls())
    }
}

fun createWebDriver(chromeDriverPath: String): ChromeDriver {
    System.setProperty("webdriver.chrome.driver", chromeDriverPath)
    return ChromeDriver(ChromeOptions().apply {
        addArguments("--remote-allow-origins=*")
    })
}

fun getProfesorsUrls() {}

