/* * Copyright (c) 2018 Samsung Electronics Co., Ltd. All rights reserved.  * Redistribution and use in source and binary forms, with or without modification, are permitted provided that  * the following conditions are met: *  *     * Redistributions of source code must retain the above copyright notice,  *       this list of conditions and the following disclaimer.  *     * Redistributions in binary form must reproduce the above copyright notice,  *       this list of conditions and the following disclaimer in the documentation and/or  *       other materials provided with the distribution.  *     * Neither the name of Samsung Electronics Co., Ltd. nor the names of its contributors may be used to endorse or  *       promote products derived from this software without specific prior written permission. *  * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE * POSSIBILITY OF SUCH DAMAGE. */package com.samsung.android.sdk.accessory.example.consumer;import java.io.IOException;import android.app.Notification;import android.app.NotificationChannel;import android.app.NotificationManager;import android.content.Context;import android.content.Intent;import android.os.Build;import android.os.Handler;import android.os.Binder;import android.os.IBinder;import android.widget.Toast;import android.util.Log;import com.samsung.android.sdk.SsdkUnsupportedException;import com.samsung.android.sdk.accessory.*;/** * Accessory SDK - Supporting Android O OS * <p> * According to "Google's official guides", there are background execution limits in Android O. * Activity Manager will kill background services in 5 seconds after started by starForegroundService(). * So, with below codes, you have to change background service to foreground service with notification. * You can change those codes to whatever your application needs. * <p> * If you don't need to keep the service in foreground over 5 seconds, * or if you build the project under Android SDK 26, you can erase below codes. * <p> * Example codes for startForeground() at SAAgent.onCreate(). * <code> *  if (Build.VERSION.SDK_INT >= 26) { *      NotificationManager notificationManager = null; *      String channel_id = "sample_channel_01"; *      if(notificationManager == null) { *          String channel_name = "Accessory_SDK_Sample"; *          notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE); *          NotificationChannel notiChannel = new NotificationChannel(channel_id, channel_name, NotificationManager.IMPORTANCE_LOW); *          notificationManager.createNotificationChannel(notiChannel); *      } * *      int notifyID = 1; *      Notification notification = new Notification.Builder(this.getBaseContext(),channel_id) *                                  .setContentTitle(TAG) *                                  .setContentText("") *                                  .setChannelId(channel_id) *                                  .build(); *      startForeground(notifyID, notification); *  } * </code> * <p> * Example codes for stopForeground() at SAAgent.onDestroy(). * <code> *  if (Build.VERSION.SDK_INT >= 26) { *      stopForeground(true); *  } * </code> */public class MessageConsumer extends SAAgent {    private static final String TAG = "MessageConsumer(C)";    Handler mHandler = new Handler();    private SAMessage mMessage = null;    private SAPeerAgent mSAPeerAgent = null;    private Toast mToast;    private final IBinder mBinder = new MessageConsumer.LocalBinder();    public MessageConsumer() {        super(TAG);    }    @Override    public void onCreate() {        super.onCreate();        /** Example codes for Android O OS (startForeground) **/        startRunningInForeground();        SA mAccessory = new SA();        try {            mAccessory.initialize(this);        } catch (SsdkUnsupportedException e) {            // try to handle SsdkUnsupportedException            if (processUnsupportedException(e) == true) {                return;            }        } catch (Exception e1) {            e1.printStackTrace();            /*             * Your application can not use Samsung Accessory SDK. Your application should work smoothly             * without using this SDK, or you may want to notify user and close your application gracefully             * (release resources, stop Service threads, close UI thread, etc.)             */            stopSelf();        }        mMessage = new SAMessage(this) {            @Override            protected void onSent(SAPeerAgent peerAgent, int id) {                Log.d(TAG, "onSent(), id: " + id + ", ToAgent: " + peerAgent.getPeerId());                String val = "" + id + " SUCCESS ";                displayToast("ACK Received: " + val, Toast.LENGTH_SHORT);            }            @Override            protected void onError(SAPeerAgent peerAgent, int id, int errorCode) {                Log.d(TAG, "onError(), id: " + id + ", ToAgent: " + peerAgent.getPeerId() + ", errorCode: " + errorCode);                String result = null;                switch (errorCode) {                    case ERROR_PEER_AGENT_UNREACHABLE:                        result = " FAILURE" + "[ " + errorCode + " ] : PEER_AGENT_UNREACHABLE ";                        break;                    case ERROR_PEER_AGENT_NO_RESPONSE:                        result = " FAILURE" + "[ " + errorCode + " ] : PEER_AGENT_NO_RESPONSE ";                        break;                    case ERROR_PEER_AGENT_NOT_SUPPORTED:                        result = " FAILURE" + "[ " + errorCode + " ] : ERROR_PEER_AGENT_NOT_SUPPORTED ";                        break;                    case ERROR_PEER_SERVICE_NOT_SUPPORTED:                        result = " FAILURE" + "[ " + errorCode + " ] : ERROR_PEER_SERVICE_NOT_SUPPORTED ";                        break;                    case ERROR_SERVICE_NOT_SUPPORTED:                        result = " FAILURE" + "[ " + errorCode + " ] : ERROR_SERVICE_NOT_SUPPORTED ";                        break;                    case ERROR_UNKNOWN:                        result = " FAILURE" + "[ " + errorCode + " ] : UNKNOWN ";                        break;                }                String val = "" + id + result;                displayToast("NAK Received: " + val, Toast.LENGTH_SHORT);                MessageActivity.updateButtonState(false);            }            @Override            protected void onReceive(SAPeerAgent peerAgent, byte[] message) {                String dataVal = new String(message);                addMessage("Received: ", dataVal);                MessageActivity.updateButtonState(false);            }        };    }    @Override    public void onDestroy() {        mSAPeerAgent = null;        /** Example codes for Android O OS (stopForeground) **/        stopRunningInForeground();        super.onDestroy();    }    @Override    public IBinder onBind(Intent intent) {        return mBinder;    }    @Override    protected void onFindPeerAgentsResponse(SAPeerAgent[] peerAgents, int result) {        if ((result == PEER_AGENT_FOUND) && (peerAgents != null)) {            updateTextView("PEERAGENT_FOUND");            displayToast("PEERAGENT_FOUND", Toast.LENGTH_LONG);            for (SAPeerAgent peerAgent : peerAgents) {                mSAPeerAgent = peerAgent;            }            return;        } else if (result == FINDPEER_DEVICE_NOT_CONNECTED) {            displayToast("FINDPEER_DEVICE_NOT_CONNECTED", Toast.LENGTH_LONG);        } else if (result == FINDPEER_SERVICE_NOT_FOUND) {            displayToast("FINDPEER_SERVICE_NOT_FOUND", Toast.LENGTH_LONG);        } else {            displayToast(R.string.NoPeersFound, Toast.LENGTH_LONG);        }        updateTextView("PEERAGENT_NOT_FOUND");    }    @Override    protected void onError(SAPeerAgent peerAgent, String errorMessage, int errorCode) {        super.onError(peerAgent, errorMessage, errorCode);    }    @Override    protected void onPeerAgentsUpdated(SAPeerAgent[] peerAgents, int result) {        final SAPeerAgent[] peers = peerAgents;        final int status = result;        mHandler.post(new Runnable() {            @Override            public void run() {                if (peers != null) {                    if (status == PEER_AGENT_AVAILABLE) {                        displayToast("PEER_AGENT_AVAILABLE", Toast.LENGTH_LONG);                    } else {                        displayToast("PEER_AGENT_UNAVAILABLE", Toast.LENGTH_LONG);                    }                }            }        });    }    public void startRunningInForeground() {        if (Build.VERSION.SDK_INT >= 26) {            NotificationManager notificationManager = null;            String channel_id = "sample_channel_01";            if(notificationManager == null) {                String channel_name = "Accessory_SDK_Sample";                notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);                NotificationChannel notiChannel = new NotificationChannel(channel_id, channel_name, NotificationManager.IMPORTANCE_LOW);                notificationManager.createNotificationChannel(notiChannel);            }            int notifyID = 1;            Notification notification = new Notification.Builder(this.getBaseContext(),channel_id)                    .setContentTitle(TAG)                    .setContentText("")                    .setChannelId(channel_id)                    .build();            startForeground(notifyID, notification);        }    }    public void stopRunningInForeground() {        if (Build.VERSION.SDK_INT >= 26) {            stopForeground(true);        }    }    public void findPeers() {        findPeerAgents();    }    public void sendData(final String message) {        if (mSAPeerAgent == null) {            displayToast("Try to find PeerAgent!", Toast.LENGTH_SHORT);            updateButtonState(false);        }        if (mMessage != null) {            new Thread(new Runnable() {                public void run() {                    try {                        int tid = mMessage.send(mSAPeerAgent, message.getBytes());                        addMessage("Sent: ", message + "(" + tid + ")");                        updateButtonState(true);                    } catch (IOException e) {                        e.printStackTrace();                        displayToast(e.getMessage(), Toast.LENGTH_SHORT);                        updateButtonState(false);                    } catch (IllegalArgumentException e) {                        e.printStackTrace();                        displayToast(e.getMessage(), Toast.LENGTH_SHORT);                        updateButtonState(false);                    }                }            }).start();        }    }    /**     * [Sending Data Securely]     *     * You can also send data more securely through SAMessage.secureSend().     * Data will be encrypted with a signed key if you use SAMessage.secureSend() instead of SAMessage.send().     *     * You can implement it as following codes.     *     * <code>        public int secureSendData(String message) {            int tid;            if (mSAPeerAgent == null) {                displayToast("Try to find PeerAgent!", Toast.LENGTH_SHORT);                return -1;            }            if (mMessage != null) {                try {                    tid = mMessage.secureSend(mSAPeerAgent, message.getBytes());                    addMessage("Sent: ", message);                    return tid;                } catch (IOException e) {                    e.printStackTrace();                    displayToast(e.getMessage(), Toast.LENGTH_SHORT);                    return -1;                } catch (IllegalArgumentException e) {                    e.printStackTrace();                    displayToast(e.getMessage(), Toast.LENGTH_SHORT);                    return -1;                }            }            return -1;        }     * </code>     */    public void clearToast() {        if (mToast != null) {            mToast.cancel();        }    }    private void displayToast(final int strId, final int duration) {        mHandler.post(new Runnable() {            @Override            public void run() {                if (mToast != null) {                    mToast.cancel();                }                mToast = Toast.makeText(getApplicationContext(), getResources().getString(strId), duration);                mToast.show();            }        });    }    private void displayToast(final String str, final int duration) {        mHandler.post(new Runnable() {            @Override            public void run() {                if (mToast != null) {                    mToast.cancel();                }                mToast = Toast.makeText(getApplicationContext(), str, duration);                mToast.show();            }        });    }    private void updateButtonState(final boolean state) {        mHandler.post(new Runnable() {            @Override            public void run() {                MessageActivity.updateButtonState(state);            }        });    }    private void updateTextView(final String str) {        mHandler.post(new Runnable() {            @Override            public void run() {                MessageActivity.updateTextView(str);            }        });    }    private void addMessage(final String prefix, final String data) {        final String strToUI = prefix.concat(data);        mHandler.post(new Runnable() {            @Override            public void run() {                MessageActivity.addMessage(strToUI);            }        });    }    private boolean processUnsupportedException(SsdkUnsupportedException e) {        e.printStackTrace();        int errType = e.getType();        if (errType == SsdkUnsupportedException.VENDOR_NOT_SUPPORTED                || errType == SsdkUnsupportedException.DEVICE_NOT_SUPPORTED) {            /*             * Your application can not use Samsung Accessory SDK. You application should work smoothly             * without using this SDK, or you may want to notify user and close your app gracefully (release             * resources, stop Service threads, close UI thread, etc.)             */            stopSelf();        } else if (errType == SsdkUnsupportedException.LIBRARY_NOT_INSTALLED) {            Log.e(TAG, "You need to install Samsung Accessory SDK to use this application.");        } else if (errType == SsdkUnsupportedException.LIBRARY_UPDATE_IS_REQUIRED) {            Log.e(TAG, "You need to update Samsung Accessory SDK to use this application.");        } else if (errType == SsdkUnsupportedException.LIBRARY_UPDATE_IS_RECOMMENDED) {            Log.e(TAG, "We recommend that you update your Samsung Accessory SDK before using this application.");            return false;        }        return true;    }    public class LocalBinder extends Binder {        public MessageConsumer getService() {            return MessageConsumer.this;        }    }}