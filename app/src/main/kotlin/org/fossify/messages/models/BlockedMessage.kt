package org.fossify.messages.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "blocked_messages",
    indices = [Index(value = ["address"])]
)
data class BlockedMessage(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "address") val address: String,
    @ColumnInfo(name = "body") val body: String,
    @ColumnInfo(name = "date") val date: Long,
    @ColumnInfo(name = "subscription_id") val subscriptionId: Int,
    @ColumnInfo(name = "reason") val reason: Int,
    @ColumnInfo(name = "matched_keyword") val matchedKeyword: String? = null,
    @ColumnInfo(name = "seen") val seen: Boolean = false,
)
