package it.polito.mad.koko.kokolab3;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.util.Log;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.google.gson.Gson;

import it.polito.mad.koko.kokolab3.auth.AuthenticationUI;
import it.polito.mad.koko.kokolab3.books.InsertBook;
import it.polito.mad.koko.kokolab3.books.SearchBooks;
import it.polito.mad.koko.kokolab3.books.ShowBooks;
import it.polito.mad.koko.kokolab3.messaging.MessageManager;
import it.polito.mad.koko.kokolab3.messaging.MyFirebaseInstanceIDService;
import it.polito.mad.koko.kokolab3.messaging.ShowChats;
import it.polito.mad.koko.kokolab3.profile.EditProfile;
import it.polito.mad.koko.kokolab3.profile.Profile;
import it.polito.mad.koko.kokolab3.profile.ProfileManager;
import it.polito.mad.koko.kokolab3.profile.ProfileService;
import it.polito.mad.koko.kokolab3.profile.ShowProfile;
import it.polito.mad.koko.kokolab3.tabsHomeActivity.HomeChatList;
import it.polito.mad.koko.kokolab3.tabsHomeActivity.HomeListBook;
import it.polito.mad.koko.kokolab3.ui.ImageManager;

public class HomeActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "HomeActivity";

    private static final String PACKAGE_NAME = "it.polito.mad.koko.kokolab3";

    /**
     * Result codes needed to distinguish among all possible activities launched
     * by this one.
     */
    private static final int AUTH = 10;

    private int INSERT_BOOK = 20;

    /**
     * Request code for the activity "ShowBooks" to show only the current user's books
     */
    private int USER_BOOKS = 0;
    private ListView listView;
    private Fragment homeListBook;
    private Fragment homeListChats;
    private ViewSwitcher viewSwitcher;
    private LinearLayout layoutRecycler;
    private LinearLayout layoutList;

    //private int SEARCH_BOOKS = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG,"onCreate() called");

        ProfileManager.logout();

        // Creating an empty SharedPreferences object
        SharedPreferences.Editor sharedPreferencesEditor = this.getSharedPreferences(PACKAGE_NAME, Context.MODE_PRIVATE).edit();
        sharedPreferencesEditor.putString("Profile", new Gson().toJson(new Profile())).commit();
        sharedPreferencesEditor.apply();

        // UI
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            /*Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();*/

            Intent insertBook = new Intent(getApplicationContext(), InsertBook.class);
            insertBook.putExtra("uid", ProfileManager.getCurrentUserID());
            //BookManager.removeUserBooksEventListener();
            //BookManager.removeSearchBooksEventListener();
            startActivityForResult(insertBook, INSERT_BOOK);
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Launching the authentication UI
        AuthenticationUI.launch(this);

        ProfileManager.populateUsersList();

        // If the user has already logged in
        if (ProfileManager.hasLoggedIn()) {
            // Starting the profile management service
            startService(new Intent(this, ProfileService.class));

            Log.d(TAG, "Registration completed: " + ProfileManager.hasCompletedRegistration());

            // If the user has not completed the registration process already
            if(!ProfileManager.hasCompletedRegistration()) {
                // Launch the EditProfile activity=
                startActivity(new Intent(getApplicationContext(), EditProfile.class));

                return;
            }

            // Retrieve all current user's chats
            MessageManager.setUserChatsIDListener();
            MessageManager.populateUserChatsID();
        }

        // UI
        viewSwitcher = findViewById(R.id.home_switcher);
        layoutRecycler = findViewById(R.id.home_recycler_switcher);
        layoutList = findViewById(R.id.home_list_switcher);
        TabLayout tab_layout = findViewById(R.id.tabs_home);
        tab_layout.setTabMode(TabLayout.MODE_FIXED);
        tab_layout.addTab(tab_layout.newTab().setText("home"));
        homeListBook = new HomeListBook();
        tab_layout.addTab(tab_layout.newTab().setText("chats"));
        homeListChats = new HomeChatList();
        tab_layout.addTab(tab_layout.newTab().setText("in progress"));
        selectFragment(0);

        tab_layout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                Log.d(TAG,"onTabSelected"+String.valueOf(tab.getPosition()));
                selectFragment(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                Log.d(TAG,"onTabUnselected"+String.valueOf(tab.getPosition()));
                removeFragment(tab.getPosition());
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                Log.d(TAG,"onTabReselected"+String.valueOf(tab.getPosition()));
            }
        });
    }

    private void removeFragment(int position) {
        switch (position){
            case 0:
                getFragmentManager().beginTransaction().remove(homeListBook).commit();
                break;
            case 1:
                    getFragmentManager().beginTransaction().remove(homeListChats).commit();
                break;
            case 2:
                break;
            default:

                break;
        }

    }

    private void selectFragment(int position) {
        switch (position){
            case 0:
                if (viewSwitcher.getCurrentView() != layoutRecycler) {
                    viewSwitcher.showPrevious();
                    getFragmentManager().beginTransaction().add(android.R.id.content, homeListBook).commit();
                }
                //new HomeListBook();
                break;
            case 1:
                if (viewSwitcher.getCurrentView() != layoutList) {
                    viewSwitcher.showNext();
                }
                getFragmentManager().beginTransaction().add(android.R.id.content, homeListChats).commit();
                break;
            case 2:
                if (viewSwitcher.getCurrentView() != layoutList) {
                    viewSwitcher.showNext();
                }
                //getFragmentManager().beginTransaction().add(android.R.id.content, homeListChats).commit();
                break;
            default:

                break;
        }

    }

    /**
     * When the sign-in flow is complete
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @SuppressLint("RestrictedApi")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d(TAG,"onActivityResult() called");

        /*if (requestCode == SEARCH_BOOKS && resultCode != RESULT_CANCELED) {

            Intent showSearchBooks = new Intent(getApplicationContext(), ShowBooks.class);
            showSearchBooks.putExtra("request_code", SEARCH_BOOKS);
            startActivity(showSearchBooks);
        }*/

        // Debugging
        Log.d(TAG, "requestCode: " + requestCode);
        Log.d(TAG, "resultCode: " + resultCode);

        //if (requestCode == INSERT_BOOK)
            // Retrieving all user's books
            //BookManager.populateUserBookList();

            // Returning in HomeActivity from an Authentication procedure
        if (resultCode == AUTH) {
            // Debug
            Log.d(TAG, "Returning in HomeActivity from an Authentication procedure.");

            // Inform the user of the successful authentication
            Toast.makeText(this, "Successfully signed in", Toast.LENGTH_LONG).show();

            if(!ProfileService.isRunning())
                // Starting the profile management service
                startService(new Intent(getApplicationContext(), ProfileService.class));

            // Retrieve all current user's chats
            MessageManager.setUserChatsIDListener();
            MessageManager.populateUserChatsID();

            // Loading the new profile on Firebase
            ProfileManager.addProfile(ProfileManager.getCurrentUserID(), ProfileManager.getCurrentUser().getEmail());
            MyFirebaseInstanceIDService myFirebaseInstanceIDService = new MyFirebaseInstanceIDService();
            myFirebaseInstanceIDService.onTokenRefresh();

            ProfileService.refreshCurrentUserProfileListener();

            // If this is a new user or the user has not finished the registration
            if (ProfileManager.profileIsNotPresent((ProfileManager.getCurrentUserID()))) {
                startActivity(new Intent(getApplicationContext(), EditProfile.class));
            } else {
                if (ProfileManager.getProfile().getImage() != null) {
                    Profile p = ProfileManager.getProfile();
                    ImageManager.loadBitmap(p.getImage());
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG,"onBackPressed() called");

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG,"onOptionsItemSelected() called");

        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        Log.d(TAG,"onNavigationItemSelected() called");

        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.view_profile) {
            Intent i = new Intent(getApplicationContext(), ShowProfile.class);
            i.putExtra("UserID", ProfileManager.getCurrentUserID());
            startActivity(i);

        } else if (id == R.id.edit_profile) {
            startActivity(new Intent(getApplicationContext(), EditProfile.class));
        } else if (id == R.id.my_books) {

            Intent showBooks = new Intent(getApplicationContext(), ShowBooks.class);
            showBooks.putExtra("request_code", USER_BOOKS);
            startActivity(showBooks);

        } else if (id == R.id.search_books) {
            Intent searchBooks = new Intent(getApplicationContext(), SearchBooks.class);
            // BookManager.removeUserBooksEventListener();
            // startActivityForResult(searchBooks, SEARCH_BOOKS);
            startActivity(searchBooks);

        } else if (id == R.id.nav_chats) {
            //DefaultDialogsActivity.open(this);

            startActivity(new Intent(getApplicationContext(), ShowChats.class));

        } else if (id == R.id.sign_out) {
            ProfileManager.logout();
            AuthenticationUI.launch(this);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d(TAG,"onResume() called");
    }
}