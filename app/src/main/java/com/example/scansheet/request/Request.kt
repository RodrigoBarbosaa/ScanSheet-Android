package com.example.scansheet.request

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.security.SecureRandom
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec


sealed class UploadResult {
    data class Success(val fileUri: Uri) : UploadResult()
    data class Failure(val message: String) : UploadResult()
    object Loading : UploadResult()
}

class Request(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .callTimeout(120, TimeUnit.SECONDS)       // Timeout total da chamada
        .connectTimeout(120, TimeUnit.SECONDS)    // Timeout para estabelecer a conexão
        .readTimeout(120, TimeUnit.SECONDS)       // Timeout entre a chegada de cada byte
        .writeTimeout(120, TimeUnit.SECONDS)      // Timeout para enviar a requisição
        .build()

    // Adaptador Moshi para converter objetos Kotlin para JSON
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    // Constantes para evitar "magic strings"
    companion object {
        private const val API_URL = "https://scansheet-api-6wfd.onrender.com/process-image"
        private const val AUTH_TOKEN = "u3-yBDVGGh40o1L7uth"
        private const val ENCRYPTION_KEY = "qaqn0vD3fx4ibDB84m2Kmoaj90wxDb7zBLGAevu4MtY="
        private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_SIZE_BYTES = 12 // Nonce de 12 bytes, padrão para GCM
        private const val TAG_SIZE_BITS = 128 // Tamanho da tag de autenticação
    }

    suspend fun createUploadRequest(imageUris: List<Uri>): UploadResult {

        Log.d("DEBUG", "createUploadRequest: Chamado ")
        return withContext(Dispatchers.IO) {
            try {
                // 1. Converter URIs de imagem para ByteArrays
                val imageBytesList = imageUris.mapNotNull { uri ->
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        // Comprimir a imagem para JPEG, similar ao código Swift
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        val outputStream = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                        outputStream.toByteArray()
                    }
                }

                if (imageBytesList.size != imageUris.size) {
                    return@withContext UploadResult.Failure("Erro ao processar as imagens.")
                }

                // 2. Criar o payload JSON inicial e codificar as imagens em Base64
                val payloadMap = mapOf(
                    "image_bytes" to imageBytesList.map { Base64.encodeToString(it, Base64.NO_WRAP) },
                    "title" to "outros"
                )

                // 3. Criptografar o payload
                val jsonAdapter = moshi.adapter<Map<String, Any>>(Map::class.java)
                val payloadJson = jsonAdapter.toJson(payloadMap)
                val encryptedData = encryptPayload(payloadJson)
                    ?: return@withContext UploadResult.Failure("Erro na criptografia dos dados.")

                // 4. Criar o payload final para a requisição
                val requestPayloadMap = mapOf("payload" to Base64.encodeToString(encryptedData, Base64.NO_WRAP))
                val requestPayloadJson = moshi.adapter<Map<String, String>>(Map::class.java).toJson(requestPayloadMap)

                // 5. Construir e executar a requisição com OkHttp
                val requestBody = requestPayloadJson.toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url(API_URL)
                    .post(requestBody)
                    .addHeader("Authorization", AUTH_TOKEN)
                    .addHeader("Content-Type", "application/json")
                    .build()

                Log.d("DEBUG", "REQUISIÇÃO CRIADA com sucesso!")

                client.newCall(request).execute().use { response ->
                    // 6. Processar a resposta do servidor
                    if (!response.isSuccessful) {
                        val errorMessage = when (response.code) {
                            400 -> "Dados enviados são inválidos"
                            401 -> "Não autorizado. Verifique as credenciais"
                            403 -> "Acesso negado"
                            404 -> "Serviço não encontrado"
                            in 500..599 -> "Erro interno do servidor"
                            else -> "Erro no servidor (código ${response.code})"
                        }
                        Log.d("DEBUG", "ERRO: $errorMessage")
                        return@withContext UploadResult.Failure(errorMessage)
                    }

                    Log.d("DEBUG", "REQUISIÇÃO RECEBIDA com sucesso!")

                    val responseBody = response.body?.string()
                        ?: return@withContext UploadResult.Failure("Nenhum dado recebido do servidor.")

                    // 7. Extrair, decodificar e descriptografar a resposta
                    val responseJsonAdapter = moshi.adapter<Map<String, String>>(Map::class.java)
                    val responseMap = responseJsonAdapter.fromJson(responseBody)
                    val encryptedTable = responseMap?.get("table")
                        ?: return@withContext UploadResult.Failure("Resposta do servidor está em formato inválido.")

                    val encryptedTableData = Base64.decode(encryptedTable, Base64.NO_WRAP)
                    val decryptedJsonString = decryptPayload(encryptedTableData)
                        ?: return@withContext UploadResult.Failure("Erro ao descriptografar dados do servidor.")

                    // TODO: Chamar lógica para processar e salvar o CSV
                    // Ex: CSVHandler.processAndSaveCSV(decryptedCsvString)
                    Log.d("DEBUG", "createUploadRequest: CSV Descriptografado com sucesso!")
                    val x = decryptedJsonString
                    Log.d("DEBUG", "createUploadRequest: $x")
                    val csvResult = CSVHandler.processAndSaveCsv(decryptedJsonString, context)

                    return@withContext if (csvResult.isSuccess) {
                        // Se o CSV foi salvo, retorna Sucesso com a URI do arquivo
                        UploadResult.Success(csvResult.getOrThrow())
                    } else {
                        // Se falhou, retorna Falha com a mensagem do erro
                        UploadResult.Failure(csvResult.exceptionOrNull()?.message ?: "Erro desconhecido ao salvar o arquivo CSV.")
                    }

                    UploadResult.Success(csvResult.getOrThrow())
                }

            } catch (e: IOException) {
                // Trata erros de conexão, timeout, etc.
                return@withContext UploadResult.Failure("Erro de conexão: ${e.message}")
            } catch (e: Exception) {
                // Trata outros erros inesperados (criptografia, JSON, etc.)
                return@withContext UploadResult.Failure("Ocorreu um erro inesperado: ${e.message}")
            }
        }
    }

    private fun encryptPayload(payloadJson: String): ByteArray? {
        return try {
            val keyData = Base64.decode(ENCRYPTION_KEY, Base64.NO_WRAP)
            val secretKey = SecretKeySpec(keyData, "AES")

            val iv = ByteArray(IV_SIZE_BYTES)
            SecureRandom().nextBytes(iv)
            val gcmParameterSpec = GCMParameterSpec(TAG_SIZE_BITS, iv)

            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec)

            val encryptedData = cipher.doFinal(payloadJson.toByteArray(Charsets.UTF_8))

            iv + encryptedData
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun decryptPayload(encryptedData: ByteArray): String? {
        return try {
            val keyData = Base64.decode(ENCRYPTION_KEY, Base64.NO_WRAP)
            val secretKey = SecretKeySpec(keyData, "AES")

            val iv = encryptedData.copyOfRange(0, IV_SIZE_BYTES)
            val ciphertext = encryptedData.copyOfRange(IV_SIZE_BYTES, encryptedData.size)

            val gcmParameterSpec = GCMParameterSpec(TAG_SIZE_BITS, iv)
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec)

            val decryptedData = cipher.doFinal(ciphertext)
            String(decryptedData, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}