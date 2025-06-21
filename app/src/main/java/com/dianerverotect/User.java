package com.dianerverotect;

import com.google.firebase.database.IgnoreExtraProperties;

/**
 * User model class for Firebase Database
 * Stores basic user information
 */
@IgnoreExtraProperties
public class User {
    private String fullName;
    private String email;
    private String profileImageUrl;
    
    // Empty constructor needed for Firebase
    public User() {
        // Default constructor required for Firebase
    }
    
    public User(String fullName, String email) {
        this.fullName = fullName;
        this.email = email;
        this.profileImageUrl = ""; // Default empty value
    }
    
    public User(String fullName, String email, String profileImageUrl) {
        this.fullName = fullName;
        this.email = email;
        this.profileImageUrl = profileImageUrl;
    }
    
    // Getters and setters required for Firebase
    public String getFullName() {
        return fullName;
    }
    
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getProfileImageUrl() {
        return profileImageUrl;
    }
    
    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }
}
