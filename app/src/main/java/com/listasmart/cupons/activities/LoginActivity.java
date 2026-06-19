package com.listasmart.cupons.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.listasmart.cupons.R;
import com.listasmart.cupons.helpers.SessionManager;
import com.listasmart.cupons.network.ApiClient;
import com.listasmart.cupons.network.AuthRequest;
import com.listasmart.cupons.network.AuthResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Tela de entrada com autenticação real no backend (JWT).
 * Se já houver sessão COM token salvo, pula direto para a MainActivity.
 * Caso contrário, tenta login; se o usuário ainda não existe, registra.
 */
public class LoginActivity extends AppCompatActivity {

    private SessionManager session;
    private EditText inputName;
    private EditText inputPassword;
    private Button btnEnter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        session = new SessionManager(this);

        // Só considera logado quando há token válido (sessões legadas sem token
        // são forçadas a re-autenticar para obter o JWT do backend).
        if (session.isLoggedIn() && session.getToken() != null) {
            goToMain();
            return;
        }

        setContentView(R.layout.activity_login);

        inputName = findViewById(R.id.inputName);
        inputPassword = findViewById(R.id.inputPassword);
        btnEnter = findViewById(R.id.btnEnter);

        btnEnter.setOnClickListener(v -> attemptAuth());
    }

    private void attemptAuth() {
        String username = inputName.getText().toString().trim();
        String password = inputPassword.getText().toString();

        if (username.isEmpty()) {
            alert(R.string.error_required_title, R.string.error_name_required);
            return;
        }
        if (password.length() < 6) {
            alert(R.string.error_required_title, R.string.error_password_required);
            return;
        }

        btnEnter.setEnabled(false);
        AuthRequest body = new AuthRequest(username, password);

        // 1) Tenta login. 2) Se falhar (usuário inexistente/credencial), tenta registrar.
        ApiClient.getApiService().login(body).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse> call, @NonNull Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    onAuthSuccess(response.body());
                } else {
                    register(body);
                }
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse> call, @NonNull Throwable t) {
                btnEnter.setEnabled(true);
                alert(R.string.error_auth_title, R.string.error_network);
            }
        });
    }

    /** Fallback: cria a conta quando o login não encontra o usuário. */
    private void register(AuthRequest body) {
        ApiClient.getApiService().register(body).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse> call, @NonNull Response<AuthResponse> response) {
                btnEnter.setEnabled(true);
                if (response.isSuccessful() && response.body() != null) {
                    onAuthSuccess(response.body());
                } else {
                    // 409 (username em uso com senha errada) ou validação: credenciais inválidas.
                    alert(R.string.error_auth_title, R.string.error_auth_failed);
                }
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse> call, @NonNull Throwable t) {
                btnEnter.setEnabled(true);
                alert(R.string.error_auth_title, R.string.error_network);
            }
        });
    }

    private void onAuthSuccess(AuthResponse auth) {
        session.login(auth); // persiste token + id + username e ativa no ApiClient
        goToMain();
    }

    private void alert(int titleRes, int messageRes) {
        new AlertDialog.Builder(LoginActivity.this)
                .setTitle(titleRes)
                .setMessage(messageRes)
                .setPositiveButton(R.string.btn_ok, null)
                .show();
    }

    private void goToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
