package com.biometrics;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.hardware.fingerprint.FingerprintManagerCompat;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

public class BiometricsUtils {




        public static boolean isBiometricPromptEnabled() {
            return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P);
        }


        /*
         * Condition I: Check if the android version in device is greater than
         * Marshmallow, since fingerprint authentication is only supported
         * from Android 6.0.
         * Note: If your project's minSdkversion is 23 or higher,
         * then you won't need to perform this check.
         *
         * */
        public static boolean isSdkVersionSupported() {
            return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M);
        }



        /*
         * Condition II: Check if the device has fingerprint sensors.
         * Note: If you marked android.hardware.fingerprint as something that
         * your app requires (android:required="true"), then you don't need
         * to perform this check.
         *
         * */
        public static boolean isHardwareSupported(Context context) {
            FingerprintManagerCompat fingerprintManager = FingerprintManagerCompat.from(context);
            return fingerprintManager.isHardwareDetected();
        }



        /*
         * Condition III: Fingerprint authentication can be matched with a
         * registered fingerprint of the user. So we need to perform this check
         * in order to enable fingerprint authentication
         *
         * */
        public static boolean isFingerprintAvailable(Context context) {
            FingerprintManagerCompat fingerprintManager = FingerprintManagerCompat.from(context);
            return fingerprintManager.hasEnrolledFingerprints();
        }



        /*
         * Condition IV: Check if the permission has been added to
         * the app. This permission will be granted as soon as the user
         * installs the app on their device.
         *
         * */
        public static boolean isPermissionGranted(Context context) {
            return ActivityCompat.checkSelfPermission(context, Manifest.permission.USE_FINGERPRINT) ==
                    PackageManager.PERMISSION_GRANTED;
        }


    /*
     *Convert JSON TO WRITABLE NATIVE MAP
     *
     * */
    public static WritableMap jsonToWritableMap(JSONObject jsonObject) {
        WritableMap writableMap = new WritableNativeMap();
        try {
            Iterator iterator = jsonObject.keys();
            while(iterator.hasNext()) {
                String key = (String) iterator.next();
                Object value = jsonObject.get(key);
                if (value instanceof Float || value instanceof Double) {
                    writableMap.putDouble(key, jsonObject.getDouble(key));
                } else if (value instanceof Number) {
                    writableMap.putInt(key, jsonObject.getInt(key));
                } else if (value instanceof String) {
                    writableMap.putString(key, jsonObject.getString(key));
                } else if (value instanceof JSONObject) {
                    writableMap.putMap(key, jsonToWritableMap(jsonObject.getJSONObject(key)));
                } else if (value instanceof JSONArray){
                    writableMap.putArray(key, jsonToWritableArray(jsonObject.getJSONArray(key)));
                } else if (value == JSONObject.NULL){
                    writableMap.putNull(key);
                }
            }
        } catch(JSONException e){
            // Fail silently
        }
        return writableMap;
    }

    /*
     *Convert JSON TO WRITABLE NATIVE ARRAY.
     *
     * */

    public static WritableArray jsonToWritableArray(JSONArray jsonArray) {
        WritableArray writableArray = new WritableNativeArray();
        try {
            for(int i=0; i < jsonArray.length(); i++) {
                Object value = jsonArray.get(i);
                if (value instanceof Float || value instanceof Double) {
                    writableArray.pushDouble(jsonArray.getDouble(i));
                } else if (value instanceof Number) {
                    writableArray.pushInt(jsonArray.getInt(i));
                } else if (value instanceof String) {
                    writableArray.pushString(jsonArray.getString(i));
                } else if (value instanceof JSONObject) {
                    writableArray.pushMap(jsonToWritableMap(jsonArray.getJSONObject(i)));
                } else if (value instanceof JSONArray){
                    writableArray.pushArray(jsonToWritableArray(jsonArray.getJSONArray(i)));
                } else if (value == JSONObject.NULL){
                    writableArray.pushNull();
                }
            }
        } catch(JSONException e){
            // Fail silently
        }

        return writableArray;
    }

    public static String FINGERPRINT_NOT_SUPPORTED="FINGERPRINT_NOT_SUPPORTED";
    public static String ERROR_IN_AUTHENTICATION="ERROR_IN_AUTHENTICATION";
    public static String AUTHENTICATION_SUCCEEDED="AUTHENTICATION_SUCCEEDED";
    public static String OPEN_FINGERPRINT_DIALOG="OPEN_FINGERPRINT_DIALOG";
    public static String AUTHENTICATION_FAILED="AUTHENTICATION_FAILED";
    public static String AUTHENTICATION_CANCELLED_BY_USER="AUTHENTICATION_CANCELLED_BY_USER";



}
