package com.mparticle.kits;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.neura.resources.authentication.AnonymousAuthenticateCallBack;
import com.neura.resources.authentication.AnonymousAuthenticateData;
import com.neura.resources.authentication.AnonymousAuthenticationStateListener;
import com.neura.resources.authentication.AuthenticationState;
import com.neura.sdk.object.AnonymousAuthenticationRequest;
import com.neura.standalonesdk.events.NeuraEvent;
import com.neura.standalonesdk.events.NeuraEventCallBack;
import com.neura.standalonesdk.events.NeuraPushCommandFactory;
import com.neura.standalonesdk.service.NeuraApiClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 *
 * This is an mParticle kit, used to extend the functionality of mParticle SDK. Most Kits are wrappers/adapters
 * to a 3rd party SDK, primarily used to map analogous public mParticle APIs onto a 3rd-party API/platform.
 *
 *sss
 * Follow the steps below to implement your kit:
 *
 *  - Edit ./build.gradle to add any necessary dependencies, such as your company's SDK
 *  - Rename this file/class, using your company name as the prefix, ie "AcmeKit"
 *  - View the javadocs to learn more about the KitIntegration class as well as the interfaces it defines.
 *  - Choose the additional interfaces that you need and have this class implement them,
 *    ie 'AcmeKit extends KitIntegration implements KitIntegration.PushListener'
 *
 *  In addition to this file, you also will need to edit:
 *  - ./build.gradle (as explained above)
 *  - ./README.md
 *  - ./src/main/AndroidManifest.xml
 *  - ./consumer-proguard.pro
 */
public class NeuraKit extends KitIntegration implements KitIntegration.PushListener{

    private NeuraApiClient mNeuraApiClient;

    @Override
    protected List<ReportingMessage> onKitCreate(Map<String, String> settings, Context context) {

        HashMap newMap = new HashMap(settings);
        String apiKey = (String) newMap.get("apiKey");
        String appSecret = (String) newMap.get("appSecret");
        try {
            FirebaseApp.initializeApp(context);
        } catch (IllegalStateException ex) {
            FirebaseApp.initializeApp(getContext(), Objects.requireNonNull(FirebaseOptions.fromResource(getContext())));
        }
        mNeuraApiClient = NeuraApiClient.getClient(context, apiKey, appSecret);
        authenticateAnonymously(new AnonymousAuthenticationStateListener() {
            @Override
            public void onStateChanged(AuthenticationState state) {
                switch (state) {
                    case AccessTokenRequested:
                        break;
                    case AuthenticatedAnonymously:
                        // successful authentication
                        mNeuraApiClient.unregisterAuthStateListener();
                        break;
                    case NotAuthenticated:
                    case FailedReceivingAccessToken:
                        // Authentication failed indefinitely. a good opportunity to retry the authentication flow
                        mNeuraApiClient.unregisterAuthStateListener();
                        break;
                    default:
                }
            }
        });
        return null;
    }


    @Override
    public String getName() {
        return "Neura";
    }


    @Override
    public List<ReportingMessage> setOptOut(boolean optedOut) {
        mNeuraApiClient.forgetMe(getCurrentActivity().get(), optedOut, new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                return true;
            }
        });
        return null;
    }

    public void authenticateAnonymously(final AnonymousAuthenticationStateListener silentStateListener) {
        if (!isMinVersion()) {
            return;
        }

        if (mNeuraApiClient.isLoggedIn()) {
            return;
        }

        //Get the FireBase Instance ID, we will use it to instantiate AnonymousAuthenticationRequest
        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override

                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            return;
                        }

                        // Get new Instance ID token
                        if (task.getResult() != null) {
                            String pushToken = task.getResult().getToken();
                            AnonymousAuthenticationRequest request = new AnonymousAuthenticationRequest(pushToken);
                            //Pass the AnonymousAuthenticationRequest instance and register a call back for success and failure events.
                            mNeuraApiClient.authenticate(request, new AnonymousAuthenticateCallBack() {
                                @Override
                                public void onSuccess(AnonymousAuthenticateData data) {
                                    mNeuraApiClient.registerAuthStateListener(silentStateListener);
                                }

                                @Override
                                public void onFailure(int errorCode) {
                                    mNeuraApiClient.unregisterAuthStateListener();
                                }
                            });
                        }
                    }
                });


    }
    private static boolean isMinVersion() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
    }

    @Override
    public boolean willHandlePushMessage(Intent intent) {
        return true;
    }

    @Override
    public void onPushMessageReceived(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        String dataString = (String) bundle.get("pushData");
        String typeString = (String) bundle.get("pushType");

        Map<String, String> map = new HashMap<>();
        map.put("pushData", dataString);
        map.put("pushType", typeString);
        NeuraPushCommandFactory pushCommand = NeuraPushCommandFactory.getInstance();
        final Context appContext = getContext();
        boolean isNeuraPush = pushCommand.isNeuraPush(appContext, map, new NeuraEventCallBack() {
            @Override
            public void neuraEventDetected(NeuraEvent event) {
            }
        });
    }

    @Override
    public boolean onPushRegistration(String s, String s1) {
        return true;
    }


}
