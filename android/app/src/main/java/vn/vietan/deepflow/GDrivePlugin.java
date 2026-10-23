package vn.vietan.deepflow;

import android.accounts.Account;
import android.content.Intent;

import androidx.activity.result.ActivityResult;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.ActivityCallback;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;

/**
 * Đăng nhập Google và lấy access token để đọc/ghi Google Drive (thư mục ẩn của app + file do app tạo).
 * Chỉ cần OAuth Client ID loại "Android" (tên gói + SHA-1) trong Google Cloud, không cần client secret.
 */
@CapacitorPlugin(name = "GDrive")
public class GDrivePlugin extends Plugin {
    private static final String S_APPDATA = "https://www.googleapis.com/auth/drive.appdata";
    private static final String S_FILE = "https://www.googleapis.com/auth/drive.file";
    private static final String TOKEN_SCOPE = "oauth2:" + S_APPDATA + " " + S_FILE + " email";

    private GoogleSignInClient client() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(S_APPDATA), new Scope(S_FILE))
                .build();
        return GoogleSignIn.getClient(getActivity(), gso);
    }

    @PluginMethod
    public void signIn(PluginCall call) {
        GoogleSignInAccount acc = GoogleSignIn.getLastSignedInAccount(getContext());
        if (acc != null && GoogleSignIn.hasPermissions(acc, new Scope(S_APPDATA), new Scope(S_FILE))) {
            fetchToken(call, acc, true);
            return;
        }
        startActivityForResult(call, client().getSignInIntent(), "onSignIn");
    }

    @ActivityCallback
    private void onSignIn(PluginCall call, ActivityResult result) {
        if (call == null) return;
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
        try {
            GoogleSignInAccount acc = task.getResult(ApiException.class);
            fetchToken(call, acc, true);
        } catch (ApiException e) {
            call.reject("Đăng nhập thất bại (mã " + e.getStatusCode() + ")", String.valueOf(e.getStatusCode()));
        }
    }

    @PluginMethod
    public void getToken(PluginCall call) {
        GoogleSignInAccount acc = GoogleSignIn.getLastSignedInAccount(getContext());
        if (acc == null) { call.reject("Chưa đăng nhập", "signedOut"); return; }
        fetchToken(call, acc, true);
    }

    @PluginMethod
    public void clearToken(PluginCall call) {
        final String token = call.getString("token");
        new Thread(() -> {
            try { if (token != null) GoogleAuthUtil.clearToken(getContext(), token); } catch (Exception ignored) {}
            call.resolve();
        }).start();
    }

    @PluginMethod
    public void signOut(PluginCall call) {
        client().signOut().addOnCompleteListener(t -> call.resolve());
    }

    @PluginMethod
    public void status(PluginCall call) {
        GoogleSignInAccount acc = GoogleSignIn.getLastSignedInAccount(getContext());
        JSObject r = new JSObject();
        r.put("signedIn", acc != null);
        if (acc != null) { r.put("email", acc.getEmail()); r.put("name", acc.getDisplayName()); }
        call.resolve(r);
    }

    private void fetchToken(final PluginCall call, final GoogleSignInAccount acc, final boolean allowUi) {
        new Thread(() -> {
            try {
                Account a = acc.getAccount();
                String token = GoogleAuthUtil.getToken(getContext(), a, TOKEN_SCOPE);
                JSObject r = new JSObject();
                r.put("accessToken", token);
                r.put("email", acc.getEmail());
                r.put("name", acc.getDisplayName());
                call.resolve(r);
            } catch (UserRecoverableAuthException e) {
                if (allowUi && e.getIntent() != null) {
                    getActivity().runOnUiThread(() -> startActivityForResult(call, e.getIntent(), "onConsent"));
                } else {
                    call.reject("Cần cấp quyền Google Drive", "consent");
                }
            } catch (Exception e) {
                call.reject("Không lấy được quyền truy cập Google Drive: " + e.getMessage(), "token");
            }
        }).start();
    }

    @ActivityCallback
    private void onConsent(PluginCall call, ActivityResult result) {
        if (call == null) return;
        GoogleSignInAccount acc = GoogleSignIn.getLastSignedInAccount(getContext());
        if (acc == null) { call.reject("Chưa đăng nhập", "signedOut"); return; }
        fetchToken(call, acc, false);
    }
}
