package com.mesalu.viv2.android_ui.ui.overview;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.mesalu.viv2.android_ui.R;
import com.mesalu.viv2.android_ui.data.LoginRepository;
import com.mesalu.viv2.android_ui.ui.login.LoginActivity;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Simple activity for presenting "info at a glance" for the user
 * Delegates to other activities as needed. (e.g. launches login activity
 * by default if no user logged in), and offers other navigation paths, such
 * as signing out, or entering a pet-specific management activity.
 */
public class OverviewActivity extends AppCompatActivity {
    // used to drive the silent refresh loop under the UI
    // anchored here in a UI-activity so that it
    // can be start/stopped in accordance to app activity.
    // NOTE: If some condition were to schedule multiple
    //       refresh attempts then those duplicates will
    //       never 'work themselves out' of the refresh cycle.
    //       So that will be a bug to be on the lookout for.
    private ScheduledExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_overview);

        // Auth token life cycle management:
        // whenever we get a new token set, schedule a refresh for
        // when it expires - handle initializing if necessary when recieving tokenset..
        LoginRepository.getInstance().getObservable().observe(this,
                tokenSet -> {
                    if (tokenSet != null) {
                        if (executorService == null || executorService.isShutdown())
                            startSilentRefreshCycle();
                        else
                            scheduleRefreshForExpiry(tokenSet.getAccessExpiry());
                    }
                });

        // if we're not logged in at all, then hand off to the login activity.
        if (!LoginRepository.getInstance().isLoggedIn()) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        }

        BottomNavigationView navView = findViewById(R.id.nav_view);
        navView.setBackground(null);

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_pet, R.id.navigation_env)
                .build();

        final NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> {
            if (navController.getCurrentDestination() == null) return;

            int id = navController.getCurrentDestination().getId();
            if (id == R.id.navigation_pet) {
                Log.d("OA", "FAB pressed with pet fragment on");
                new ViewModelProvider(this).get(PetInfoViewModel.class).signal();
            }
            else if (id == R.id.navigation_env) {
                Log.d("OA", "FAB pressed with env fragment on");
                new ViewModelProvider(this).get(EnvironmentInfoViewModel.class).signal();
            }
            else {
                Snackbar snackbar = Snackbar
                        .make(v, "Fab: Hello world", Snackbar.LENGTH_SHORT);
                snackbar.show();
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();

        // suspend the refresh service - don't want to be draining battery.
        stopSilentRefreshCycle(); // handles side conditions, we can call blindly.
    }

    @Override
    public void onResume() {
        super.onResume();

        // check if we should be running a refresh cycle
        if (LoginRepository.getInstance().getTokens() != null) {
            Log.d("OA", "Resumed with tokens available, starting refresh cycle");
            startSilentRefreshCycle();
        }
        else {
            Log.d("OA", "Resumed with no tokens, awaiting LoginActivity success");
        }
    }

    private void startSilentRefreshCycle() {
        if (executorService == null || executorService.isShutdown())
            executorService = Executors.newSingleThreadScheduledExecutor();

        else return; // if neither are true then the refresh cycle is already running.

        if (LoginRepository.getInstance().getTokens() != null) {
            // check if the tokens have already expired:
            ZonedDateTime accessExpiry = LoginRepository.getInstance().getTokens().getAccessExpiry();
            if (ZonedDateTime.now().compareTo(accessExpiry) >= 0) {
                // attempt a refresh now
                executorService.schedule(this::silentRefreshHandler,
                        1,
                        TimeUnit.NANOSECONDS);
            }
            else {
                // schedule a refresh for just before it expires.
                scheduleRefreshForExpiry(accessExpiry);
            }
        }
    }

    private void stopSilentRefreshCycle() {
        if (executorService == null || executorService.isShutdown()) return;

        Log.d("OA", "Shutting down token refresh cycle");
        executorService.shutdownNow();
    }

    /**
     * Schedules a refresh attempt to occur before the specified expiry time.
     *
     * @param expiryTime when the active access token will expire.
     */
    private void scheduleRefreshForExpiry(ZonedDateTime expiryTime) {
        if (executorService == null || executorService.isShutdown()) return;
        // calculate the span of time between now and expiry time.
        Duration span = Duration.between(ZonedDateTime.now(), expiryTime);

        // schedule the attempt
        Log.d("OA", "Scheduling a refresh to occur at: " + expiryTime.toString() +
                " (" + span.getSeconds() + " seconds from now)");

        executorService.schedule(this::silentRefreshHandler,
                span.getSeconds(),
                TimeUnit.SECONDS);
    }

    /**
     * Used when a refresh attempt errors - should slowly back off and eventually
     * terminate retries.
     */
    private void scheduleRetryAttempt() {
        if (executorService == null || executorService.isShutdown()) return;

        Log.d("OA", "Token refresh failure detected, queuing up a retry");
        // TODO: count the number of retries since the last success, use that to steadily backoff
        //       and eventually cancel.
        executorService.schedule(this::silentRefreshHandler,
               3,
                TimeUnit.SECONDS);
    }

    /**
     * Used as the entry point for scheduled refresh actions.
     */
    private void silentRefreshHandler() {
        if (executorService == null || executorService.isShutdown()) return; // do nothing

        LoginRepository.getInstance().attemptRefresh(throwable -> {
            Log.e("OA", "Token refresh attempt failed! " + throwable.toString());
            scheduleRetryAttempt();
        });
    }
}