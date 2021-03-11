package com.mesalu.viv2.android_ui.ui.overview;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;

import com.mesalu.viv2.android_ui.R;
import com.mesalu.viv2.android_ui.data.LoginRepository;
import com.mesalu.viv2.android_ui.ui.login.LoginActivity;

/**
 * Simple activity for presenting "info at a glance" for the user
 * Delegates to other activities as needed. (e.g. launches login activity
 * by default if no user logged in), and offers other navigation paths, such
 * as signing out, or entering a pet-specific management activity.
 */
public class OverviewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_overview);

        // Check if the login info is not configured.
        if (LoginRepository.loginRequired()) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }
}