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
 *     * Neither the name of Samsung Electronics Co., Ltd. nor the names of its contributors may be used to endorse or
 *       promote products derived from this software without specific prior written permission.
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

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.samsung.android.sdk.accessory.SAAgentV2;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FileTransferActivity extends Activity {
    private static final String TAG = "FileTransferActivity(C)";
    private static final String SRC_PATH =
            Environment.getExternalStorageDirectory().getAbsolutePath() + "/src.aaa";

    private ProgressBar mSentProgressBar;

    // FTSender
    private String mDirPath;
    private long currentTransId;
    private long mFileSize;
    private List<Long> mTransactions = new ArrayList<Long>();

    // Instances of SAAgentV2
    private FileTransferSender mFTSender = null;

    private SAAgentV2.RequestAgentCallback mAgentCallback3 = new SAAgentV2.RequestAgentCallback() {
        @Override
        public void onAgentAvailable(SAAgentV2 agent) {
            mFTSender = (FileTransferSender)agent;
            if(mFTSender != null)
                mFTSender.registerFileAction(getFileAction());
        }

        @Override
        public void onError(int errorCode, String message) {
            Log.e(TAG, "Agent initialization error: " + errorCode + ". ErrorMsg: " + message);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_transfer);
        initializeFT();

        // set permission of storage
        if(!checkPermission()) {
            requestPermission();
        }
        // set permission of storage
       /* if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }*/
    }

    private boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else  if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            int result = checkSelfPermission( Manifest.permission.READ_EXTERNAL_STORAGE);
            int result1 = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED;
        }
        else
        {
            return true;
        }
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.addCategory("android.intent.category.DEFAULT");
                intent.setData(Uri.parse(String.format("package:%s",getApplicationContext().getPackageName())));
                startActivityForResult(intent, 2296);
            } catch (Exception e) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivityForResult(intent, 2296);
            }
        } else {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
                //below android 11
                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
        }
    }

    @Override
    protected void onDestroy() {
        // Clean up connections
        destroyFT();
        super.onDestroy();
    }

    public void mOnClickFileTransfer(View v) {
        switch (v.getId()) {
            case R.id.buttonConnect3: {
                if (mFTSender != null) {
                    mFTSender.connect();
                } else {
                    Toast.makeText(getApplicationContext(), "Service not Bound", Toast.LENGTH_SHORT).show();
                }
                break;
            }
            case R.id.buttonSend3: {
                File file = new File(SRC_PATH);
                mFileSize = file.length();
                if (mFTSender != null && currentTransId == -1) {
                    Toast.makeText(getApplicationContext(), SRC_PATH + " selected " + " size " + mFileSize + " bytes", Toast.LENGTH_SHORT).show();
                    try {
                        int trId = mFTSender.sendFile(SRC_PATH);
                        mTransactions.add((long) trId);
                        currentTransId = trId;
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                        Toast.makeText(getApplicationContext(), "IllegalArgumentException", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getApplicationContext(), currentTransId + " Current file is not yet finished.", Toast.LENGTH_SHORT).show();
                }
                break;
            }
            case R.id.buttonCancel: {
                if (mFTSender != null) {
                    try {
                        mFTSender.cancelFileTransfer((int) currentTransId);
                        mTransactions.remove(currentTransId);
                        currentTransId = -1;
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                        Toast.makeText(getApplicationContext(), "IllegalArgumentException", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "no binding to service", Toast.LENGTH_SHORT).show();
                }
                break;
            }
            case R.id.buttonCancelAll: {
                if (mFTSender != null) {
                    mFTSender.cancelAllTransactions();
                    mTransactions.clear();
                    currentTransId = -1;
                } else {
                    Toast.makeText(getApplicationContext(), "no binding to service", Toast.LENGTH_SHORT).show();
                }
                break;
            }
            default:
        }
    }

    private void initializeFT() {
        currentTransId = -1;

        mSentProgressBar = (ProgressBar) findViewById(R.id.fileTransferProgressBar);
        mSentProgressBar.setMax(100);

        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Toast.makeText(getApplicationContext(), " No SDCARD Present", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            mDirPath = Environment.getExternalStorageDirectory() + File.separator + "FileTransferSender";
            File file = new File(mDirPath);
            if (file.mkdirs()) {
                Toast.makeText(getApplicationContext(), " Stored in " + mDirPath, Toast.LENGTH_LONG).show();
            }
        }

        SAAgentV2.requestAgent(getApplicationContext(), FileTransferSender.class.getName(), mAgentCallback3);
    }

    private void destroyFT() {
        //currentTransId = -1;

        if(mFTSender != null) {
            try {
                if(currentTransId != -1) {
                    mFTSender.cancelAllTransactions();
                    mTransactions.clear();
                    currentTransId = -1;
                }
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
            mFTSender.releaseAgent();
            mFTSender = null;
        }
    }

    private FileTransferSender.FileAction getFileAction() {
        return new FileTransferSender.FileAction() {
            @Override
            public void onFileActionError(final String message) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mSentProgressBar.setProgress(0);
                        mTransactions.remove(currentTransId);
                        currentTransId = -1;
                        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFileActionProgress(final long progress) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mSentProgressBar.setProgress((int) progress);
                    }
                });
            }

            @Override
            public void onFileActionTransferComplete() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mSentProgressBar.setProgress(0);
                        mTransactions.remove(currentTransId);
                        currentTransId = -1;
                        Toast.makeText(getApplicationContext(), "Transfer Completed!", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFileActionCancelAllComplete() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mSentProgressBar.setProgress(0);
                        mTransactions.remove(currentTransId);
                        currentTransId = -1;
                    }
                });
            }
        };
    }


}
