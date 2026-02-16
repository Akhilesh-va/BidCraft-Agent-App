package com.example.bidcarftagent.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "srs_table")
data class SrsEntity(
    @PrimaryKey val filename: String,
    val srsJson: String?,
    val rawText: String?,
    val ts: Long,
    val status: String? = null,
    val proposalJson: String? = null,
    val reportPdfPath: String? = null
)

