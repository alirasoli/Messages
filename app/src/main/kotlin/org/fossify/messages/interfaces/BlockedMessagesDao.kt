package org.fossify.messages.interfaces

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import org.fossify.messages.models.BlockedMessage

@Dao
interface BlockedMessagesDao {
    @Insert
    fun insert(message: BlockedMessage): Long

    @Query("SELECT * FROM blocked_messages ORDER BY date DESC, id DESC")
    fun getAll(): List<BlockedMessage>

    @Query("SELECT * FROM blocked_messages WHERE address = :address ORDER BY date ASC, id ASC")
    fun getForAddress(address: String): List<BlockedMessage>

    @Query("SELECT * FROM blocked_messages WHERE address IN (:addresses)")
    fun getForAddresses(addresses: List<String>): List<BlockedMessage>

    @Query("DELETE FROM blocked_messages WHERE id IN (:ids)")
    fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM blocked_messages WHERE address IN (:addresses)")
    fun deleteByAddresses(addresses: List<String>)

    @Query("DELETE FROM blocked_messages WHERE date < :timestamp")
    fun deleteOlderThan(timestamp: Long)

    @Query("DELETE FROM blocked_messages")
    fun deleteAll()
}
