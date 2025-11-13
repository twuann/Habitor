package com.example.habitor.model;

public class User {
    public String name;
    public int age;
    public String gender;
    public String profilePhotoUri;

    public User(String name, int age, String gender, String profilePhotoUri) {
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.profilePhotoUri = profilePhotoUri;
    }
}
