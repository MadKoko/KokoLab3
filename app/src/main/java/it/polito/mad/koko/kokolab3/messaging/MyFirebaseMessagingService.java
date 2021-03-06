/**
 * Copyright 2016 Google Inc. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.polito.mad.koko.kokolab3.messaging;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import it.polito.mad.koko.kokolab3.R;
import it.polito.mad.koko.kokolab3.profile.Profile;
import it.polito.mad.koko.kokolab3.profile.ProfileManager;
import it.polito.mad.koko.kokolab3.ui.ImageManager;
import it.polito.mad.koko.kokolab3.util.JsonUtil;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";

    /**
     * Counter to increment notification ID
     */
    private static int counterNotificationId = 0;

    /**
     * Currently active chat
     */
    private static String activeChatId = "";

    /**
     * Notification properties
     */
    private static final int NOTIFICATION_ICON = R.mipmap.notification_icon,
            NOTIFICATION_PRIORITY = NotificationCompat.PRIORITY_MAX;

    /**
     * Request notification properties
     */
    protected static final String REQUEST_ACTION = "request";
    private static final int REQUEST_REQUEST_CODE = 1;

    /**
     * Accepting a book exchange request actions and properties
     */
    private static final String ACCEPT_BUTTON_STRING = "Accept";
    private static final int ACCEPT_ICON = R.mipmap.icon,
            ACCEPT_REQUEST_CODE = 2;
    protected static final String ACCEPT_ACTION = "accept";

    /**
     * Denying a book exchange request actions and properties
     */
    private static final String DECLINE_BUTTON_STRING = "Decline";
    private static final int DECLINE_ICON = R.mipmap.icon,
            DECLINE_REQUEST_CODE = 3;
    protected static final String DECLINE_ACTION = "decline";

    /**
     * Message notification properties
     */
    protected static final String MESSAGE_ACTION = "message";

    /**
     * Tapping on the notification actions
     */
    private static final int    ON_TAP_NO_ACTION = 0,
                                ON_TAP_SHOW_PROFILE = 1,
                                ON_TAP_SHOW_CHAT = 2;

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        if(ProfileManager.hasLoggedIn()) {
            // [START_EXCLUDE]
            // There are two types of messages: data messages and notification messages. Data messages are handled
            // here in onMessageReceived whether the app is in the foreground or background. Data messages are the type
            // traditionally used with GCM. Notification messages are only received here in onMessageReceived when the app
            // is in the foreground. When the app is in the background an automatically generated notification is displayed.
            // When the user taps on the notification they are returned to the app. Messages containing both notification
            // and data payloads are treated as notification messages. The Firebase console always sends notification
            // messages. For more see: https://firebase.google.com/docs/cloud-messaging/concept-options
            // [END_EXCLUDE]

            // TODO(developer): Handle FCM messages here.
            // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
            Log.d(TAG, "From: " + remoteMessage.getFrom());


            MessageManager.setUserChatsIDListener();
            ProfileManager.populateUsersList();
            MessageManager.populateUserChatsID();
            MessageManager.populateUserMessages();
            // MessageManager.createChat(Intent intent, String bookTitle); // DA FARE -> dov'è l'intent? dov'è book title?!

            // Check if message contains a data payload.
            if (remoteMessage.getData().size() > 0) {
                Log.d(TAG, "Received message with a payload");

                // Retrieving the notification type
                String notificationType = remoteMessage.getData().get("type");

                /*  Figuring out whether it is a request notification
                    or not, so action buttons will be displayed accordingly */
                boolean showResponseButtons = notificationType.compareTo("request") == 0;

                // Figuring out what action should be performed upon tapping the notification
                int onTapAction = ON_TAP_NO_ACTION; // nothing has to be performed
                if (notificationType.compareTo("request") == 0)
                    onTapAction = ON_TAP_SHOW_PROFILE; // the sender's profile has to be shown
                else if (notificationType.compareTo("message") == 0 || notificationType.compareTo("accept") == 0)
                    onTapAction = ON_TAP_SHOW_CHAT; // the chat with the sender has to be opened

                // If this is a new message notification
                if (notificationType.compareTo("message") == 0) {
                    synchronized (activeChatId) {
                        // Retrieving the chat ID corresponding to this new notification
                        String chatID = remoteMessage.getData().get("chatID");

                        // !! If the chat corresponding to this notification is not currently active
                        if (chatID.compareTo(activeChatId) != 0)
                            // Show the new message notification
                            showNotification(remoteMessage, onTapAction);
                    }
                } else {
                    // Always show the notification
                    showNotification(remoteMessage, onTapAction);
                }
            }

            // Check if message contains a notification payload.
            if (remoteMessage.getNotification() != null) {
                Log.d(TAG, "Received message with a notification");
            }

            // Also if you intend on generating your own notifications as a result of a received FCM
            // message, here is where that should be initiated. See showNotification method below.
        }
    }
    // [END receive_message]

    /**
     * Create and show a simple notification containing the received FCM message.
     *
     * @param receivedMessage     the received remote message.
     * @param onTapAction         the action that must be performed upon tapping on the
     *                            notification. Possible values:
     *                            ON_TAP_NO_ACTION:     nothing has to be performed
     *                            ON_TAP_SHOW_PROFILE:  the sender's profile has to be shown
     *                            ON_TAP_SHOW_CHAT:     the chat with the sender has to be opened
     */
    private void showNotification(RemoteMessage receivedMessage, int onTapAction) {
        // Retrieving the notification data object
        Map<String, String> notificationObject = JsonUtil.deserialize(receivedMessage.getData().get("notification"));

        // Retrieving notification information
        String notificationTitle = notificationObject.get("title");
        String notificationBody = notificationObject.get("body");
        Log.d(TAG, "Notification data: " + JsonUtil.formatJson(receivedMessage.getData().toString()));

        // Retrieving chat information
        String chatId = receivedMessage.getData().get("chatID");

        // Retrieving the sender data object
        Map<String, String> senderObject = JsonUtil.deserialize(receivedMessage.getData().get("sender"));

        // Retrieving the receiver data object
        Map<String, String> receiverObject = JsonUtil.deserialize(receivedMessage.getData().get("receiver"));

        // Retrieving the book data object
        Map<String, String> bookObject = JsonUtil.deserialize(receivedMessage.getData().get("book"));

        // Intent used upon accepting the book exchange request
        PendingIntent acceptPendingIntent = createAcceptPendingIntent(this, chatId, senderObject, receiverObject, bookObject);

        // Intent used upon declining the book exchange request
        PendingIntent declinePendingIntent = createDeclinePendingIntent(this, chatId, senderObject, receiverObject, bookObject);

        // Creating the notification
        String channelId = getString(R.string.default_notification_channel_id);
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        // Retrieving the notification group
        String notificationType = receivedMessage.getData().get("type");

        // Building the base notification
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        // Notification's icon
                        .setSmallIcon(NOTIFICATION_ICON)

                        // Sender's user set
                        .setLargeIcon(ImageManager.getCircleBitmap(ImageManager.getBitmapFromURL(senderObject.get("image"))))

                        // Title and expandable subtitle
                        .setContentTitle(notificationTitle)
                        .setContentText(notificationBody)
                        // .setSubText() // TODO cumulative number of requests

                        // Stack notification
                        .setGroup(notificationType)
                        .setGroupSummary(true)

                        // Priorities, sound and style
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setPriority(NOTIFICATION_PRIORITY)
                        .setOnlyAlertOnce(false)
                        .setColorized(true);

        switch(notificationType) {
            case "message":
                // Retrieving the sender's username
                String senderUsername = senderObject.get("username");

                // Building the message object
                NotificationCompat.MessagingStyle.Message message =
                        new NotificationCompat.MessagingStyle.Message(
                                notificationBody,
                                System.currentTimeMillis(), // TODO sent time or received time?
                                senderUsername
                        );

                // Setting the message notification style
                notificationBuilder.setStyle(
                        new NotificationCompat.MessagingStyle(senderUsername).addMessage(message)
                );

                break;

            case "request":
                // Notification action buttons
                notificationBuilder
                        .addAction(ACCEPT_ICON, ACCEPT_BUTTON_STRING, acceptPendingIntent)
                        .addAction(DECLINE_ICON, DECLINE_BUTTON_STRING, declinePendingIntent);

                break;

            default:

        }

        if(notificationType.compareTo("message") != 0)
            // Setting the big text notification style
            notificationBuilder.setStyle(
                    new NotificationCompat.BigTextStyle().bigText(notificationBody)
            );

        // Action to be performed upon tapping the notification
        switch (onTapAction) {
            // Nothing has to be performed
            case ON_TAP_NO_ACTION:
                break;

            // The sender's profile has to be shown
            case ON_TAP_SHOW_PROFILE:

                // Creating the requestIntent that will open the request sender's profile
                Intent requestIntent = new Intent(this, NotificationReceiver.class);
                requestIntent.setAction(REQUEST_ACTION);
                requestIntent.putExtra("chatID", chatId); // * serve davvero?!
                requestIntent.putExtra("UserID", FirebaseAuth.getInstance().getUid()); //serve ?! /* TODO insert sender's ID */
                // !!!! puttare qua userChaTInfo?!
                PendingIntent requestPendingIntent = PendingIntent.getBroadcast(this, REQUEST_REQUEST_CODE, requestIntent,
                        PendingIntent.FLAG_ONE_SHOT);

                // Setting the onTap intent
                notificationBuilder.setContentIntent(requestPendingIntent);

                /*Intent requestIntent = new Intent(this, NotificationReceiver.class);
                requestIntent.setAction(REQUEST_ACTION);
                requestIntent.putExtra("UserID", *//* TODO insert sender's ID *//* ProfileManager.getCurrentUserID());
                requestIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                PendingIntent requestPendingIntent = PendingIntent.getBroadcast(this, REQUEST_REQUEST_CODE, requestIntent,
                        PendingIntent.FLAG_ONE_SHOT);*/

                // Setting the onTap intent
                // notificationBuilder.setContentIntent(requestPendingIntent);

                break;

            // The chat with the sender has to be opened
            case ON_TAP_SHOW_CHAT:
                // Creating the messageIntent that will open the chat with the request's sender
                PendingIntent messagePendingIntent = createMessagePendingIntent(this, chatId, senderObject, receiverObject, bookObject);

                // Setting the onTap intent
                notificationBuilder.setContentIntent(messagePendingIntent);

                break;

            default:
                throw new IllegalArgumentException("Illegal onTapAction value.\n" +
                        "Possible values:\n" +
                        "\t0: nothing has to be performed\n" +
                        "\t1: the sender's profile has to be shown\n" +
                        "\t2: the chat with the sender has to be opened"
                );
        }

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }
        Notification builtNotification = notificationBuilder.build();
        notificationManager.notify(counterNotificationId++/* notification ID */, builtNotification);
        //notificationManager.cancelAll();
    }

    private static void loadExchangeIntentData(Intent exchangeIntent,
                                               Map<String, String> sender,
                                               Map<String, String> receiver,
                                               Map<String, String> book) {
        // Notification ID
        exchangeIntent.putExtra("notificationID", counterNotificationId);

        // Sender info
        exchangeIntent.putExtra("senderId", sender.get("id"));
        exchangeIntent.putExtra("senderUsername", sender.get("username"));
        exchangeIntent.putExtra("senderImage", sender.get("image"));
        exchangeIntent.putExtra("senderToken", sender.get("token"));

        // Receiver info
        exchangeIntent.putExtra("receiverId", receiver.get("id"));
        exchangeIntent.putExtra("receiverUsername", receiver.get("username"));
        exchangeIntent.putExtra("receiverImage", receiver.get("image"));
        exchangeIntent.putExtra("receiverToken", receiver.get("token"));

        // Book info
        exchangeIntent.putExtra("bookId", book.get("id"));
        exchangeIntent.putExtra("bookTitle", book.get("title"));
    }

    public static PendingIntent createAcceptPendingIntent(Context context,
                                                          String chatId,
                                                          Map<String, String> senderObject,
                                                          Map<String, String> receiverObject,
                                                          Map<String, String> bookObject) {
        Intent acceptIntent = new Intent(context, NotificationReceiver.class);
        acceptIntent.setAction(ACCEPT_ACTION);
        acceptIntent.putExtra("chatID", chatId);

        // TODO delete these useless fields: mantained for backward compatibility
        acceptIntent.putExtra("receiverInfo", new UserChatInfo(receiverObject));
        acceptIntent.putExtra("senderInfo", new UserChatInfo(senderObject));

        loadExchangeIntentData(acceptIntent, senderObject, receiverObject, bookObject);
        return PendingIntent.getBroadcast(context, ACCEPT_REQUEST_CODE, acceptIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static PendingIntent createDeclinePendingIntent(Context context,
                                                           String chatId,
                                                           Map<String, String> senderObject,
                                                           Map<String, String> receiverObject,
                                                           Map<String, String> bookObject) {
        Intent declineIntent = new Intent(context, NotificationReceiver.class);
        declineIntent.setAction(DECLINE_ACTION);
        declineIntent.putExtra("chatID", chatId);
        loadExchangeIntentData(declineIntent, senderObject, receiverObject, bookObject);
        return PendingIntent.getBroadcast(context, DECLINE_REQUEST_CODE, declineIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static PendingIntent createMessagePendingIntent(Context context,
                                                            String chatId,
                                                            Map<String, String> senderObject,
                                                            Map<String, String> receiverObject,
                                                            Map<String, String> bookObject) {
        Intent messageIntent = new Intent(context, NotificationReceiver.class);
        messageIntent.setAction(MESSAGE_ACTION);
        messageIntent.putExtra("chatID", chatId);
        messageIntent.putExtra("receiverInfo", new UserChatInfo(receiverObject));
        messageIntent.putExtra("senderInfo", new UserChatInfo(senderObject));
        return PendingIntent.getBroadcast(context, REQUEST_REQUEST_CODE, messageIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);
    }

    /**
     * Setting the currently active chat ID
     *
     * @param activeChatId the currently active chat ID
     */
    public static void setActiveChat(String activeChatId) {
        if (activeChatId != null)
            synchronized (MyFirebaseMessagingService.activeChatId) {
                MyFirebaseMessagingService.activeChatId = activeChatId;
            }
    }

    /**
     * Clearing the currently active chat ID
     */
    public static void clearActiveChat() {
        setActiveChat("");
    }

    /**
     * check if the activity is running on background or foreground
     *
     * @param context context of this method (the service in this case)
     * @return false if any of your activity is in foreground; true otherwise.
     */
    public static boolean isApplicationSentToBackground(final Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(1);
        if (!tasks.isEmpty()) {
            ComponentName topActivity = tasks.get(0).topActivity;

            Log.d(TAG, "top activity: " + topActivity.getShortClassName().substring(1));

            if (!topActivity.getPackageName().equals(context.getPackageName())) {
                return true;
            }
        }

        return false;
    }
}