package se.wibrick.demo;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;

import se.wibrick.sdk.ExtendedJSONObject;
import se.wibrick.sdk.StorageInterface;

/**
 * Created by Jonas on 2017-01-16.
 */

public class LocalProfile implements StorageInterface {

    private static final String TAG = "DEVELOP " + LocalProfile.class.getSimpleName();
    private static final String IDENTIFIER = "profile";
    private String profileID;
    private String profileBirthday;
    private String profileName;
    private String profileEmail;

    public LocalProfile() {
    }

    @Override
    public ExtendedJSONObject getJSONRepresentationOfData() {

        ExtendedJSONObject json = new ExtendedJSONObject();
        try {
            json.put("profileName", getProfileName());
            json.put("profileID", getProfileID());
            json.put("profileEmail", getProfileEmail());
            json.put("profileBirthday", getProfileBirthday());
        } catch (JSONException e) {
            Log.d(TAG, "JSONException: " + e.getMessage());
        }

        return json;
    }

    @Override
    public void setLocalDataFromJSON(Context context, ExtendedJSONObject jsonData) {

        this.setProfileName(jsonData.getString("profileName", ""));
        this.setProfileID(jsonData.getString("profileID", ""));
        this.setProfileEmail(jsonData.getString("profileEmail", ""));
        this.setProfileBirthday(jsonData.getString("profileBirthday", ""));

    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }

    private String getProfileBirthday() {
        return profileBirthday;
    }

    public void setProfileBirthday(String profileBirthday) {
        this.profileBirthday = profileBirthday;
    }

    private String getProfileName() {
        return profileName;
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    private String getProfileID() {
        return profileID;
    }

    public void setProfileID(String profileID) {
        this.profileID = profileID;
    }

    private String getProfileEmail() {
        return profileEmail;
    }

    public void setProfileEmail(String profileEmail) {
        this.profileEmail = profileEmail;
    }
}
