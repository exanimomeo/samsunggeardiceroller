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

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.samsung.android.sdk.SsdkUnsupportedException;
import com.samsung.android.sdk.accessory.SAAgentV2;
import com.samsung.android.sdk.accessory.SAPeerAgent;
import com.samsung.android.sdk.accessory.SASocket;
import com.samsung.android.sdk.accessoryfiletransfer.SAFileTransfer;
import com.samsung.android.sdk.accessoryfiletransfer.SAFileTransfer.EventListener;
import com.samsung.android.sdk.accessoryfiletransfer.SAft;

public class FileTransferSender extends SAAgentV2 {
    private static final String TAG = "FileTransferSender(C)";
    private static final Class<ServiceConnection> SASOCKET_CLASS = ServiceConnection.class;
    private int trId = -1;
    private int errCode = SAFileTransfer.ERROR_NONE;
    private SAPeerAgent mPeerAgent = null;
    private SAFileTransfer mSAFileTransfer = null;
    private EventListener mCallback = null;
    private FileAction mFileAction = null;
    private Context mContext = null;

    public FileTransferSender(Context context) {
        super(TAG, context, SASOCKET_CLASS);
        mContext = context;
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
                    Toast.makeText(mContext, "onCancelAllCompleted : ERROR_TRANSACTION_NOT_FOUND.", Toast.LENGTH_SHORT).show();
                }
                else if (errorCode == SAFileTransfer.ERROR_NOT_SUPPORTED) {
                    Toast.makeText(mContext, "onCancelAllCompleted : ERROR_NOT_SUPPORTED.", Toast.LENGTH_SHORT).show();
                }
                Log.e(TAG, "onCancelAllCompleted: Error Code " + errorCode);
            }
        };
        SAft saft = new SAft();
        try {
            saft.initialize(mContext);
        } catch (SsdkUnsupportedException e) {
            if (e.getType() == SsdkUnsupportedException.DEVICE_NOT_SUPPORTED) {
                Toast.makeText(mContext, "Cannot initialize, DEVICE_NOT_SUPPORTED", Toast.LENGTH_SHORT).show();
            } else if (e.getType() == SsdkUnsupportedException.LIBRARY_NOT_INSTALLED) {
                Toast.makeText(mContext, "Cannot initialize, LIBRARY_NOT_INSTALLED.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(mContext, "Cannot initialize, UNKNOWN.", Toast.LENGTH_SHORT).show();
            }
            e.printStackTrace();
            return;
        } catch (Exception e1) {
            Toast.makeText(mContext, "Cannot initialize, SAft.", Toast.LENGTH_SHORT).show();
            e1.printStackTrace();
            return;
        }
        mSAFileTransfer = new SAFileTransfer(FileTransferSender.this, mCallback);
    }

    @Override
    protected void onFindPeerAgentsResponse(SAPeerAgent[] peerAgents, int result) {
        if (peerAgents != null) {
            for(SAPeerAgent peerAgent : peerAgents)
                mPeerAgent = peerAgent;

            requestServiceConnection(mPeerAgent);
        } else {
            Log.e(TAG, "No peer Agent found:" + result);
            Toast.makeText(mContext, "No peer agent found.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onPeerAgentsUpdated(SAPeerAgent[] peerAgents, int result) {
        Log.d(TAG, "Peer agent updated- result: "+ result + " trId: "+ trId);
        for(SAPeerAgent peerAgent : peerAgents)
            mPeerAgent = peerAgent;
        if (result == SAAgentV2.PEER_AGENT_UNAVAILABLE) {
            if (errCode != SAFileTransfer.ERROR_CONNECTION_LOST) {
                try {
                    cancelFileTransfer(trId);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                    Toast.makeText(mContext, "IllegalArgumentException", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    protected void onServiceConnectionResponse(SAPeerAgent peerAgent, SASocket socket, int result) {
        Log.i(TAG, "onServiceConnectionResponse: result - " + result);
        if (socket == null) {
            if (result == SAAgentV2.CONNECTION_ALREADY_EXIST) {
                Toast.makeText(mContext, "CONNECTION_ALREADY_EXIST", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(mContext, "Connection could not be made. Please try again", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(mContext, "Connection established for FT", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(mContext, "Peer could not be found. Try again.", Toast.LENGTH_SHORT).show();
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
            Log.e(TAG, "onServiceConnectionLost: reason-" + reason);
           /* if (mSAFileTransfer != null) {
                mFileAction.onFileActionError("Service Terminated");
            }else
            {
                Toast.makeText(getApplicationContext(), "Service Terminated", Toast.LENGTH_SHORT).show();
            }*/
            mPeerAgent = null;
        }

        @Override
        public void onReceive(int channelId, byte[] data) {
        }

        @Override
        public void onError(int channelId, String errorMessage, int errorCode) {
        }
    }

    @Override
    public void releaseAgent()
    {
        if(mPeerAgent != null)
        {
            Toast.makeText(getApplicationContext(), "Service Terminated", Toast.LENGTH_SHORT).show();
        }
        super.releaseAgent();


    }

    public void releaseAgentFromApplication()
    {
        Log.v(TAG,"releaseAgentFromApplication");
        if(mPeerAgent != null)
        {
            Toast.makeText(getApplicationContext(), "Service Terminated", Toast.LENGTH_SHORT).show();
        }
    }

    public interface FileAction {
        void onFileActionError(String message);

        void onFileActionProgress(long progress);

        void onFileActionTransferComplete();

        void onFileActionCancelAllComplete();
    }




}
