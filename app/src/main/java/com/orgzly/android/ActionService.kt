package com.orgzly.android

import android.app.IntentService
import android.content.Intent
import android.support.v4.content.LocalBroadcastManager
import com.orgzly.BuildConfig
import com.orgzly.R
import com.orgzly.android.prefs.AppPreferences
import com.orgzly.android.util.LogUtils


/**
 * TODO: Perform all actions in a single place
 */
class ActionService : IntentService(TAG) {
    init {
        setIntentRedelivery(true)
    }

    private val shelf = Shelf(this)
    private val localBroadcastManager = LocalBroadcastManager.getInstance(this)

    override fun onHandleIntent(intent: Intent?) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, intent)

        when (intent?.action) {
            AppIntent.ACTION_IMPORT_GETTING_STARTED_NOTEBOOK ->
                importGettingStartedNotebook()

            AppIntent.ACTION_REPARSE_NOTES ->
                reparseNotes()

            AppIntent.ACTION_CLEAR_DATABASE ->
                clearDatabase()
        }
    }

    private fun clearDatabase() {
        shelf.clearDatabase()

        val intent = Intent(AppIntent.ACTION_DB_CLEARED)
        localBroadcastManager.sendBroadcast(intent)
    }

    private fun reparseNotes() {
        localBroadcastManager.sendBroadcast(Intent(AppIntent.ACTION_REPARSING_NOTES_STARTED))
        shelf.reParseNotesStateAndTitles(null)
        localBroadcastManager.sendBroadcast(Intent(AppIntent.ACTION_REPARSING_NOTES_ENDED))
    }

    private fun importGettingStartedNotebook() {
        val book: Book = catch {
            shelf.loadBookFromResource(
                    GETTING_STARTED_NOTEBOOK_NAME,
                    BookName.Format.ORG,
                    resources,
                    GETTING_STARTED_NOTEBOOK_RESOURCE_ID)
        } ?: return

        /* Update status */
        shelf.setBookStatus(book, null, BookAction(
                BookAction.Type.INFO,
                resources.getString(R.string.loaded_from_resource, GETTING_STARTED_NOTEBOOK_NAME)))

        /* If notebook was already previously loaded, user probably requested reload.
         * Display a message in that case.
         */
        if (AppPreferences.isGettingStartedNotebookLoaded(this)) {
            val intent = Intent(AppIntent.ACTION_BOOK_LOADED)
            localBroadcastManager.sendBroadcast(intent)

        } else {
            AppPreferences.isGettingStartedNotebookLoaded(this, true)
        }
    }

    private fun <T>catch (f: () -> T): T? {
        return try {
            f()
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }
    }

    companion object {
        val TAG: String = ActionService::class.java.name

        const val GETTING_STARTED_NOTEBOOK_NAME = "Getting Started with Orgzly"
        const val GETTING_STARTED_NOTEBOOK_RESOURCE_ID = R.raw.orgzly_getting_started
    }
}
