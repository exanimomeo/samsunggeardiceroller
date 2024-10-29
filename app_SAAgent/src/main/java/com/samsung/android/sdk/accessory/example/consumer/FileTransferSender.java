/*    
 * Copyright (c) 2018 Samsung Electronics Co., Ltd. All rights reserved.
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that 
 * the following conditions are met:
 * 
 *     * Redistributions of source code must retain the above copyright notice, 
 *       this list of conditions and the following disclaimer. 
 *     * Redistributions in binary form must reproduce the above copyright notice, 
 *       this list of conditions and the following disclaimer in the documentation and/or 
 *       other materials provided with the distribution. 
 *     * Neither the name of Samsung Electronics Co., Ltd. nor the names of its contributors may be used to endorse
 *       or promote products derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.samsung.android.sdk.accessory.example.consumer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.samsung.android.sdk.SsdkUnsupportedException;
import com.samsung.android.sdk.accessory.SAAgent;
import com.samsung.android.sdk.accessory.SAPeerAgent;
import com.samsung.android.sdk.accessory.SASocket;
import com.samsung.android.sdk.accessoryfiletransfer.SAFileTransfer;
import com.samsung.android.sdk.accessoryfiletransfer.SAFileTransfer.EventListener;
import com.samsung.android.sdk.accessoryfiletransfer.SAft;

/**
 * Accessory SDK - Supporting Android O OS
 *
 * According to "Google's official guides", there are background execution limits in Android O.
 * Activity Manager will kill background services in 5 seconds after started by starForegroundService().
 * So, with below codes, you have to change background service to foreground service with notification.
 * You can change those codes to whatever your application needs.
 *
 * If you don't need to keep the service in foreground over 5 seconds,
 * or if you build the project under Android SDK 26, you can erase below codes.
 *
 * Example codes for startForeground() at SAAgent.onCreate().
 * <code>
 *  if (Build.VERSION.SDK_INT >= 26) {
 *      NotificationManager notificationManager = null;
 *      String channel_id = "sample_channel_01";
 *      if(notificationManager == null) {
 *          String channel_name = "Accessory_SDK_Sample";
 *          notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
 *          NotificationChannel notiChannel = new NotificationChannel(channel_id, channel_name, NotificationManager.IMPORTANCE_LOW);
 *          notificationManager.createNotificationChannel(notiChannel);
 *      }
 *
 *      int notifyID = 1;
 *      Notification notification = new Notification.Builder(this.getBaseContext(),channel_id)
 *              .setContentTitle(TAG)
 *              .setContentText("")
 *              .setChannelId(channel_id)
 *              .build();
 *      startForeground(notifyID, notification);
 *  }
 * </code>
 *
 * Example codes for stopForeground() at SAAgent.onDestroy().
 * <code>
 *  if (Build.VERSION.SDK_INT >= 26) {
 *      stopForeground(true);
 *  }
 * </code>
 *
 */

public class FileTransferSender extends SAAgent {
    private static final String TAG = "FileTransferSender(C)";
    private static final Class<ServiceConnection> SASOCKET_CLASS = ServiceConnection.class;
    private int trId = -1;
    private int errCode = SAFileTransfer.ERROR_NONE;
    private SAPeerAgent mPeerAgent = null;
    private SAFileTransfer mSAFileTransfer = null;
    private EventListener mCallback = null;
    private FileAction mFileAction = null;

    private final IBinder mSenderBinder = new SenderBinder();

    public FileTransferSender() {
        super(TAG, SASOCKET_CLASS);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "On Create of Sample FileTransferSender Service");

        /** Example codes for Android O OS (startForeground) **/
        startRunningInForeground();

        mCallback = new EventListener() {
            @Override
            public void onProgressChanged(int transId, int progress) {
                Log.d(TAG, "onProgressChanged : " + progress + " for transaction : " + transId);
                if (mFileAction != null) {
                    mFileAction.onFileActionProgress(progress);
                }
            }

            @Override
            public void onTransferCompleted(int transId, String fileName, int errorCode) {
                errCode = errorCode;
                Log.d(TAG, "onTransferCompleted: tr id : " + transId + " file name : " + fileName + " error : "
                            + errorCode);
                if (errorCode == SAFileTransfer.ERROR_NONE) {
                    mFileAction.onFileActionTransferComplete();
                } else {
                    mFileAction.onFileActionError("Error");
                }
            }

            @Override
            public void onTransferRequested(int id, String fileName) {
                // No use at sender side
            }

            @Override
            public void onCancelAllCompleted(int errorCode) {
                if (errorCode == SAFileTransfer.ERROR_NONE) {
                    mFileAction.onFileActionCancelAllComplete();
                }
                else if (errorCode == SAFileTransfer.ERROR_TRANSACTION_NOT_FOUND) {
                    Toast.makeText(getBaseContext(), "onCancelAllCompleted : ERROR_TRANSACTION_NOT_FOUND.", Toast.LENGTH_SHORT).show();
                }
                else if (errorCode == SAFileTransfer.ERROR_NOT_SUPPORTED) {
                    Toast.makeText(getBaseContext(), "onCancelAllCompleted : ERROR_NOT_SUPPORTED.", Toast.LENGTH_SHORT).show();
                }
                Log.e(TAG, "onCancelAllCompleted: Error Code " + errorCode);
            }
        };
        SAft saft = new SAft();
        try {
            saft.initialize(this);
        } catch (SsdkUnsupportedException e) {
            if (e.getType() == SsdkUnsupportedException.DEVICE_NOT_SUPPORTED) {
                Toast.makeText(getBaseContext(), "Cannot initialize, DEVICE_NOT_SUPPORTED", Toast.LENGTH_SHORT).show();
            } else if (e.getType() == SsdkUnsupportedException.LIBRARY_NOT_INSTALLED) {
                Toast.makeText(getBaseContext(), "Cannot initialize, LIBRARY_NOT_INSTALLED.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getBaseContext(), "Cannot initialize, UNKNOWN.", Toast.LENGTH_SHORT).show();
            }
            e.printStackTrace();
            return;
        } catch (Exception e1) {
            Toast.makeText(getBaseContext(), "Cannot initialize, SAft.", Toast.LENGTH_SHORT).show();
            e1.printStackTrace();
            return;
        }
        mSAFileTransfer = new SAFileTransfer(FileTransferSender.this, mCallback);
    }

