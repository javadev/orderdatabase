package com.github.javadev.orderdatabase;

import com.firebase.client.AuthData;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.Query;
import com.firebase.client.ValueEventListener;
import com.firebase.security.token.TokenGenerator;
import com.github.underscore.lodash.$;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FirebaseService {
    private final String appName;
    private final String token;

    static {
        Firebase.getDefaultConfig().setLogLevel(com.firebase.client.Logger.Level.ERROR);
    }

    public FirebaseService(String appName, String token) {
        this.appName = !$.isString(appName) || appName.trim().isEmpty()
                ? "amber-torch-6986" : appName;
        this.token = !$.isString(token) || token.trim().isEmpty()
                ? "DJYf0UDidCrwtFRjZgYS4qXJvmVeUownDTzqVNWR" : token;
    }

    public List<Map<String, Object>> readAll() {
        final List<Map<String, Object>> result = new ArrayList<>();
        final Firebase myFirebaseRef = new Firebase($.join(Arrays.asList("https://", appName, ".firebaseio.com/"), ""));
        final Semaphore semaphore = new Semaphore(0);
        Map<String, Object> authPayload = new HashMap<>();
        authPayload.put("uid", "1");
        authPayload.put("provider", "jvm");
        TokenGenerator tokenGenerator = new TokenGenerator(token);
        String tokenGenerated = tokenGenerator.createToken(authPayload);

        myFirebaseRef.authWithCustomToken(tokenGenerated, new Firebase.AuthResultHandler() {
            @Override
            public void onAuthenticated(AuthData ad) {
                Firebase orderdataRef = myFirebaseRef.child("orderdata");
                orderdataRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        try {
                            for (DataSnapshot data : snapshot.getChildren()) {
                                if (data.getValue() instanceof Map) {
                                    result.add((Map<String, Object>) data.getValue());
                                }
                                if (data.getValue() instanceof List) {
                                    result.addAll((List<Map<String, Object>>) data.getValue());
                                }
                            }
                        } catch (ClassCastException ex) {
                        }
                        myFirebaseRef.unauth();
                        semaphore.release();
                    }
                    @Override
                    public void onCancelled(FirebaseError fe) {
                        semaphore.release();
                    }
                });
            }
            @Override
            public void onAuthenticationError(FirebaseError fe) {
                semaphore.release();
                Logger.getLogger(DatabaseService.class.getName()).log(Level.SEVERE, "Login error", fe);
            }
        });
        try {
            semaphore.acquire();
        } catch (InterruptedException ex) {
            Logger.getLogger(FirebaseService.class.getName()).log(Level.SEVERE, null, ex);
        }
        return result;
    }
    
    public void insertData(final List<Map<String, Object>> dataList) {
        final Firebase myFirebaseRef = new Firebase($.join(Arrays.asList("https://", appName, ".firebaseio.com/"), ""));
        Map<String, Object> authPayload = new HashMap<>();
        authPayload.put("uid", "1");
        authPayload.put("provider", "jvm");
        TokenGenerator tokenGenerator = new TokenGenerator(token);
        String tokenGenerated = tokenGenerator.createToken(authPayload);
        myFirebaseRef.authWithCustomToken(tokenGenerated, new Firebase.AuthResultHandler() {
            @Override
            public void onAuthenticated(AuthData ad) {
                Firebase orderdataRef = myFirebaseRef.child("orderdata");
                orderdataRef.push().setValue(dataList);
                myFirebaseRef.unauth();
            }
            @Override
            public void onAuthenticationError(FirebaseError fe) {
                Logger.getLogger(DatabaseService.class.getName()).log(Level.SEVERE, "Login error", fe);
            }
        });
    }
}
