package com.noxlogic.joindin;

/*
 * This is the main activity class. All activies in our application will extend
 * JIActivity instead of activity. This class supplies us with some additional
 * tools and options which need to be set for all activity screens (for instance,
 * the menu)
 */

import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.widget.Toast;

public class JIActivity extends Activity {

    // Returns boolean if the user has entered valid credentials in the preferences
    // screen to login into the joind.in API. Needed to send registered comments and
    // to attend events.
    static public boolean hasValidCredentials (Context context) {
        String ret = validateCredentials (context);
        return ret.startsWith("T|");
    }

    // This function will validate the credentials given in the preferences.
    // Returns a string in the following format:
    // T|<message>   credentials are valid
    // F|<message>   credentials are not valid
    static public String validateCredentials (Context context) {
        String result;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        // Check credentials in the joind.in API
        JIRest rest = new JIRest ();
        int error = rest.postXML ("http://joind.in/api/user", "<request><action type=\"validate\" output=\"json\"><uid>"+prefs.getString("username", "")+"</uid><pass>"+JIRest.md5(prefs.getString("password", ""))+"</pass></action></request>");
        if (error == JIRest.OK) {
            try {
                JSONObject json = new JSONObject(rest.getResult());
                result = json.optString("msg");
            } catch (Exception e) {
                // Incorrect JSON, just return plain result from http
                result = rest.getResult();
            }
        } else {
            // Incorrect result, return error
            result = rest.getError();
        }

        // Result ok?
        if (result.compareTo ("success") == 0) {
            return "T|Correct credentials supplied";
        }
        // Something went wrong
        if (result.compareTo ("Invalid user") == 0) result = "Incorrect login credentials. Change credentials in the Settings menu.";
        return "F|"+result;
    }

    // Automatically called by all activities.
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Needed to show the circular progress animation in the top right corner.
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
    }

    // Displays (or hides) the ciruclar progress animation in the top left corner
    public void displayProgressBar (final boolean state) {
        runOnUiThread(new Runnable() {
            public void run() {
                setProgressBarIndeterminateVisibility(state);
            }
        });
    }

    // Automatically called. Creates option menu. All activities share the same menu.
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    // Handler for options menu
    public boolean onOptionsItemSelected (MenuItem item) {
        switch (item.getItemId()) {
            case R.id.about_menu_item :
                        // Display about box
                        Dialog about = new AlertDialog.Builder(this)
                            .setIcon(R.drawable.rating_5)
                            .setTitle("About,..")
                            .setPositiveButton("Close", null)
                            .setMessage("Joind.In Android application created by Joshua Thijssen. Please send bugreports, feature requests etc to jthijssen@noxlogic.nl \n\nLogos and images are copyright Joind.in")
                            .create();
                        about.show();
                        break;

            case R.id.clear_menu_item :
                        // Removes all items from the database
                        DataHelper dh = new DataHelper(this);
                        dh.deleteAll ();
                        Toast toast = Toast.makeText (getBaseContext(), "Cache is cleared", Toast.LENGTH_LONG);
                        toast.show ();
                        break;

            case R.id.settings_menu_item :
                        // Displays preferences
                        Intent settingsActivity = new Intent(getBaseContext(), Preferences.class);
                        startActivity(settingsActivity);
                        break;
        }
        return true;
    }

}