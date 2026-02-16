package com.example.bidcarftagent.data.repository

import android.net.Uri
import com.example.bidcarftagent.data.network.ApiService
import com.example.bidcarftagent.presentation.auth.AuthManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.ResponseBody
import java.io.File
import javax.inject.Inject
import android.util.Log

class BackendRepository @Inject constructor(
    private val api: ApiService,
    private val authManager: AuthManager
    , private val localRepo: com.example.bidcarftagent.data.local.LocalStorageRepository
) {
    private suspend fun getAuthHeader(): String {
        val tokenRes = authManager.getIdToken()
        if (tokenRes.isSuccess) {
            val tok = tokenRes.getOrNull() ?: ""
            // log token length but never log full token
            Log.d("BackendRepository", "Using ID token length=${tok.length}")
            return "Bearer $tok"
        }
        throw Exception("No auth token")
    }

    suspend fun uploadProfilePdf(filePath: String): String? = withContext(Dispatchers.IO) {
        val header = getAuthHeader()
        val file = File(filePath)
        Log.d("BackendRepository", "Uploading profile PDF: ${file.absolutePath}")
        val reqBody: RequestBody = file.asRequestBody("application/pdf".toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", file.name, reqBody)
        val resp = api.uploadProfile(header, part)
        try {
            val bodyString = resp.errorBody()?.string() ?: resp.body()?.string()
            Log.d("BackendRepository", "uploadProfile response: code=${resp.code()}, body=$bodyString")
            return@withContext bodyString
        } catch (e: Exception) {
            Log.w("BackendRepository", "Failed to read response body", e)
        }
        return@withContext null
    }

    suspend fun uploadSrsPdf(filePath: String): String? = withContext(Dispatchers.IO) {
        val header = getAuthHeader()
        val file = File(filePath)
        Log.d("BackendRepository", "Uploading SRS PDF: ${file.absolutePath}")
        val reqBody: RequestBody = file.asRequestBody("application/pdf".toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", file.name, reqBody)
        val resp = api.uploadSrs(header, part)
        try {
            val bodyString = resp.errorBody()?.string() ?: resp.body()?.string()
            Log.d("BackendRepository", "uploadSrs response: code=${resp.code()}, body=$bodyString")
            return@withContext bodyString
        } catch (e: Exception) {
            Log.w("BackendRepository", "Failed to read response body", e)
        }
        return@withContext null
    }

    suspend fun getProfile(): String? = withContext(Dispatchers.IO) {
        val header = getAuthHeader()
        val resp = api.getProfile(header)
        try {
            val bodyString = resp.errorBody()?.string() ?: resp.body()?.string()
            Log.d("BackendRepository", "getProfile response: code=${resp.code()}, body=$bodyString")
            return@withContext bodyString
        } catch (e: Exception) {
            Log.w("BackendRepository", "Failed to read profile response body", e)
        }
        return@withContext null
    }

    suspend fun generateProposalForFile(fileName: String): String? = withContext(Dispatchers.IO) {
        val header = getAuthHeader()
        val srsJson = localRepo.getSrsForFile(fileName) ?: return@withContext null
        try {
            val gson = com.google.gson.Gson()
            val parsed = gson.fromJson(srsJson, Map::class.java) as? Map<*, *> ?: return@withContext null
            // Convert keys to String for Retrofit @Body
            val map = HashMap<String, Any?>()
            parsed.forEach { (k, v) -> if (k != null) map[k.toString()] = v }
            val resp = api.generateProposal(header, map as Map<String, Any>)
            val bodyString = resp.errorBody()?.string() ?: resp.body()?.string()
            Log.d("BackendRepository", "generateProposal response: code=${resp.code()}, body=$bodyString")
            // Persist proposal and generated PDF (if present) into local storage for offline access
            try {
                if (resp.isSuccessful && !bodyString.isNullOrBlank()) {
                    // try to extract reportHtml from response JSON
                    val gson = com.google.gson.Gson()
                    val obj = gson.fromJson(bodyString, Map::class.java) as? Map<*, *>
                    val reportHtml = obj?.get("reportHtml") as? String
                    // save proposal JSON under same filename key and mark as last only on success
                    localRepo.saveProposalForFile(fileName, bodyString, reportHtml)
                    localRepo.setLastProposalFile(fileName)
                } else {
                    Log.w("BackendRepository", "generateProposal not successful: code=${resp.code()}")
                }
            } catch (e: Exception) {
                Log.w("BackendRepository", "Failed to persist proposal locally", e)
            }
            return@withContext bodyString
        } catch (e: Exception) {
            Log.w("BackendRepository", "Failed to send generateProposal", e)
            return@withContext null
        }
    }
}

