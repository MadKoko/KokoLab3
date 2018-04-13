package it.polito.mad.koko.kokolab2.profile;

import android.support.annotation.NonNull;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;

public class Profile {

    private String  name,
                    email,
                    phone,
                    location,
                    bio;

    /**
     * Profile profile data is stored in a firebase database.
     */
    private DatabaseReference mDatabase;
    private FirebaseUser mFirebaseUser;

    /**
     *  class that implement Profile
     * @param mDatabase firebase database
     * @param mFirebaseUser firebase user information
     */
    public Profile(DatabaseReference mDatabase, FirebaseUser mFirebaseUser) {
        this.mDatabase = mDatabase;
        this.mFirebaseUser = mFirebaseUser;
    }

    public Profile(String name, String email, String phone, String location, String bio) {
        this.name=name;
        this.email=email;
        this.phone=phone;
        this.location=location;
        this.bio=bio;
    }

    /**
     *
     * @param name of user
     */
    public void setName(String name) {
        this.name = name;
        mDatabase.child("users").child(mFirebaseUser.getUid()).child("name").setValue(name);

    }

    /**
     *
     * @param email of user
     */
    public void setEmail(String email) {
        this.email = email;
        mDatabase.child("users").child(mFirebaseUser.getUid()).child("email").setValue(email);
    }

    /**
     *
     * @param phone of user
     */

    public void setPhone(String phone) {
        this.phone = phone;
        mDatabase.child("users").child(mFirebaseUser.getUid()).child("phone").setValue(phone);
    }

    /**
     *
     * @param location of user
     */
    public void setLocation(String location) {
        this.location = location;
        mDatabase.child("users").child(mFirebaseUser.getUid()).child("location").setValue(location);
    }

    /**
     *
     * @param bio of user
     */
    public void setBio(String bio) {
        this.bio = bio;
        mDatabase.child("users").child(mFirebaseUser.getUid()).child("bio").setValue(bio);
    }

}
