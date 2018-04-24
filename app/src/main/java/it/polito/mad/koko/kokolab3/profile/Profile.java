package it.polito.mad.koko.kokolab3.profile;

import java.io.Serializable;

public class Profile implements Serializable {

    private String  name,
                    email,
                    phone,
                    location,
                    bio,imgUrl;

    public Profile() {
    }


    public Profile(String name, String email, String phone, String location, String bio, String imgUrl) {
        this.name=name;
        this.email=email;
        this.phone=phone;
        this.location=location;
        this.bio=bio;
        this.imgUrl=imgUrl;
    }

    public Profile(String name, String email) {
        this.name=name;
        this.email=email;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getLocation() {
        return location;
    }

    public String getBio() {
        return bio;
    }

    public String getImgUrl() {
        return imgUrl;
    }

    public synchronized void setName(String name) {
        this.name = name;
    }

    public synchronized void setEmail(String email) {
        this.email = email;
    }

    public synchronized void setPhone(String phone) {
        this.phone = phone;
    }

    public synchronized void setLocation(String location) {
        this.location = location;
    }

    public synchronized void setBio(String bio) {
        this.bio = bio;
    }

    public synchronized void setImgUrl(String imgUrl) {
        this.imgUrl = imgUrl;
    }

    @Override
    public String toString() {
        return "Profile{" +
                "name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", location='" + location + '\'' +
                ", bio='" + bio + '\'' +
                ", imgUrl='" + imgUrl + '\'' +
                '}';
    }
}