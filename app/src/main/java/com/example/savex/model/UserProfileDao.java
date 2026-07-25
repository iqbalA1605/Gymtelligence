package com.example.savex.model;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface UserProfileDao {

    @Insert
    long insert(UserProfile userProfile);

    @Query("SELECT * FROM user_profile ORDER BY id DESC LIMIT 1")
    UserProfile getLatestProfileSync();

    @Query("SELECT * FROM user_profile ORDER BY id DESC LIMIT 1")
    LiveData<UserProfile> getLatestProfileLive();
}