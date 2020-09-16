package com.tolunayozturk.drivekitdemo;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.huawei.cloud.base.auth.DriveCredential;
import com.huawei.cloud.base.http.FileContent;
import com.huawei.cloud.base.media.MediaHttpDownloader;
import com.huawei.cloud.base.util.StringUtils;
import com.huawei.cloud.client.exception.DriveCode;
import com.huawei.cloud.services.drive.Drive;
import com.huawei.cloud.services.drive.DriveScopes;
import com.huawei.cloud.services.drive.model.Comment;
import com.huawei.cloud.services.drive.model.CommentList;
import com.huawei.cloud.services.drive.model.File;
import com.huawei.cloud.services.drive.model.FileList;
import com.huawei.cloud.services.drive.model.Reply;
import com.huawei.cloud.services.drive.model.ReplyList;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.common.ApiException;
import com.huawei.hms.support.api.entity.auth.Scope;
import com.huawei.hms.support.hwid.HuaweiIdAuthAPIManager;
import com.huawei.hms.support.hwid.HuaweiIdAuthManager;
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParams;
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParamsHelper;
import com.huawei.hms.support.hwid.result.AuthHuaweiId;
import com.huawei.hms.support.hwid.service.HuaweiIdAuthService;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = "MainActivity";
    private static int REQUEST_SIGN_IN_LOGIN = 1002;

    // region Resource Declaration
    Button btn_login;
    Button btn_uploadFiles;
    Button btn_queryFiles;
    Button btn_downloadFiles;
    Button btn_commentFile;
    Button btn_queryComments;
    Button btn_replyComment;
    Button btn_queryReplies;
    EditText et_uploadFileName;
    EditText et_searchFileName;
    EditText et_comment;
    EditText et_replyComment;
    TextView tv_queryResult;
    TextView tv_comments;
    TextView tv_replies;
    CheckBox cb_isApplicationData;
    // endregion

    private DriveCredential mDriveCredential;
    private File directoryCreated;
    private File fileUploaded;
    private File fileSearched;
    private Comment mComment;
    private Reply mReply;

    private String mAccessToken;
    private String mUnionId;

    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
    };

    private static final Map<String, String> MIME_TYPE_MAP = new HashMap<>();

    static {
        MIME_TYPE_MAP.put(".doc", "application/msword");
        MIME_TYPE_MAP.put(".jpg", "image/jpeg");
        MIME_TYPE_MAP.put(".mp3", "audio/x-mpeg");
        MIME_TYPE_MAP.put(".mp4", "video/mp4");
        MIME_TYPE_MAP.put(".pdf", "application/pdf");
        MIME_TYPE_MAP.put(".png", "image/png");
        MIME_TYPE_MAP.put(".txt", "text/plain");
    }

    private DriveCredential.AccessMethod refreshAccessToken = new DriveCredential.AccessMethod() {
        @Override
        public String refreshToken() {
            return mAccessToken;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // region Resource Assignment
        btn_login = findViewById(R.id.btn_login);
        btn_uploadFiles = findViewById(R.id.btn_uploadFiles);
        btn_queryFiles = findViewById(R.id.btn_queryFiles);
        btn_downloadFiles = findViewById(R.id.btn_downloadFiles);
        btn_commentFile = findViewById(R.id.btn_commentFile);
        btn_queryComments = findViewById(R.id.btn_queryComments);
        btn_replyComment = findViewById(R.id.btn_replyComment);
        btn_queryReplies = findViewById(R.id.btn_queryReplies);
        et_uploadFileName = findViewById(R.id.et_uploadFileName);
        et_searchFileName = findViewById(R.id.et_searchFileName);
        et_comment = findViewById(R.id.et_comment);
        et_replyComment = findViewById(R.id.et_replyComment);
        tv_queryResult = findViewById(R.id.tv_queryResult);
        tv_comments = findViewById(R.id.tv_comments);
        tv_replies = findViewById(R.id.tv_replies);
        cb_isApplicationData = findViewById(R.id.cb_isApplicationData);
        // endregion

        requestPermissions(PERMISSIONS_STORAGE, 1);

        btn_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                driveSignIn();
            }
        });
        btn_uploadFiles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                uploadFiles();
            }
        });
        btn_queryFiles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                queryFiles();
            }
        });
        btn_downloadFiles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                downloadFiles();
            }
        });
        btn_commentFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                commentFile();
            }
        });
        btn_queryComments.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                queryComment();
            }
        });
        btn_replyComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                replyComment();
            }
        });
        btn_queryReplies.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                queryReply();
            }
        });
    }

    public int init(String unionId, String accessToken, DriveCredential.AccessMethod refreshAccessToken) {
        if (StringUtils.isNullOrEmpty(unionId) || StringUtils.isNullOrEmpty(accessToken)) {
            return DriveCode.ERROR;
        }
        DriveCredential.Builder builder = new DriveCredential.Builder(unionId, refreshAccessToken);
        mDriveCredential = builder.build().setAccessToken(accessToken);
        return DriveCode.SUCCESS;
    }

    private void driveSignIn() {
        List<Scope> scopeList = new ArrayList<>();
        scopeList.add(new Scope(DriveScopes.SCOPE_DRIVE));
        scopeList.add(new Scope(DriveScopes.SCOPE_DRIVE_APPDATA));
        scopeList.add(new Scope(DriveScopes.SCOPE_DRIVE_FILE));
        scopeList.add(new Scope(DriveScopes.SCOPE_DRIVE_METADATA));
        scopeList.add(new Scope(DriveScopes.SCOPE_DRIVE_METADATA_READONLY));
        scopeList.add(new Scope(DriveScopes.SCOPE_DRIVE_READONLY));
        scopeList.add(HuaweiIdAuthAPIManager.HUAWEIID_BASE_SCOPE);

        HuaweiIdAuthParams authParams = new HuaweiIdAuthParamsHelper(HuaweiIdAuthParams.DEFAULT_AUTH_REQUEST_PARAM)
                .setAccessToken()
                .setIdToken()
                .setScopeList(scopeList)
                .createParams();

        HuaweiIdAuthService authService = HuaweiIdAuthManager.getService(this, authParams);
        startActivityForResult(authService.getSignInIntent(), REQUEST_SIGN_IN_LOGIN);
    }

    private void uploadFiles() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mAccessToken == null) {
                        Log.d(TAG, "need to sign in first");
                        return;
                    }
                    if (StringUtils.isNullOrEmpty(et_uploadFileName.getText().toString())) {
                        Log.d(TAG, "file name is required to upload");
                        return;
                    }

                    String path = getExternalFilesDir(null).getAbsolutePath()
                            + "/" + et_uploadFileName.getText();
                    Log.d(TAG, "run: " + path);
                    java.io.File fileObj = new java.io.File(path);
                    if (!fileObj.exists()) {
                        Log.d(TAG, "file does not exists");
                        return;
                    }

                    Drive drive = buildDrive();
                    Map<String, String> appProperties = new HashMap<>();
                    appProperties.put("appProperties", "property");

                    String dirName = "Uploads";
                    File file = new File();
                    file.setFileName(dirName)
                            .setAppSettings(appProperties)
                            .setMimeType("application/vnd.huawei-apps.folder");
                    directoryCreated = drive.files().create(file).execute();

                    // Upload the file
                    File fileToUpload = new File()
                            .setFileName(fileObj.getName())
                            .setMimeType(mimeType(fileObj))
                            .setParentFolder(Collections.singletonList(directoryCreated.getId()));

                    Drive.Files.Create request = drive.files()
                            .create(fileToUpload, new FileContent(mimeType(fileObj), fileObj));

                    fileUploaded = request.execute();

                    Log.d(TAG, "upload success");
                } catch (Exception e) {
                    Log.d(TAG, "upload error " + e.toString());
                }
            }
        }).start();
    }

    private void queryFiles() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mAccessToken == null) {
                        Log.d(TAG, "need to sign in first");
                        return;
                    }

                    String containers = "";
                    String queryFile = "fileName = '" + et_searchFileName.getText()
                            + "' and mimeType != 'application/vnd.huawei-apps.folder'";

                    if (cb_isApplicationData.isChecked()) {
                        containers = "applicationData";
                        queryFile = "'applicationData' in parentFolder and ".concat(queryFile);
                    }

                    Drive drive = buildDrive();
                    Drive.Files.List request = drive.files().list();
                    FileList files;

                    while (true) {
                        files = request
                                .setQueryParam(queryFile)
                                .setPageSize(10)
                                .setOrderBy("fileName")
                                .setFields("category,nextCursor,files(id,fileName,size)")
                                .setContainers(containers)
                                .execute();

                        if (files == null || files.getFiles().size() > 0) {
                            break;
                        }

                        if (!StringUtils.isNullOrEmpty(files.getNextCursor())) {
                            request.setCursor(files.getNextCursor());
                        } else {
                            break;
                        }
                    }

                    String text = "";
                    if (files != null && files.getFiles().size() > 0) {
                        fileSearched = files.getFiles().get(0);
                        text = fileSearched.toString();
                    } else {
                        text = "empty";
                    }

                    final String finalText = text;
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tv_queryResult.setText(finalText);
                        }
                    });

                    Log.d(TAG, "query success");
                } catch (Exception e) {
                    Log.d(TAG, "query error " + e.toString());
                }
            }
        }).start();
    }

    private void downloadFiles() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mAccessToken == null) {
                        Log.d(TAG, "need to sign in first");
                        return;
                    }
                    if (fileSearched == null) {
                        Log.d(TAG, "query file first");
                        return;
                    }

                    Drive drive = buildDrive();
                    File fileToDownload = new File();
                    Drive.Files.Get request = drive.files().get(fileSearched.getId());
                    fileToDownload.setFileName(fileSearched.getFileName())
                            .setId(fileSearched.getId());

                    MediaHttpDownloader downloader = request.getMediaHttpDownloader();
                    downloader.setContentRange(0, fileSearched.getSize() - 1);

                    String path = getExternalFilesDir(null).getAbsolutePath() + "/" + fileSearched.getFileName();
                    request.executeContentAndDownloadTo(new FileOutputStream(new java.io.File(path)));

                    Log.d(TAG, "download success");
                } catch (Exception e) {
                    Log.d(TAG, "download error " + e.toString());
                }
            }
        }).start();
    }

    private void commentFile() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mAccessToken == null) {
                        Log.d(TAG, "need to sign in first");
                        return;
                    }
                    if (fileSearched == null) {
                        Log.d(TAG, "query file first");
                        return;
                    }
                    if (StringUtils.isNullOrEmpty(et_comment.getText().toString())) {
                        Log.d(TAG, "file name is required to comment");
                        return;
                    }

                    Drive drive = buildDrive();
                    Comment comment = new Comment();
                    comment.setDescription(et_comment.getText().toString());
                    mComment = drive.comments()
                            .create(fileSearched.getId(), comment)
                            .setFields("*")
                            .execute();

                    if (mComment != null && mComment.getId() != null) {
                        Log.d(TAG, "add comment success");
                    } else {
                        Log.d(TAG, "add comment failed");
                    }

                } catch (Exception e) {
                    Log.d(TAG, "add comment failed" + e.toString());
                }
            }
        }).start();
    }

    private void queryComment() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mAccessToken == null) {
                        Log.d(TAG, "need to sign in first");
                        return;
                    }
                    if (fileSearched == null) {
                        Log.d(TAG, "query file first");
                        return;
                    }

                    Drive drive = buildDrive();
                    CommentList response = drive.comments()
                            .list(fileSearched.getId())
                            .setFields("comments(id,description,replies(description))")
                            .execute();

                    final String text = response.getComments().toString();
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tv_comments.setText(text);
                        }
                    });

                    Log.d(TAG, "query comments success");
                } catch (Exception e) {
                    Log.d(TAG, "query comments failed " + e.toString());
                }
            }
        }).start();
    }

    private void replyComment() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mAccessToken == null) {
                        Log.d(TAG, "need to sign in first");
                        return;
                    }
                    if (fileSearched == null) {
                        Log.d(TAG, "query file first");
                        return;
                    }
                    if (mComment == null) {
                        Log.d(TAG, "comment the file first");
                        return;
                    }
                    if (StringUtils.isNullOrEmpty(et_replyComment.getText().toString())) {
                        Log.d(TAG, "comment is required to reply");
                        return;
                    }

                    Drive drive = buildDrive();
                    Reply reply = new Reply();
                    reply.setDescription(et_replyComment.getText().toString());
                    mReply = drive.replies()
                            .create(fileSearched.getId(), mComment.getId(), reply)
                            .setFields("*")
                            .execute();

                    if (mReply != null && mReply.getId() != null) {
                        Log.d(TAG, "reply comment success");
                    } else {
                        Log.d(TAG, "reply comment failed");
                    }
                } catch (Exception e) {
                    Log.d(TAG, "reply comment error", e);
                }
            }
        }).start();
    }

    private void queryReply() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mAccessToken == null) {
                        Log.d(TAG, "need to sign in first");
                        return;
                    }
                    if (fileSearched == null) {
                        Log.d(TAG, "query file first");
                        return;
                    }
                    if (mComment == null) {
                        Log.d(TAG, "comment the file first");
                        return;
                    }

                    Drive drive = buildDrive();
                    ReplyList response = drive.replies()
                            .list(fileSearched.getId(), mComment.getId())
                            .setFields("replies(id,description)")
                            .execute();

                    final String text = response.getReplies().toString();
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tv_replies.setText(text);
                        }
                    });
                } catch (Exception e) {
                    Log.d(TAG, "query reply failed" + e.toString());
                }
            }
        }).start();
    }

    private Drive buildDrive() {
        Drive service = new Drive.Builder(mDriveCredential, this).build();
        return service;
    }

    private String mimeType(java.io.File file) {
        if (file != null && file.exists() && file.getName().contains(".")) {
            String fileName = file.getName();
            String suffix = fileName.substring(fileName.lastIndexOf("."));
            if (MIME_TYPE_MAP.keySet().contains(suffix)) {
                return MIME_TYPE_MAP.get(suffix);
            }
        }
        return "*/*";
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_SIGN_IN_LOGIN) {
            Task<AuthHuaweiId> authHuaweiIdTask = HuaweiIdAuthManager.parseAuthResultFromIntent(data);
            if (authHuaweiIdTask.isSuccessful()) {
                AuthHuaweiId authHuaweiId = authHuaweiIdTask.getResult();
                mAccessToken = authHuaweiId.getAccessToken();
                mUnionId = authHuaweiId.getUnionId();
                int returnCode = init(mUnionId, mAccessToken, refreshAccessToken);

                if (returnCode == DriveCode.SUCCESS) {
                    Log.d(TAG, "onActivityResult: driveSignIn success");
                } else {
                    Log.d(TAG, "onActivityResult: driveSignIn failed");
                }
            } else {
                Log.d(TAG, "onActivityResult, signIn failed: " + ((ApiException) authHuaweiIdTask.getException()).getStatusCode());
            }
        }
    }
}