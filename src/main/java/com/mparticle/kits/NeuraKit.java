package com.mparticle.kits;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.neura.resources.authentication.AnonymousAuthenticateCallBack;
import com.neura.resources.authentication.AnonymousAuthenticateData;
import com.neura.resources.authentication.AnonymousAuthenticationStateListener;
import com.neura.sdk.object.AnonymousAuthenticationRequest;
import com.neura.standalonesdk.service.NeuraApiClient;
import com.neura.standalonesdk.util.SDKUtils;

import java.util.List;
import java.util.Map;

/**
 *
 * This is an mParticle kit, used to extend the functionality of mParticle SDK. Most Kits are wrappers/adapters
 * to a 3rd party SDK, primarily used to map analogous public mParticle APIs onto a 3rd-party API/platform.
 *
 *
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
public class NeuraKit extends KitIntegration {
    private static final String TAG = KitIntegration.class.getSimpleName();

    private NeuraApiClient mNeuraApiClient;

    @Override
    protected List<ReportingMessage> onKitCreate(Map<String, String> settings, Context context) {
        /** TODO: Initialize your SDK here
         * This method is analogous to Application#onCreate, and will be called once per app execution.
         *
         * If for some reason you can't start your SDK (such as settings are not present), you *must* throw an Exception
         *
         * If you forward any events on startup that are analagous to any mParticle messages types, return them here
         * as ReportingMessage objects. Otherwise, return null.
         */
        return null;
    }


    @Override
    public String getName() {
        //TODO: Replace this with your company name
        return "Neura";
    }



    @Override
    public List<ReportingMessage> setOptOut(boolean optedOut) {
        //TODO: Disable or enable your SDK when a user opts out.
        //TODO: If your SDK can not be opted out of, return null
        ReportingMessage optOutMessage = new ReportingMessage(this, ReportingMessage.MessageType.OPT_OUT, System.currentTimeMillis(), null);
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
                            Log.w(TAG, "getInstanceId failed", task.getException());
                            return;
                        }

                        // Get new Instance ID token
                        if (task.getResult() != null) {
                            String pushToken = task.getResult().getToken();

                            //Instantiate AnonymousAuthenticationRequest instance.
                            AnonymousAuthenticationRequest request = new AnonymousAuthenticationRequest(pushToken);

                            //Pass the AnonymousAuthenticationRequest instance and register a call back for success and failure events.
                            mNeuraApiClient.authenticate(request, new AnonymousAuthenticateCallBack() {
                                @Override
                                public void onSuccess(AnonymousAuthenticateData data) {
                                    mNeuraApiClient.registerAuthStateListener(silentStateListener);
                                    Log.i(TAG, "Successfully requested authentication with neura. ");
                                }

                                @Override
                                public void onFailure(int errorCode) {
                                    mNeuraApiClient.unregisterAuthStateListener();
                                    //Log.e(TAG, "Failed to authenticate with neura. " + "Reason : " + SDKUtils.errorCodeToString(errorCode));
                                }
                            });
                        } else {
                            Log.e(TAG, "Firebase task returned without result, cannot proceed with Authentication flow.");
                        }
                    }
                });


    }
    private static boolean isMinVersion() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
    }
}