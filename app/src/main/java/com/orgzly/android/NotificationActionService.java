package com.orgzly.android;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;

import com.orgzly.BuildConfig;
import com.orgzly.android.util.LogUtils;
import com.orgzly.android.reminders.SnoozeJob;

public class NotificationActionService extends IntentService {
    public static final String TAG = NotificationActionService.class.getName();

    public static final String EXTRA_NOTIFICATION_TAG = "notification_tag";
    public static final String EXTRA_NOTIFICATION_ID = "notification_id";

    public static final String EXTRA_NOTE_ID = "note_id";
    public static final String EXTRA_NOTE_TIME_TYPE = "note_time_type";
    public static final String EXTRA_SNOOZE_TIMESTAMP = "snooze_timestamp";

    public NotificationActionService() {
        super(TAG);

        setIntentRedelivery(true);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, intent);

        if (intent == null) {
            return;
        }

        dismissNotification(this, intent);

        if (AppIntent.ACTION_NOTE_MARK_AS_DONE.equals(intent.getAction())) {
            long noteId = intent.getLongExtra(EXTRA_NOTE_ID, 0);

            if (noteId > 0) {
                Shelf shelf = new Shelf(this);
                shelf.setStateToDone(noteId);
                shelf.syncOnNoteCreate();
            } else {
                throw new IllegalArgumentException("Missing note ID");
            }

        } else if (AppIntent.ACTION_REMINDER_SNOOZE_REQUEST.equals(intent.getAction())) {
            long noteId = intent.getLongExtra(NotificationActionService.EXTRA_NOTE_ID, 0);
            int noteTimeType = intent.getIntExtra(NotificationActionService.EXTRA_NOTE_TIME_TYPE, 0);
            long timestamp = intent.getLongExtra(NotificationActionService.EXTRA_SNOOZE_TIMESTAMP, 0);
            if (BuildConfig.LOG_DEBUG) LogUtils.d(TAG, noteId, timestamp);
            if (noteId > 0) {
                SnoozeJob.scheduleJob(this, noteId, noteTimeType, timestamp);
            } else {
                throw new IllegalArgumentException("Missing note id");
            }
        }
    }

    /**
     * If notification ID was passed as an extra,
     * it means this action was performed from the notification.
     * Cancel the notification here.
     */
    private void dismissNotification(Context context, Intent intent) {
        int id = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0);
        String tag = intent.getStringExtra(EXTRA_NOTIFICATION_TAG);

        if (id > 0) {
            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            notificationManager.cancel(tag, id);
        }
    }
}
