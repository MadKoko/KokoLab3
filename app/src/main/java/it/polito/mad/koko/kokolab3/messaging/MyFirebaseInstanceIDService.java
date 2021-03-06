package it.polito.mad.koko.kokolab3.messaging;

import android.util.Log;

import com.firebase.ui.auth.data.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.OnDisconnect;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import it.polito.mad.koko.kokolab3.books.Book;
import it.polito.mad.koko.kokolab3.firebase.DatabaseManager;
import it.polito.mad.koko.kokolab3.firebase.OnGetDataListener;
import it.polito.mad.koko.kokolab3.profile.Profile;
import it.polito.mad.koko.kokolab3.profile.ProfileManager;


public class MyFirebaseInstanceIDService extends FirebaseInstanceIdService {

    private static final String TAG = "MyFirebaseIIDService";

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the InstanceID token
     * is initially generated so this is where you would retrieve the token.
     */
    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG, "Refreshed token: " + refreshedToken);

        // If the current user has performed the login operation
        if(ProfileManager.hasLoggedIn()) {
            // Update the token field on Firebase
            DatabaseManager.set(refreshedToken, "users/" + ProfileManager.getCurrentUserID() + "/tokenMessage");

            /*  For each chat opened with another user, the second party's UserChatInfo
                object must be updated with the new current user's token */
            ProfileManager.getCurrentUserReference().child("chats").addListenerForSingleValueEvent(
                    new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if(dataSnapshot.exists()) {
                                // Retrieving all current user's chats
                                Map<String, UserChatInfo> currentUserChats = new HashMap<>();
                                for(DataSnapshot currentUserChat : dataSnapshot.getChildren())
                                    currentUserChats.put(
                                        currentUserChat.getKey(),
                                        currentUserChat.getValue(UserChatInfo.class)
                                    );

                                // If the current user has opened at least one chat
                                if(!currentUserChats.isEmpty()) {
                                    // For each current user's chat
                                    for(Map.Entry<String, UserChatInfo> userChat : currentUserChats.entrySet()) {
                                        // Update the current user's token in the other UserChatInfo object
                                        DatabaseManager.set(
                                            refreshedToken,
                                            "users/"
                                                + userChat.getValue().getSecondPartyId()
                                                + "/chats/"
                                                + userChat.getKey()
                                                + "/secondPartyToken"
                                        );
                                    }
                                }
                            }
                        }

                        @Override public void onCancelled(DatabaseError databaseError) {}
                    }
            );

            ProfileManager.getBooks(new OnGetDataListener() {
                @Override
                public void onSuccess(DataSnapshot data) {
                    if(data.exists()) {
                        // For each current user's book ID
                        for(String bookId : ((Map<String, Book>) data.getValue()).keySet())
                            DatabaseManager.set(
                                refreshedToken,
                                "books/" + bookId + "/bookOwner/tokenMessage"
                            );
                    }
                }

                @Override public void onFailed(DatabaseError databaseError) {}
            });
        }
    }
}