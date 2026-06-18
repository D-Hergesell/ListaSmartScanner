package com.listasmart.cupons.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.listasmart.cupons.R;
import com.listasmart.cupons.helpers.SessionManager;

/**
 * Tela de entrada (cadastro/login simulado).
 * Se já houver sessão salva em SharedPreferences, pula direto para a MainActivity.
 */
public class LoginActivity extends AppCompatActivity {

    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        session = new SessionManager(this);

        // Sessão ativa -> vai direto para a tela principal (Intent).
        if (session.isLoggedIn()) {
            goToMain();
            return;
        }

        setContentView(R.layout.activity_login);

        final EditText inputName = findViewById(R.id.inputName);
        Button btnEnter = findViewById(R.id.btnEnter);

        btnEnter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = inputName.getText().toString().trim();

                // Validação de campo + AlertDialog de feedback.
                if (name.isEmpty()) {
                    new AlertDialog.Builder(LoginActivity.this)
                            .setTitle("Campo obrigatório")
                            .setMessage("Por favor, informe seu nome para continuar.")
                            .setPositiveButton("OK", null)
                            .show();
                    return;
                }

                session.login(name);
                goToMain();
            }
        });
    }

    private void goToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
