package com.example.bidcarftagent.data.local

import com.example.bidcarftagent.data.local.db.SrsDao
import com.example.bidcarftagent.data.local.db.SrsEntity
import com.example.bidcarftagent.presentation.home.RecentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton
import android.content.Context
import org.json.JSONArray
import java.io.File

@Singleton
class LocalStorageRepository @Inject constructor(
    private val srsDao: SrsDao,
    @ApplicationContext private val ctx: Context
) {
    private val prefsName = "bidcraft_local_storage"
    private val keyRecent = "recent_bids"
    private val keySrsCache = "srs_cache"

    private fun prefs() = ctx.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
    // emit filenames when a proposal is persisted successfully
    private val _proposalSaved = MutableSharedFlow<String>(replay = 0)
    val proposalSaved = _proposalSaved.asSharedFlow()

    // Migrate any old shared-preferences cache into Room on demand
    private suspend fun migrateIfNeeded() = withContext(Dispatchers.IO) {
        val existing = srsDao.getRecent(1)
        if (existing.isNotEmpty()) return@withContext
        // read old srs cache
        val json = prefs().getString(keySrsCache, null) ?: return@withContext
        try {
            val obj = org.json.JSONObject(json)
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val v = obj.optString(k)
                if (v.isNotEmpty()) {
                    val ent = SrsEntity(k, v, null, System.currentTimeMillis())
                    srsDao.insert(ent)
                }
            }
        } catch (_: Exception) { /* ignore */ }

        // migrate recent list if present
        val recJson = prefs().getString(keyRecent, null) ?: return@withContext
        try {
            val arr = JSONArray(recJson)
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i)
                if (o != null) {
                    val name = o.optString("name", "")
                    val ts = o.optLong("ts", System.currentTimeMillis())
                    if (name.isNotEmpty()) {
                        val existingEnt = srsDao.getByFilename(name)
                        if (existingEnt == null) {
                            srsDao.insert(SrsEntity(name, null, null, ts))
                        } else {
                            srsDao.insert(existingEnt.copy(ts = ts))
                        }
                    }
                }
            }
        } catch (_: Exception) { /* ignore */ }
    }

    suspend fun getRecentFiles(): List<RecentFile> = withContext(Dispatchers.IO) {
        migrateIfNeeded()
        val ents = srsDao.getRecent(10)
        ents.map { RecentFile(it.filename, it.ts) }
    }

    suspend fun addRecentFile(name: String, limit: Int = 10) = withContext(Dispatchers.IO) {
        migrateIfNeeded()
        val now = System.currentTimeMillis()
        val existing = srsDao.getByFilename(name)
        if (existing == null) {
            srsDao.insert(SrsEntity(name, null, null, now))
        } else {
            srsDao.insert(existing.copy(ts = now))
        }
        // trim to limit
        val all = srsDao.getRecent(100)
        if (all.size > limit) {
            val toRemove = all.drop(limit)
            toRemove.forEach { srsDao.deleteByFilename(it.filename) }
        }
    }

    suspend fun saveSrsForFile(name: String, srsJson: String) = withContext(Dispatchers.IO) {
        migrateIfNeeded()
        val now = System.currentTimeMillis()
        val existing = srsDao.getByFilename(name)
        if (existing == null) {
            srsDao.insert(SrsEntity(name, srsJson, null, now))
        } else {
            srsDao.insert(existing.copy(srsJson = srsJson, ts = now))
        }
    }

    suspend fun saveProposalForFile(name: String, proposalJson: String, reportHtml: String? = null) = withContext(Dispatchers.IO) {
        migrateIfNeeded()
        val now = System.currentTimeMillis()
        val existing = srsDao.getByFilename(name)
        // Just persist JSON immediately — PDF is generated lazily on Save/Share tap
        if (existing == null) {
            srsDao.insert(SrsEntity(name, null, reportHtml, now, null, proposalJson, null))
        } else {
            srsDao.insert(existing.copy(
                proposalJson = proposalJson,
                rawText = if (!reportHtml.isNullOrBlank()) reportHtml else existing.rawText,
                ts = now
            ))
        }
        prefs().edit().putString("last_proposal_filename", name).apply()
        Log.d("LocalStorageRepo", "Saved proposal for file=$name (no PDF yet, will generate lazily)")
        // notify collectors that proposal is saved
        try {
            _proposalSaved.emit(name)
            Log.d("LocalStorageRepo", "Emitted proposalSaved for $name")
        } catch (ex: Exception) {
            Log.w("LocalStorageRepo", "Failed to emit proposalSaved for $name: ${ex.message}")
        }
    }

    suspend fun savePdfPathForFile(name: String, pdfPath: String) = withContext(Dispatchers.IO) {
        val existing = srsDao.getByFilename(name) ?: return@withContext
        srsDao.insert(existing.copy(reportPdfPath = pdfPath))
        Log.d("LocalStorageRepo", "Saved PDF path for file=$name: $pdfPath")
    }

    suspend fun getProposalForFile(name: String): String? = withContext(Dispatchers.IO) {
        migrateIfNeeded()
        val ent = srsDao.getByFilename(name) ?: return@withContext null
        return@withContext ent.proposalJson
    }

    fun setLastProposalFile(name: String) {
        prefs().edit().putString("last_proposal_filename", name).apply()
    }

    fun getLastProposalFile(): String? {
        return prefs().getString("last_proposal_filename", null)
    }
    
    fun setWaitingForProposalFile(name: String) {
        prefs().edit().putString("waiting_proposal_filename", name).apply()
    }

    fun getWaitingForProposalFile(): String? {
        return prefs().getString("waiting_proposal_filename", null)
    }

    fun clearWaitingForProposalFile() {
        prefs().edit().remove("waiting_proposal_filename").apply()
    }

    suspend fun getSrsForFile(name: String): String? = withContext(Dispatchers.IO) {
        migrateIfNeeded()
        val ent = srsDao.getByFilename(name) ?: return@withContext null
        return@withContext ent.srsJson
    }

    // (saveProposalForFile and getProposalForFile implemented above)

    suspend fun getReportPdfPathForFile(name: String): String? = withContext(Dispatchers.IO) {
        migrateIfNeeded()
        val ent = srsDao.getByFilename(name) ?: return@withContext null
        return@withContext ent.reportPdfPath
    }

    suspend fun getAllSrs(): List<SrsEntity> = withContext(Dispatchers.IO) {
        migrateIfNeeded()
        srsDao.getAll()
    }
}
