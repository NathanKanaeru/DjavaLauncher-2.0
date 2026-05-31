package com.nathan.djavarp.game;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.joom.paranoid.Obfuscate;
import com.nathan.djavarp.game.ui.AttachEdit;
import com.nathan.djavarp.game.ui.ChatWindow;
import com.nathan.djavarp.game.ui.CustomKeyboard;
import com.nathan.djavarp.game.ui.LoadingScreen;
import com.nathan.djavarp.game.ui.tab.TabManager;
import androidx.activity.OnBackPressedCallback;
import com.nathan.djavarp.game.ui.dialog.DialogManager;
import com.nathan.djavarp.launcher.util.SignatureChecker;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
@Obfuscate
public class SAMP extends GTASA implements CustomKeyboard.InputListener, HeightProvider.HeightListener {
    private static final String TAG = "SAMP";
    private static SAMP instance;

    private CustomKeyboard mKeyboard;
    private DialogManager mDialog;
    private HeightProvider mHeightProvider;

    private AttachEdit mAttachEdit;
    private LoadingScreen mLoadingScreen;
    private ChatWindow mChatWindow;
    private TabManager mTabManager;

    //public native void sendDialogResponse(int i, int i2, int i3, byte[] str);

    public static SAMP getInstance() {
        return instance;
    }

    private void showTab()
    {
        runOnUiThread(() -> {
            if (mTabManager != null) mTabManager.show();
        });
    }

    private void hideTab()
    {
        runOnUiThread(() -> {
            if (mTabManager != null) mTabManager.hide();
        });
    }

    private void setTab(int id, String name, int score, int ping)
    {
        int color;
        // Standard SA-MP player color derivation
        switch ((id * 3 + 7) % 10) {
            case 0: color = 0xFFE2C063; break;
            case 1: color = 0xFFC4C4C4; break;
            case 2: color = 0xFF4A6C7A; break;
            case 3: color = 0xFF94A2B3; break;
            case 4: color = 0xFF889C8E; break;
            case 5: color = 0xFFC8A8A8; break;
            case 6: color = 0xFF6B8B9A; break;
            case 7: color = 0xFF9CB48C; break;
            case 8: color = 0xFFC8B48C; break;
            default: color = 0xFFB48C9C; break;
        }
        final int c = color;
        runOnUiThread(() -> {
            if (mTabManager != null) mTabManager.setStat(id, c, name, score, ping);
        });
    }

    private void clearTab()
    {
        runOnUiThread(() -> {
            if (mTabManager != null) mTabManager.clear();
        });
    }

    private void showLoadingScreen()
    {

    }

    private void hideLoadingScreen()
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLoadingScreen.hide();
            }
        });
    }

    public void setPauseState(boolean pause) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (pause) {
                    mDialog.hideWithoutReset();
                    mAttachEdit.hideWithoutReset();
                }
                else {
                    if(mDialog.isShow)
                        mDialog.showWithOldContent();
                    if(mAttachEdit.isShow)
                        mAttachEdit.showWithoutReset();
                }
            }
        });
    }

    public void addChatMessage(String message) {
        if (mChatWindow != null) {
            mChatWindow.AddChatMessage(message);
        }
    }

    public void exitGame(){
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(false);

        finishAndRemoveTask();
        System.exit(0);
    }

    public void showDialog(int dialogId, int dialogTypeId, byte[] bArr, byte[] bArr2, byte[] bArr3, byte[] bArr4) {
        final String caption = new String(bArr, StandardCharsets.UTF_8);
        final String content = new String(bArr2, StandardCharsets.UTF_8);
        final String leftBtnText = new String(bArr3, StandardCharsets.UTF_8);
        final String rightBtnText = new String(bArr4, StandardCharsets.UTF_8);
        runOnUiThread(() -> { this.mDialog.show(dialogId, dialogTypeId, caption, content, leftBtnText, rightBtnText); });
    }

    private native void onInputEnd(byte[] str);
    @Override
    public void OnInputEnd(String str)
    {
        byte[] toReturn = null;
        try
        {
            toReturn = str.getBytes("windows-874");
        }
        catch(UnsupportedEncodingException e)
        {

        }

        try {
            onInputEnd(toReturn);
        }
        catch (UnsatisfiedLinkError e5) {
            Log.e(TAG, e5.getMessage());
        }
    }

    private void showKeyboard()
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d("AXL", "showKeyboard()");
                mKeyboard.ShowInputLayout();
            }
        });
    }

    private void hideKeyboard()
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mKeyboard.HideInputLayout();
            }
        });
    }

    private void showEditObject()
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAttachEdit.show();
            }
        });
    }

    private void hideEditObject()
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAttachEdit.hide();
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "**** onCreate");
        super.onCreate(savedInstanceState);

        if(!SignatureChecker.isSignatureValid(this, getPackageName()))
        {
            Toast.makeText(this, "Use original launcher! No remake", Toast.LENGTH_LONG).show();
            return;
        }

        //mHeightProvider = new HeightProvider(this);

        mKeyboard = new CustomKeyboard(this);

        mDialog = new DialogManager(this);

        mAttachEdit = new AttachEdit(this);

        mLoadingScreen = new LoadingScreen(this);

        mChatWindow = new ChatWindow(this);
        mTabManager = new TabManager(this);

        instance = this;

        try {
            initAssetManager(getAssets());
            initAssetManager(getAssets());
            initializeSAMP(getExternalFilesDir(null).getAbsolutePath() + "/");
        } catch (UnsatisfiedLinkError e5) {
            Log.e(TAG, e5.getMessage());
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Perform default action
                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
                // Notify native
                onEventBackPressed();
                setEnabled(true);
            }
        });
    }

    private native void initializeSAMP(String storagePath);
    public native void initAssetManager(android.content.res.AssetManager assetManager);



    @Override
    public void onStart() {
        Log.i(TAG, "**** onStart");
        super.onStart();
    }

    @Override
    public void onRestart() {
        Log.i(TAG, "**** onRestart");
        super.onRestart();
    }

    @Override
    public void onResume() {
        Log.i(TAG, "**** onResume");
        super.onResume();
        //mHeightProvider.init(view);
    }

    public native void onEventBackPressed();

    //@Override
    //public void onBackPressed() {
    //    super.onBackPressed();
    //    onEventBackPressed();
    //}

    @SuppressLint("GestureBackNavigation")
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK)
        {
            onEventBackPressed();
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onPause() {
        Log.i(TAG, "**** onPause");
        super.onPause();
    }

    @Override
    public void onStop() {
        Log.i(TAG, "**** onStop");
        super.onStop();
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "**** onDestroy");
        super.onDestroy();
    }

    @Override
    public void onHeightChanged(int orientation, int height) {
        //mKeyboard.onHeightChanged(height);
        //mDialog.onHeightChanged(height);
    }
}