    @Override
    public void onDestroy() {
        try {
            mSAFileTransfer.close();
            mSAFileTransfer = null;
        } catch (RuntimeException e) {
            Log.e(TAG, e.getMessage());
        }
        if(mPeerAgent != null)
        {
            Log.e(TAG, "onDestroy mPeerAgent not null" );
            mPeerAgent = null;
            Toast.makeText(getBaseContext(), "Service Terminated", Toast.LENGTH_SHORT).show();
        }
        /** Example codes for Android O OS (stopForeground) **/
        stopRunningInForeground();



        Log.i(TAG, "FileTransferSender Service is Stopped.");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return mSenderBinder;
    }

    @Override
    protected void onFindPeerAgentsResponse(SAPeerAgent[] peerAgents, int result) {
        if (peerAgents != null) {
            for(SAPeerAgent peerAgent : peerAgents)
                mPeerAgent = peerAgent;

            requestServiceConnection(mPeerAgent);
        } else {
            Log.e(TAG, "No peer Agent found:" + result);
            Toast.makeText(getBaseContext(), "No peer agent found.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onPeerAgentsUpdated(SAPeerAgent[] peerAgents, int result) {
        Log.d(TAG, "Peer agent updated- result: "+ result + " trId: "+ trId);
        for(SAPeerAgent peerAgent : peerAgents)
            mPeerAgent = peerAgent;
        if (result == SAAgent.PEER_AGENT_UNAVAILABLE) {
            if (errCode != SAFileTransfer.ERROR_CONNECTION_LOST) {
                try {
                    cancelFileTransfer(trId);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                    Toast.makeText(getBaseContext(), "IllegalArgumentException", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    protected void onServiceConnectionResponse(SAPeerAgent peerAgent, SASocket socket, int result) {
        Log.i(TAG, "onServiceConnectionResponse: result - " + result);
        if (socket == null) {
            if (result == SAAgent.CONNECTION_ALREADY_EXIST) {
                Toast.makeText(getBaseContext(), "CONNECTION_ALREADY_EXIST", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getBaseContext(), "Connection could not be made. Please try again", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getBaseContext(), "Connection established for FT", Toast.LENGTH_SHORT).show();
        }
    }

    public void startRunningInForeground() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager notificationManager = null;
            String channel_id = "sample_channel_01";

            if(notificationManager == null) {
                String channel_name = "Accessory_SDK_Sample";
                notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                NotificationChannel notiChannel = new NotificationChannel(channel_id, channel_name, NotificationManager.IMPORTANCE_LOW);
                notificationManager.createNotificationChannel(notiChannel);
            }

            int notifyID = 1;
            Notification notification = new Notification.Builder(this.getBaseContext(),channel_id)
                    .setContentTitle(TAG)
                    .setContentText("")
                    .setChannelId(channel_id)
                    .build();

            startForeground(notifyID, notification);
        }
    }

    public void stopRunningInForeground() {
        if (Build.VERSION.SDK_INT >= 26) {
            stopForeground(true);
        }
    }

    public void connect() {
        if (mPeerAgent != null) {
            requestServiceConnection(mPeerAgent);
        } else {
            super.findPeerAgents();
        }
    }

    public int sendFile(String mSelectedFileName) {
        if (mSAFileTransfer != null && mPeerAgent != null) {
            trId = mSAFileTransfer.send(mPeerAgent, mSelectedFileName);
            return trId;
        } else {
            Toast.makeText(getBaseContext(), "Peer could not be found. Try again.", Toast.LENGTH_SHORT).show();
            findPeerAgents();
            return -1;
        }
    }

    public void cancelFileTransfer(int transId) {
        if (mSAFileTransfer != null) {
            mSAFileTransfer.cancel(transId);
        }
    }

    public void cancelAllTransactions()
    {
        if (mSAFileTransfer != null) {
            mSAFileTransfer.cancelAll();
        }
    }

    public void registerFileAction(FileAction action) {
        this.mFileAction = action;
    }

    public class ServiceConnection extends SASocket {
        public ServiceConnection() {
            super(ServiceConnection.class.getName());
        }

        @Override
        protected void onServiceConnectionLost(int reason) {
            Log.e(TAG, "onServiceConnectionLost: reason-" + reason+ mSAFileTransfer);
           if(mPeerAgent != null) {
               if (mSAFileTransfer != null) {
                   mFileAction.onFileActionError("Service Terminated");
               } else {
                   Toast.makeText(getApplicationContext(), "Service Terminated", Toast.LENGTH_SHORT).show();
               }
           }
            mPeerAgent = null;
        }

        @Override
        public void onReceive(int channelId, byte[] data) {
        }

        @Override
        public void onError(int channelId, String errorMessage, int errorCode) {
        }
    }

    public interface FileAction {
        void onFileActionError(String message);

        void onFileActionProgress(long progress);

        void onFileActionTransferComplete();

        void onFileActionCancelAllComplete();
    }

    public class SenderBinder extends Binder {
        public FileTransferSender getService() {
            return FileTransferSender.this;
        }
    }
}
