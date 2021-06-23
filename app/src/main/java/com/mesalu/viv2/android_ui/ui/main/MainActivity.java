package com.mesalu.viv2.android_ui.ui.main;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.mesalu.viv2.android_ui.R;
import com.mesalu.viv2.android_ui.data.LoginRepository;
import com.mesalu.viv2.android_ui.ui.charting.ChartFragment;
import com.mesalu.viv2.android_ui.ui.charting.ChartTarget;
import com.mesalu.viv2.android_ui.ui.events.ChartTargetEvent;
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
public class MainActivity extends AppCompatActivity {
    private static final int LOGIN_REQUEST_CODE = 1;

    // used to drive the silent refresh loop under the UI
    // anchored here in a UI-activity so that it
    // can be start/stopped in accordance to app activity.
    // NOTE: If some condition were to schedule multiple
    //       refresh attempts then those duplicates will
    //       never 'work themselves out' of the refresh cycle.
    //       So that will be a bug to be on the lookout for.
    private ScheduledExecutorService executorService;

    private MainActivityViewModel stateViewModel;
    private FloatingActionButton fab;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        stateViewModel = new ViewModelProvider(this).get(MainActivityViewModel.class);
        stateViewModel.getChartTargetEvent().observe(this, this::onChartTargetEvent);

        // Auth token life cycle management:
        // whenever we get a new token set, schedule a refresh for
        // when it expires - handle initializing if necessary when receiving token set..
        LoginRepository.getInstance().getObservable().observe(this,
                tokenSet -> {
                    if (tokenSet != null) {
                        if (executorService == null || executorService.isShutdown())
                            startBackgroundTasks();
                        else
                            scheduleRefreshForExpiry(tokenSet.getAccessExpiry());
                    }
                });

        // if we're not logged in at all, then hand off to the login activity.
        if (!LoginRepository.getInstance().isLoggedIn()) {
            startLoginActivity();
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

        fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> {
            CommonSignalAwareViewModel activeViewModel = getActiveFragmentViewModel();
            if (activeViewModel != null) activeViewModel.signalFab();
            else {
                Snackbar snackbar = Snackbar
                        .make(v, "Fab: Hello world", Snackbar.LENGTH_SHORT);
                snackbar.show();
            }
        });

        // Setup the swipe-refresh callback:
        SwipeRefreshLayout swipeRefreshLayout = findViewById(R.id.swipe_refresh);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            dispatchRefresh();

