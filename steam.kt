import javafx.application.Application
import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.stage.FileChooser
import javafx.stage.Stage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileWriter
import java.io.IOException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

class SteamNewsFetcher : Application() {
    private val client = OkHttpClient()

    override fun start(primaryStage: Stage) {
        primaryStage.title = "Steam News Fetcher"

        val appIdLabel = Label("Enter App ID:")
        val appIdField = TextField()

        val newsArea = TextArea().apply {
            isWrapText = true
        }

        val getNewsButton = Button("Get News").apply {
            setOnAction {
                val appId = appIdField.text
                if (appId.isNotBlank() && appId.all { it.isDigit() }) {
                    fetchNews(appId, newsArea)
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", "App ID must be a number.")
                }
            }
        }

        val saveButton = Button("Save JSON").apply {
            setOnAction {
                val news = newsArea.text
                if (news.isNotBlank()) {
                    saveNewsToFile(news, primaryStage)
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", "No news to save.")
                }
            }
        }

        val openJsonLinkButton = Button("Open API Link").apply {
            setOnAction {
                val appId = appIdField.text
                if (appId.isNotBlank() && appId.all { it.isDigit() }) {
                    openLink("https://api.steampowered.com/ISteamNews/GetNewsForApp/v2/?appid=$appId")
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", "App ID must be a number.")
                }
            }
        }

        val openSteamLinkButton = Button("Open Steam Link").apply {
            setOnAction {
                val appId = appIdField.text
                if (appId.isNotBlank() && appId.all { it.isDigit() }) {
                    openLink("https://store.steampowered.com/news/app/$appId")
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", "App ID must be a number.")
                }
            }
        }

        val inputRow = HBox(10.0, appIdLabel, appIdField, getNewsButton)
        val buttonRow = HBox(10.0, saveButton, openJsonLinkButton, openSteamLinkButton)

        val layout = VBox(10.0, inputRow, buttonRow, newsArea).apply {
            padding = Insets(10.0)
        }

        primaryStage.scene = Scene(layout, 600.0, 400.0)
        primaryStage.show()
    }

    private fun fetchNews(appId: String, newsArea: TextArea) {
        runBlocking {
            try {
                val url = "https://api.steampowered.com/ISteamNews/GetNewsForApp/v2/?appid=$appId"
                val response = withContext(Dispatchers.IO) {
                    val request = Request.Builder().url(url).build()
                    client.newCall(request).execute()
                }

                if (response.isSuccessful) {
                    val body = response.body?.string()
                    val mapper = ObjectMapper()
                    val newsData: Map<String, Any> = mapper.readValue(body ?: "{}")
                    val newsItems = (newsData["appnews"] as? Map<*, *>)?.get("newsitems") as? List<Map<String, Any>>

                    newsArea.clear()
                    if (!newsItems.isNullOrEmpty()) {
                        newsItems.forEach { item ->
                            val title = item["title"] ?: "No Title Available"
                            val contents = item["contents"] ?: "No Content Available"
                            val link = item["url"] ?: ""

                            newsArea.appendText("$title\n")
                            newsArea.appendText("${contents.toString().take(200)}...\n")
                            if (link.isNotEmpty()) {
                                newsArea.appendText("Link: $link\n")
                            }
                            newsArea.appendText("\n")
                        }
                    } else {
                        newsArea.appendText("No news found for this app.")
                    }
                } else {
                    showAlert(Alert.AlertType.ERROR, "HTTP Error", "Failed to fetch news. Code: ${response.code}")
                }
            } catch (e: IOException) {
                showAlert(Alert.AlertType.ERROR, "Connection Error", "Unable to connect to the server.")
            } catch (e: Exception) {
                showAlert(Alert.AlertType.ERROR, "Error", "An unexpected error occurred: ${e.message}")
            }
        }
    }

    private fun saveNewsToFile(news: String, stage: Stage) {
        val fileChooser = FileChooser().apply {
            title = "Save JSON"
            extensionFilters.add(FileChooser.ExtensionFilter("JSON Files", "*.json"))
        }
        val file = fileChooser.showSaveDialog(stage)
        file?.let {
            try {
                FileWriter(it).use { writer ->
                    writer.write(news)
                }
                showAlert(Alert.AlertType.INFORMATION, "Save Complete", "File saved successfully to: ${it.absolutePath}")
            } catch (e: IOException) {
                showAlert(Alert.AlertType.ERROR, "File Error", "Error writing the file: ${e.message}")
            }
        }
    }

    private fun openLink(url: String) {
        try {
            java.awt.Desktop.getDesktop().browse(java.net.URI(url))
        } catch (e: Exception) {
            showAlert(Alert.AlertType.ERROR, "Error", "Unable to open link: ${e.message}")
        }
    }

    private fun showAlert(type: Alert.AlertType, title: String, content: String) {
        Alert(type).apply {
            this.title = title
            headerText = null
            contentText = content
            showAndWait()
        }
    }
}

fun main() {
    Application.launch(SteamNewsFetcher::class.java)
}
