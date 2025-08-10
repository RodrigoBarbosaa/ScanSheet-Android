package com.example.scansheet.request

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CSVHandler {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    /**
     * Função principal que orquestra o processo de conversão e salvamento.
     * @param jsonResponse A string de resposta completa recebida do servidor.
     * @param context O Context da aplicação, necessário para acessar o ContentResolver.
     * @return Um [Result] que contém a Uri do arquivo salvo em caso de sucesso, ou uma exceção em caso de falha.
     */
    suspend fun processAndSaveCsv(jsonResponse: String, context: Context): Result<Uri> {
        return withContext(Dispatchers.IO) { // Garante que tudo rode em background
            try {
                val mergedData = parseAndMergeJson(jsonResponse)
                if (mergedData.isEmpty()) {
                    throw IOException("Não foi possível extrair dados do JSON.")
                }

                val csvContent = createCsvString(mergedData)

                // A função de salvar agora é uma suspend fun
                val fileUri = saveCsvToFile(csvContent, context)
                Log.d("DEBUG", "CSV salvo com sucesso em: $fileUri")
                Result.success(fileUri)
            } catch (e: Exception) {
                Log.e("DEBUG", "Falha ao processar ou salvar CSV", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Parseia a resposta JSON que contém um array de strings JSON e une os campos "content".
     */
    private fun parseAndMergeJson(jsonResponse: String): Map<String, String> {
        // Define o tipo para uma lista de strings
        val listMyData = Types.newParameterizedType(List::class.java, String::class.java)
        val jsonAdapter = moshi.adapter<List<String>>(listMyData)

        // Parseia o array de strings JSON
        val jsonStringList = jsonAdapter.fromJson(jsonResponse) ?: emptyList()

        val mergedMap = mutableMapOf<String, Any?>()

        // Define o tipo para o objeto interno com "title" e "content"
        val innerObjectType = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
        val innerAdapter = moshi.adapter<Map<String, Any>>(innerObjectType)

        // Itera sobre cada string JSON dentro do array
        jsonStringList.forEach { stringJson ->
            val innerObject = innerAdapter.fromJson(stringJson)
            if (innerObject != null && innerObject["content"] is Map<*, *>) {
                // Adiciona todos os dados do objeto "content" ao mapa geral
                @Suppress("UNCHECKED_CAST")
                mergedMap.putAll(innerObject["content"] as Map<String, Any?>)
            }
        }

        // Converte todos os valores para String, tratando nulos como strings vazias
        return mergedMap.mapValues { it.value?.toString() ?: "" }
    }

    /**
     * Cria o conteúdo de texto do CSV (cabeçalho + linha de dados).
     */
    private fun createCsvString(data: Map<String, String>): String {
        val header = data.keys.joinToString(",")
        val values = data.values.joinToString(",")
        return "$header\n$values"
    }

    /**
     * Salva o conteúdo de texto em um arquivo .csv na pasta de Downloads pública.
     */
    private suspend fun saveCsvToFile(csvContent: String, context: Context): Uri {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "scansheet_data_$timeStamp.csv"

        // LÓGICA CONDICIONAL BASEADA NA VERSÃO DO SDK
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Log.d("DEBUG", "Usando saveCSV 10+")
            // Abordagem MODERNA (Android 10+) com MediaStore
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/ScanSheet")
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                ?: throw IOException("Falha ao criar entrada no MediaStore.")
            resolver.openOutputStream(uri)?.use { it.write(csvContent.toByteArray()) }
                ?: throw IOException("Falha ao abrir OutputStream para a Uri.")
            return uri
        } else {
            // Abordagem LEGADA (Android 9 e inferior) com acesso direto ao arquivo
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val appDir = File(downloadsDir, "ScanSheet")
            if (!appDir.exists()) {
                appDir.mkdirs()
            }
            val file = File(appDir, fileName)
            file.writeText(csvContent) // Escreve o conteúdo no arquivo

            // Notifica o MediaScanner para que o arquivo apareça em apps de galeria/downloads
            suspendCancellableCoroutine<Unit> { continuation ->
                MediaScannerConnection.scanFile(context, arrayOf(file.toString()), arrayOf("text/csv")) { _, uri ->
                    Log.d("DEBUG", "MediaScanner finalizou. Arquivo disponível em: $uri")
                }
            }

            // Retorna uma Uri segura e compartilhável usando o FileProvider
            return FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        }
    }
}