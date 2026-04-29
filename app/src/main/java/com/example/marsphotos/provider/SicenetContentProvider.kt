package com.example.marsphotos.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.sqlite.db.SupportSQLiteQueryBuilder
import com.example.marsphotos.data.local.SicenetDatabase

class SicenetContentProvider : ContentProvider() {

    companion object {
        const val AUTHORITY = "com.example.marsphotos.provider"
        const val PATH_ACADEMIC_LOAD = "academic_load"
        const val PATH_CARDEX = "cardex"

        const val ACADEMIC_LOAD_DIR = 100
        const val CARDEX_DIR = 200

        val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, PATH_ACADEMIC_LOAD, ACADEMIC_LOAD_DIR)
            addURI(AUTHORITY, PATH_CARDEX, CARDEX_DIR)
        }
    }

    override fun onCreate(): Boolean {
        // Initialization is done lazily when required
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        val context = context ?: return null
        val database = SicenetDatabase.getDatabase(context).openHelper.readableDatabase

        val tableName = when (uriMatcher.match(uri)) {
            ACADEMIC_LOAD_DIR -> "academic_load"
            CARDEX_DIR -> "cardex"
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }

        val queryBuilder = SupportSQLiteQueryBuilder.builder(tableName)
            .columns(projection)
            .selection(selection, selectionArgs)
            .orderBy(sortOrder)

        val cursor = database.query(queryBuilder.create())
        cursor.setNotificationUri(context.contentResolver, uri)
        return cursor
    }

    override fun getType(uri: Uri): String? {
        return when (uriMatcher.match(uri)) {
            ACADEMIC_LOAD_DIR -> "vnd.android.cursor.dir/vnd.$AUTHORITY.$PATH_ACADEMIC_LOAD"
            CARDEX_DIR -> "vnd.android.cursor.dir/vnd.$AUTHORITY.$PATH_CARDEX"
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        val context = context ?: return null
        if (values == null) return null
        
        val database = SicenetDatabase.getDatabase(context).openHelper.writableDatabase
        
        val tableName = when (uriMatcher.match(uri)) {
            ACADEMIC_LOAD_DIR -> "academic_load"
            CARDEX_DIR -> "cardex"
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }

        val id = database.insert(tableName, SQLiteDatabase.CONFLICT_REPLACE, values)
        
        context.contentResolver.notifyChange(uri, null)
        return Uri.withAppendedPath(uri, id.toString())
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        val context = context ?: return 0
        val database = SicenetDatabase.getDatabase(context).openHelper.writableDatabase

        val tableName = when (uriMatcher.match(uri)) {
            ACADEMIC_LOAD_DIR -> "academic_load"
            CARDEX_DIR -> "cardex"
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }

        val deletedRows = database.delete(tableName, selection, selectionArgs)
        if (deletedRows > 0) {
            context.contentResolver.notifyChange(uri, null)
        }
        return deletedRows
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        val context = context ?: return 0
        if (values == null) return 0
        
        val database = SicenetDatabase.getDatabase(context).openHelper.writableDatabase

        val tableName = when (uriMatcher.match(uri)) {
            ACADEMIC_LOAD_DIR -> "academic_load"
            CARDEX_DIR -> "cardex"
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }

        val updatedRows = database.update(
            tableName,
            SQLiteDatabase.CONFLICT_REPLACE,
            values,
            selection,
            selectionArgs
        )
        if (updatedRows > 0) {
            context.contentResolver.notifyChange(uri, null)
        }
        return updatedRows
    }
}
