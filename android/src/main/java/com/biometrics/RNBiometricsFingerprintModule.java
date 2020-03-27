
package com.biometrics;

import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.hardware.biometrics.BiometricPrompt;

import android.os.Build;
import android.os.CancellationSignal;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import androidx.annotation.RequiresApi;
import androidx.core.hardware.fingerprint.FingerprintManagerCompat;
import android.util.Log;
import android.widget.Toast;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;


public class RNBiometricsFingerprintModule extends ReactContextBaseJavaModule {

    private final ReactApplicationContext reactContext;
    private Promise callback;
    private String KEY_NAME = "com.nextbillion.groww.android";
    private Cipher cipher;
    private KeyStore keyStore;
    private final String TAG = "BIOMETRICS";

    protected CancellationSignal mCancellationSignal = new CancellationSignal();
    protected androidx.core.os.CancellationSignal mCancellationSignalV23 = new androidx.core.os.CancellationSignal();
    private FingerprintManagerCompat.CryptoObject cryptoObjectV23;
    private BiometricPrompt.CryptoObject cryptoObjectV28;

    public RNBiometricsFingerprintModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;

    }

    // Handle Authentication In Version EQUAL OR HIGHER Than PIE
    @TargetApi(Build.VERSION_CODES.P)
    private void displayBiometricPrompt() {
        try {
            generateKey();
            if (initCipher()) {
                cryptoObjectV28 = new BiometricPrompt.CryptoObject(cipher);

            }
            BiometricPrompt biometricPrompt = new BiometricPrompt.Builder(this.reactContext)
                    .setTitle("Login to Groww")
                    .setSubtitle("")
                    .setDescription("Touch your finger on sensor to unlock")
                    .setNegativeButton("Cancel", this.reactContext.getMainExecutor(), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            WritableMap writableMap = new WritableNativeMap();
                            writableMap.putString("errorMessage", String.valueOf("Cancelled by user"));
                            writableMap.putString("errorCode", String.valueOf(10));
                            sendEvent(BiometricsUtils.ERROR_IN_AUTHENTICATION, writableMap);
                        }
                    }).build();
            biometricPrompt.authenticate(cryptoObjectV28, mCancellationSignal, this.reactContext.getMainExecutor(), new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
                    super.onAuthenticationHelp(helpCode, helpString);
                    Log.i("BIOMETRICS", "" + helpCode);
                    System.out.println(helpString);
                    try {
                        WritableMap writableMap = new WritableNativeMap();
                        writableMap.putString("errorMessage", String.valueOf(helpString));
                        writableMap.putString("errorCode", String.valueOf(helpCode));
                        sendEvent(BiometricsUtils.ERROR_IN_AUTHENTICATION, writableMap);
                    } catch (Exception e) {
                        Log.i(TAG, e.toString());
                    }
                }

                @Override
                public void onAuthenticationFailed() {
                    super.onAuthenticationFailed();
                    Log.i("BIOMETRICS", "failed");

                    try {
                        WritableMap writableMap = new WritableNativeMap();
                        sendEvent(BiometricsUtils.AUTHENTICATION_FAILED, writableMap);
                    } catch (Exception e) {
                        Log.i(TAG, e.toString());
                    }

                }

                @Override
                public void onAuthenticationError(int errorCode, CharSequence errString) {
                    super.onAuthenticationError(errorCode, errString);
                    Log.i("BIOMETRICS", "" + errorCode);
                    try {
                        WritableMap writableMap = new WritableNativeMap();
                        writableMap.putString("errorMessage", String.valueOf(errString));
                        writableMap.putString("errorCode", String.valueOf(errorCode));
                        sendEvent(BiometricsUtils.ERROR_IN_AUTHENTICATION, writableMap);
                    } catch (Exception e) {
                        Log.i(TAG, e.toString());
                    }
                }

                @Override
                public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                    super.onAuthenticationSucceeded(result);
                    Log.i("BIOMETRICS", "Succeeded");
                    reactContext.runOnUiQueueThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(reactContext, "Authentication Successful", Toast.LENGTH_LONG).show();
                        }
                    });

                    try {
                        WritableMap success = new WritableNativeMap();
                        String s = new String(result.getCryptoObject().getCipher().doFinal());
                        success.putString("uniqueCode", s);
                        sendEvent(BiometricsUtils.AUTHENTICATION_SUCCEEDED, success);
                        Log.i(TAG, "onAuthenticationSucceeded");
                    } catch (Exception e) {
                        Log.i(TAG, e.toString());
                    }

                }
            });
        } catch (Exception error) {

        }

    }

    // Handle Authentication In Version Lower Than PIE
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void displayFingerprintDialog() {
        try {
            generateKey();
            if (initCipher()) {
                mCancellationSignalV23 = new androidx.core.os.CancellationSignal();
                cryptoObjectV23 = new FingerprintManagerCompat.CryptoObject(cipher);
                FingerprintManagerCompat fingerprintManagerCompat = FingerprintManagerCompat.from(reactContext);

                fingerprintManagerCompat.authenticate(cryptoObjectV23, 0, mCancellationSignalV23,
                        new FingerprintManagerCompat.AuthenticationCallback() {
                            @Override
                            public void onAuthenticationError(int errMsgId, CharSequence errString) {
                                super.onAuthenticationError(errMsgId, errString);
                                System.out.println(String.valueOf(errString));
                                Log.i(TAG, "onAuthenticationError");
                                try {
                                    WritableMap writableMap = new WritableNativeMap();
                                    writableMap.putString("errorMessage", String.valueOf(errString));
                                    writableMap.putString("errorCode", String.valueOf(errMsgId));
                                    sendEvent(BiometricsUtils.ERROR_IN_AUTHENTICATION, writableMap);
                                } catch (Exception e) {
                                    Log.i(TAG, e.toString());
                                }
                            }

                            @Override
                            public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
                                super.onAuthenticationHelp(helpMsgId, helpString);
                                System.out.println(String.valueOf(helpString));
                                Log.i(TAG, "onAuthenticationHelp");
                                try {
                                    WritableMap writableMap = new WritableNativeMap();
                                    writableMap.putString("errorMessage", String.valueOf(helpString));
                                    writableMap.putString("errorCode", String.valueOf(helpMsgId));
                                    sendEvent(BiometricsUtils.ERROR_IN_AUTHENTICATION, writableMap);
                                } catch (Exception e) {
                                    Log.i(TAG, e.toString());
                                }
                            }

                            @Override
                            public void onAuthenticationSucceeded(FingerprintManagerCompat.AuthenticationResult result) {
                                super.onAuthenticationSucceeded(result);
                                System.out.println(String.valueOf(result));
                                try {
                                    WritableMap success = new WritableNativeMap();
//                                success.putString("uniqueCode", result.getCryptoObject().getCipher().doFinal());
                                    sendEvent(BiometricsUtils.AUTHENTICATION_SUCCEEDED, success);
                                    Log.i(TAG, "onAuthenticationSucceeded");
                                } catch (Exception e) {
                                    Log.i(TAG, e.toString());
                                }

                            }


                            @Override
                            public void onAuthenticationFailed() {
                                super.onAuthenticationFailed();
                                Log.i(TAG, "onAuthenticationFailed");
                                try {
                                    WritableMap writableMap = new WritableNativeMap();
                                    sendEvent(BiometricsUtils.AUTHENTICATION_FAILED, writableMap);
                                } catch (Exception e) {
                                    Log.i(TAG, e.toString());
                                }
                            }
                        }, null);

            }
        } catch (Exception e) {
            WritableMap writableMap = new WritableNativeMap();
            sendEvent(BiometricsUtils.AUTHENTICATION_FAILED, writableMap);
        }

    }

    public String getName() {
        return "RNBiometricsFingerprint";
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @ReactMethod
    public void invokeFingerprint(Promise promise) {
        callback = promise;
        if (BiometricsUtils.isHardwareSupported(this.reactContext)) {
            if (BiometricsUtils.isFingerprintAvailable(this.reactContext)) {
                if (BiometricsUtils.isBiometricPromptEnabled()) {
                    promise.resolve("success");
//                    displayBiometricPrompt();
                    displayFingerprintDialog();
                } else {
                    if (BiometricsUtils.isSdkVersionSupported()) {
                        promise.resolve(BiometricsUtils.OPEN_FINGERPRINT_DIALOG);
                        displayFingerprintDialog();
                    } else {
                        promise.reject("0", BiometricsUtils.FINGERPRINT_NOT_SUPPORTED);
                    }
                }
            } else {

                promise.reject("0", BiometricsUtils.FINGERPRINT_NOT_SUPPORTED);
            }
        } else {

            promise.reject("0", BiometricsUtils.FINGERPRINT_NOT_SUPPORTED);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @ReactMethod
    public void revokeFingerprint() {
        if (mCancellationSignalV23 != null && !mCancellationSignalV23.isCanceled()) {
            mCancellationSignalV23.cancel();
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    private void generateKey() {
        try {

            keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            KeyGenerator keyGenerator;
            keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            keyGenerator.init(new
                    KeyGenParameterSpec.Builder(KEY_NAME, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build());

            keyGenerator.generateKey();

        } catch (KeyStoreException
                | NoSuchAlgorithmException
                | NoSuchProviderException
                | InvalidAlgorithmParameterException
                | CertificateException
                | IOException exc) {
            exc.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean initCipher() {
        try {
            cipher = Cipher.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES + "/"
                            + KeyProperties.BLOCK_MODE_CBC + "/"
                            + KeyProperties.ENCRYPTION_PADDING_PKCS7);

        } catch (NoSuchAlgorithmException |
                NoSuchPaddingException e) {
            throw new RuntimeException("Failed to get Cipher", e);
        }

        try {
            keyStore.load(null);
            SecretKey key = (SecretKey) keyStore.getKey(KEY_NAME,
                    null);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return true;


        } catch (KeyPermanentlyInvalidatedException e) {
            return false;

        } catch (KeyStoreException | CertificateException
                | UnrecoverableKeyException | IOException
                | NoSuchAlgorithmException | InvalidKeyException e) {

            throw new RuntimeException("Failed to init Cipher", e);
        }
    }

    public void sendEvent(String eventName, WritableMap params) {
        reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
    }

}