            // TODO: need a callback for invoking setRefreshing at the best time.
            swipeRefreshLayout.setRefreshing(false);
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.overview_actions_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_logout) {
            LoginRepository.getInstance().logout();

            // TODO: clear view models (at least the ones that contain user specific data.)

            startLoginActivity();
        }

        else if (id == R.id.action_refresh) {
            dispatchRefresh();
        }

        else {
            return super.onOptionsItemSelected(item);
        }

        return true;
    }

    @Override
    public void onPause() {
        super.onPause();

        // suspend the refresh service - don't want to be draining battery.
        stopBackgroundTasks(); // handles side conditions, we can call blindly.
    }

    @Override
    public void onResume() {
        super.onResume();

        // check if we should be running a refresh cycle
        if (LoginRepository.getInstance().getTokens() != null) {
            Log.d("OA", "Resumed with tokens available, starting refresh cycle");
            startBackgroundTasks();
        }
        else {
            Log.d("OA", "Resumed with no tokens, awaiting LoginActivity success");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == LOGIN_REQUEST_CODE) {
            // if the login activity was canceled then finish.
            if (resultCode != RESULT_OK) finish();
        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * Starts LoginActivity for a result using the proper request code
     */
    private void startLoginActivity() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivityForResult(intent, LOGIN_REQUEST_CODE);
    }

    /**
     * Starts the cycles that manage background tasks:
     * - a timer for posting UI update events (events that get updated roughly every 15 seconds.)
     * - A silent refresh cycle, which manages instigating a refrehs token exchange a few
     *      seconds before the current refresh token expires.
     */
    private void startBackgroundTasks() {
        if (executorService == null || executorService.isShutdown())
            executorService = Executors.newScheduledThreadPool(2);
        else return; // if neither are true then the various services are already running.

        executorService.scheduleAtFixedRate(this::dispatchTimedUpdate, 0, 15, TimeUnit.SECONDS);

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

    private void stopBackgroundTasks() {
        if (executorService == null || executorService.isShutdown()) return;

        Log.d("OA", "Shutting down background tasks");
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

    @Nullable
    private CommonSignalAwareViewModel getActiveFragmentViewModel() {
        // get current navigation item id, use to determine which view model to return.
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);

        // findNavController is marked nonnull, so we can assume we found it (or threw an exception?)
        NavDestination currentDestination = navController.getCurrentDestination();
        if (currentDestination == null) return null;

        ViewModelProvider provider = new ViewModelProvider(this);
        if (currentDestination.getId() == R.id.navigation_env)
            return provider.get(EnvironmentInfoViewModel.class);

        else if (currentDestination.getId() == R.id.navigation_pet)
            return provider.get(PetInfoViewModel.class);

        else return null;
    }

    private void dispatchRefresh() {
        CommonSignalAwareViewModel viewModel = getActiveFragmentViewModel();
        if (viewModel != null) viewModel.signalRefresh();
    }

    /**
     * Notifies the active fragment's main view model of a UI update. (a semi-frequent
     * dummy livedata update to trigger routine UI updates, such as updating time-related
     * text labels.)
     */
    private void dispatchTimedUpdate() {
        Log.d("OA", "Update timer fired");
        try {
            getActiveFragmentViewModel().signalUiUpdateFromBackground();
        }
        catch (Exception e) {
            Log.e("OA", "Error in dispatching timed update: ", e);
        }
    }

    private void onChartTargetEvent(ChartTargetEvent event) {
        ChartTarget target = event.consume();
        Fragment fragment = getSupportFragmentManager()
                .findFragmentByTag(ChartFragment.MGR_TAG);

        // check if the event has already be consumed.
        if (target == null && event.peek() != null) return;

        switch (event.getViewModifier()) {
            case SHOW:
                // ensure the fragment exists
                if (fragment == null)
                    getSupportFragmentManager().beginTransaction()
                            .setReorderingAllowed(true)
                            .add(R.id.chart_fragment_container, ChartFragment.class, null, ChartFragment.MGR_TAG)
                            .runOnCommit(() -> {
                                // get the fragment to load data.
                                ChartFragment chartFragment = (ChartFragment) getSupportFragmentManager()
                                        .findFragmentByTag(ChartFragment.MGR_TAG);

                                if (chartFragment != null) chartFragment.resetToTarget(target);
                                else Log.e("MainActivity", "onCommit Runnable not suitable for this use");

                                findViewById(R.id.chart_fragment_container).setVisibility(View.VISIBLE);
                                if (fab.isOrWillBeShown()) fab.hide();
                            })
                            .commit();
                // the target is set when the fragment is ready, so no need to roll through.
                // (would be slick though.)
                break;

            case NO_MODIFIER:
                // set the chart fragment (if present) to represent target
                if (fragment != null) ((ChartFragment) fragment).resetToTarget(target);
                break;

            case HIDE:
                // remove the chart fragment.
                if (fragment != null)
                    getSupportFragmentManager().beginTransaction()
                            .setReorderingAllowed(true)
                            .remove(fragment)
                            .runOnCommit(() ->{
                                findViewById(R.id.chart_fragment_container).setVisibility(View.GONE);
                                if (fab.isOrWillBeHidden()) fab.show();
                            })
                            .commit();
                else findViewById(R.id.chart_fragment_container).setVisibility(View.GONE);
        }
    }
}