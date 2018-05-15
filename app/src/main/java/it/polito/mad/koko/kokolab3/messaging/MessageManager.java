package it.polito.mad.koko.kokolab3.messaging;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import it.polito.mad.koko.kokolab3.firebase.DatabaseManager;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MessageManager {

    private static String TAG = "MessageManager";

    /*
     * All the messages of the current user
     */
    private static ArrayList<Message> userMessages;

    /*
     * All the chats' ID of the current user
     */
    private static Map<String,String> userChatIDs;

    /*
     * All the messages corresponding to a chat ID

    private static Map<String,ArrayList<Message>> chatsMessages;
    */

    /*
    * All the chats of the current user
    */

    private static ArrayList<Chat> userChats;

    /*
     * Listener to all the current user's chats ID
     */

    private static ValueEventListener userChatIDsListener;

    /*
     * Listener to all the current user's chats
     */
    private static ChildEventListener userChatsMessagesListener;


    /**
     * HTTP client
     */
    static OkHttpClient mClient = new OkHttpClient();

    /**
     * MediaType of the RequestBody.
     * If null, UTF-8 will be used.
     */
    private static final MediaType CONTENT_TYPE = null;

    /**
     * Book requests strings
     */
    public static final String
        // Message request target URL
        FCM_MESSAGE_URL = "https://fcm.googleapis.com/fcm/send",

        SENDER_USERNAME_PLACEHOLDER = "%SENDER_USER%",

        BOOK_EXCHANGE_MESSAGE_TITLE =   "Book exchange request from " +
                                        SENDER_USERNAME_PLACEHOLDER + "!",

        BOOK_EXCHANGE_MESSAGE_TEXT =    "You can accept/deny immediately or check " +
                                        SENDER_USERNAME_PLACEHOLDER +
                                        "'s profile by clicking this notification"
    ;

    /**
     * Firebase server's key access
     */
    private static String SERVER_KEY =
        "AAAAsT0hg7k:APA91bEfqnxkD9J_FkI1MBqo3NqBDgaYD1A1n9uRsrsR0HQScs1v4DddJ" +
        "KTsUh0muPmgHcgJFSjA-0zULkf-40Gurj4absEFz7AgKi_W6CRyVm2zQYIn3AcksIELpMuejGCb4QkgG4fD"
    ;
    private static String messageID;


    @SuppressLint("StaticFieldLeak")
    public static void sendMessage(final String recipient, final String title, final String body, final String message) {

        new AsyncTask<String, String, String>() {
            /**
             * AsyncTask for send message
             * @param params
             * @return result of sending message
             */
            @Override
            protected String doInBackground(String... params) {
                try {
                    //Create a JSON
                    JSONObject root = new JSONObject();
                    JSONObject notification = new JSONObject();
                    notification.put("body", body);
                    notification.put("title", title);

                    JSONObject data = new JSONObject();
                    data.put("message", message);
                    root.put("notification", notification);
                    root.put("data", data);
                    root.put("to", recipient);
                    // String that returns the result of HTTP request
                    String result = postToFCM(root.toString());
                    Log.d(TAG, "Result: " + result);
                    return result;
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                return null;
            }

            /**
             *  In this method we have the result of AsyncTasck
             * @param result of the AsyncTasck
             */

            @Override
            protected void onPostExecute(String result) {
                int success, failure = 0;
                try {
                    JSONObject resultJson = new JSONObject(result);
                    success = resultJson.getInt("success");
                    failure = resultJson.getInt("failure");
                    Log.d("MessageManager", "success is: " + String.valueOf(success));
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.d(TAG, "failure is: " + String.valueOf(failure));

                }
            }
        }.execute();
    }

    /**
     * Sending a notification to a specific user.
     * The JSON message has the following structure:
     * (Following the official Firebase JSON structure available on:
     *  https://firebase.google.com/docs/cloud-messaging/send-message#http_post_request)
     *
     * {    "notification": {
     *          "title": title,
     *          "body": body
     *      },
     *
     *      "priority": "high",
     *
     *      "to": receiver_token
     *
     *      "data": {
     *          "sender": {
     *              "id": "kE3ErSqw...",
     *              "username": sender_username,
     *              "image": "https://firebasestorage..."
     *          }
     *
     *          "receiver": {
     *              "id": "f3j1lw...",
     *              "username": receiver_username,
     *              "token: ,
     *              "image": "https://firebasestorage..."
     *          }
     *
     *          "book": {
     *              "title": book_title
     *          }
     *      }
     * }
     *
     * @param senderId
     * @param senderUsername
     * @param senderImage
     * @param receiverId
     * @param receiverUsername
     * @param receiverImage
     * @param bookTitle
     */
    public static void sendNotification(    // Sender info
                                            final String senderId,
                                            final String senderUsername,
                                            final String senderImage,

                                            // Receiver info
                                            final String receiverId,
                                            final String receiverUsername,
                                            final String receiverToken,
                                            final String receiverImage,

                                            // Book info
                                            final String bookTitle) {
        new AsyncTask<String, Void, String>() {
            @Override
            protected String doInBackground(String... params) {
                try {
                    // Notification
                    JSONObject notification = new JSONObject();
                    String notificationTitle = BOOK_EXCHANGE_MESSAGE_TITLE.replaceAll(SENDER_USERNAME_PLACEHOLDER, senderUsername);
                    String notificationText = BOOK_EXCHANGE_MESSAGE_TEXT.replaceAll(SENDER_USERNAME_PLACEHOLDER, senderUsername);
                    notification.put("title", notificationTitle);
                    notification.put("text", notificationText);

                    // Sender
                    JSONObject sender = new JSONObject();
                    sender.put("id", senderId);
                    sender.put("username", senderUsername);
                    sender.put("image", senderImage);

                    // Receiver
                    JSONObject receiver = new JSONObject();
                    receiver.put("id", receiverId);
                    receiver.put("username", receiverUsername);
                    receiver.put("image", receiverImage);

                    // Book
                    JSONObject book = new JSONObject();
                    book.put("title", bookTitle);

                    // Data
                    JSONObject data = new JSONObject();
                    data.put("sender", sender);
                    data.put("receiver", receiver);
                    data.put("book", book);

                    // Root
                    JSONObject root = new JSONObject();
                    root.put("notification", notification);
                    root.put("priority", "high");
                    root.put("to", receiverToken);
                    root.put("data", data);

                    // JSON pretty-printing
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    JsonParser jp = new JsonParser();
                    JsonElement je = jp.parse(root.toString());
                    String prettyJsonString = gson.toJson(je);
                    Log.d(TAG, "JSON message: " + prettyJsonString);

                    // Sending the JSON packet to FCM
                    String result = postToFCM(root.toString());

                    return result;
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                return null;
            }

            /**
             *  In this method we have the result of AsyncTasck in form of log
             * @param result of the AsyncTasck
             */

            @Override
            protected void onPostExecute(String result) {
                Log.d(TAG, result);

            }
        }.execute();
    }

    /**
     * create http request with OkHttpClient
     *
     * @param bodyString is Json with data
     * @return response of request
     * @throws IOException
     */
    private static String postToFCM(String bodyString) throws IOException {
        // Create a request body with json create in sendNotification or sendMessage
        RequestBody body = RequestBody.create(CONTENT_TYPE, bodyString);
        Log.d(TAG, "body: " + String.valueOf(body));
        // Create a http request
        Request request = new Request.Builder()
                .url(FCM_MESSAGE_URL)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "key=" + SERVER_KEY)
                .build();
        Log.d(TAG, "request: " + String.valueOf(request));
        // Use post http method to make a request
        Response response = mClient.newCall(request).execute();
        return response.body().string();
    }

    /*
     * Create the listener to populate the chat list with all the current user's chat from Firebase
     */

    public static void setUserChatsIDListener() {
        userChatIDs = new HashMap<>();
        userChatIDsListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    userChatIDs.putAll((Map<String, String>) dataSnapshot.getValue());
                    MessageManager.populateUserMessages();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
    }

    /**
     * Attach the listener to the chats of the current user
     */

    public static void populateUserChatsID() {
        DatabaseManager.get("users", FirebaseAuth.getInstance().getCurrentUser().getUid(), "chats").addValueEventListener(userChatIDsListener);

    }

    /**
     * set the listener to retrieve all the messages of a chat
     *
     * @param userChat chat class in which we put all the messages corresponding to the chatID
     */
    public static void setUserMessagesListener(Chat userChat){
        userChatsMessagesListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                if(dataSnapshot.exists()){

                    String datasnapshot = dataSnapshot.toString();
                    Log.d(TAG, datasnapshot);
                    Message message = new Message();
                    message.setSender((String) dataSnapshot.child("sender").getValue());
                    message.setText((String) dataSnapshot.child("text").getValue());
                    message.setTimestamp((String) dataSnapshot.child("timestamp").getValue());
                    Log.d(TAG, message.toString());

                    /**
                     * populate the Map with key: chatID and value:message
                     */
                    for(Chat chat:userChats)
                        if(chat.equals(userChat))
                            chat.getChatMessages().add(message);

                }

                Log.d(TAG,userChat.toString());

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
    }

    /**
     * For each chatID in the current user creates and attaches the Child listener to retrieve all the chat messages
     *
     * ArrayList<Chat>
     */
    public static void populateUserMessages(){

        userChats=new ArrayList<>();

        for(String chatID: userChatIDs.keySet()){

            userMessages=new ArrayList<>();
            Chat userChat=new Chat(chatID,userMessages);

            userChats.add(userChat);

            MessageManager.setUserMessagesListener(userChat);
            DatabaseManager.get("chats",chatID,"messages").addChildEventListener(userChatsMessagesListener);
        }
    }



    /**
     * It creates a chat entry in Firebase and a reference in both users involved.
     *
     * @param sender: sender side of the chat (current logged user)
     *                receiver: receiver side of the chat (value of "chatID")
     */
    public static void createChat(String sender, String receiver,String senderId, String receiverId, String imageSender, String imageReceiver, String textMessage) {


        DatabaseReference messagesRef = DatabaseManager.get("chats");

        String chatID = messagesRef.push().getKey();
        messagesRef.child(chatID).child("messages");

        DatabaseReference usersRefSender = DatabaseManager.get("users").child(senderId);
        usersRefSender.child("chats").child(chatID).child("receiver").setValue(receiver);
        usersRefSender.child("chats").child(chatID).child("receiverId").setValue(receiverId);
        usersRefSender.child("chats").child(chatID).child("imageReceiver").setValue(imageReceiver);

        DatabaseReference usersRefReceiver = DatabaseManager.get("users").child(receiverId);
        usersRefReceiver.child("chats").child(chatID).child("sender").setValue(sender);
        usersRefReceiver.child("chats").child(chatID).child("senderId").setValue(senderId);
        usersRefReceiver.child("chats").child(chatID).child("imageSender").setValue(imageSender);

        createMessage(chatID, sender, textMessage);

    }


    /**
     * Creates a message entry in Firebase
     *
     * @param chatID      id of the chat which the message belongs to
     * @param sender      id of the sender of the message
     * @param messageText content of the message
     */

    public static void createMessage(String chatID, String sender, String messageText) {

        DatabaseReference messagesRef = DatabaseManager.get("chats").child(chatID).child("messages");

        messageID = messagesRef.push().getKey();

        Message message = new Message();
        message.setSender(sender);
        message.setText(messageText);
        String timeStamp = new SimpleDateFormat("yyyy/MM/dd_HH:mm:ss").format(new Timestamp(System.currentTimeMillis()));
        message.setTimestamp(timeStamp);

        messagesRef.child(messageID).setValue(message);

    }

    /**
     * Return all user's messages from Firebase
     * @return
     */
    public static ArrayList<Chat> getUserChats(){

        return userChats;
    }

    /*
     * Return all user's chat from Firebase

    public static Map<String,ArrayList<Message>> getUserChats(){

        return chatsMessages;
    }*/

    /**
     * Return all user's chatID as Key and receiverID as Value
     */
    public static Map<String, String> getUserChatIDs() {
        return userChatIDs;
    }

    /**
     *
     * @return messageID when we create a new chat with notification
     */
    public static String getMessageID(){return messageID;}

}