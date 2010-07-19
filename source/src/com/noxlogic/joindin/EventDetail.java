package com.noxlogic.joindin;

/*
 * Displays event details (info, talk list)
 */

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

public class EventDetail extends JIActivity implements OnClickListener {
    private JSONObject eventJSON;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set layout
        setContentView(R.layout.eventdetail);

        // Get info from the intent scratch board
        try {
            this.eventJSON = new JSONObject(getIntent().getStringExtra("eventJSON"));
        } catch (JSONException e) {
            android.util.Log.e("JoindInApp", "No event passed to activity", e);
        }

        // Set title
        setTitle (R.string.activityEventDetailTitle);

        // Add handler to buttons
        Button button = (Button)findViewById(R.id.ButtonEventDetailsViewComments);
        button.setOnClickListener(this);
        button = (Button)findViewById(R.id.ButtonEventDetailsViewTalks);
        button.setOnClickListener(this);
        button = (Button)findViewById(R.id.ButtonEventDetailsViewTracks);
        button.setOnClickListener(this);
        CheckBox checkbox = (CheckBox)findViewById(R.id.CheckBoxEventDetailsAttending);
        checkbox.setOnClickListener(this);

        int event_id = this.eventJSON.optInt("ID");
        displayDetails (event_id);

        loadDetails (event_id);
    }

    public void displayDetails (int event_id) {
    	JoindInEvent event = new JoindInEvent (event_id);
    	if (event.isValid()) return;

        // Set all the event information
        TextView t;
        t = (TextView) this.findViewById(R.id.EventDetailsEventCaption);
        t.setText (event.getName());
        t = (TextView) this.findViewById(R.id.EventDetailsEventLoc);
        t.setText (event.getLocation ());
        t = (TextView) this.findViewById(R.id.EventDetailsDate);
        String d1 = event.getStartDate ();
        String d2 = event.getEndDate ();
        t.setText(d1.equals(d2) ? d1 : d1 + " - " + d2);
        t = (TextView) this.findViewById(R.id.EventDetailsStub);
        t.setText (event.getStub ());
        t = (TextView) this.findViewById(R.id.EventDetailsDescription);
        t.setText (event.getDescription());

        // Add number of comments to the correct button caption 
        Button b = (Button) this.findViewById(R.id.ButtonEventDetailsViewComments);
        int commentCount = event.getCommentCount();
        if (commentCount == 1) {
            b.setText(String.format(getString(R.string.generalViewCommentSingular), commentCount));
        } else {
            b.setText(String.format(getString(R.string.generalViewCommentPlural), commentCount));
        }

        /* Current API does not return the number of talks for specified event. As soon as it does
         * we can update the button caption */
        int talkCount = event.getTalkCount ();
        if (talkCount > 0) {
            b = (Button) this.findViewById(R.id.ButtonEventDetailsViewTalks);
            
            if (talkCount == 1) {
                b.setText(String.format(getString(R.string.generalViewTalkSingular), talkCount));
            } else {
                b.setText(String.format(getString(R.string.generalViewTalkPlural), talkCount));
            }
        }

        // See if this event has tracks
        b = (Button) this.findViewById(R.id.ButtonEventDetailsViewTracks);                
        int trackCount = event.getTrackCount ();
        if (trackCount == 1) {
            b.setText(String.format(getString(R.string.generalViewTrackSingular), trackCount));
        } else {
            b.setText(String.format(getString(R.string.generalViewTrackPlural), trackCount));
        }

        // Set track button enabled when we have at least 1 track
        b.setEnabled((trackCount > 0));

        // Tick the checkbox, depending on if we are attending or not
        CheckBox c = (CheckBox)findViewById(R.id.CheckBoxEventDetailsAttending);
        c.setChecked(event.isUserAttending());
    }

    
    public void loadDetails (final int event_id) {
        // Display progress bar
        displayProgressBar (true);

        new Thread () {
            public void run () {
                // Fetch talk data from joind.in API
                JIRest rest = new JIRest (EventDetail.this);
                int error = rest.postXML ("event", "<request><action type=\"getdetail\" output=\"json\"><event_id>"+event_id+"</event_id></action></request>");

                if (error == JIRest.OK) {
                    try {
                        JSONArray json = new JSONArray (rest.getResult());
                        JSONObject json_event = json.getJSONObject(0);

                        //  Update event details
                        DataHelper dh = DataHelper.getInstance();
                        dh.updateEvent (event_id, json_event);
                    } catch (JSONException e) { }
                }

                runOnUiThread(new Runnable() {
                    public void run() {
                        displayDetails (event_id);
                    }
                });

                // Remove progress bar
                displayProgressBar (false);
            }

        }.start();
    }


    public void onClick(View v) {
        if (v == findViewById(R.id.ButtonEventDetailsViewComments)) {
            // Display event comments activity
            Intent myIntent = new Intent ();
            myIntent.setClass(getBaseContext(), EventComments.class);
            myIntent.putExtra("eventJSON", getIntent().getStringExtra("eventJSON"));
            startActivity(myIntent);
        }
        if (v == findViewById(R.id.ButtonEventDetailsViewTalks)) {
            // Display talks activity
            Intent myIntent = new Intent ();
            myIntent.setClass(getBaseContext(), EventTalks.class);
            myIntent.putExtra("eventJSON", getIntent().getStringExtra("eventJSON"));
            startActivity(myIntent);
        }
        if (v == findViewById(R.id.ButtonEventDetailsViewTracks)) {
            // Display talks activity
            Intent myIntent = new Intent ();
            myIntent.setClass(getBaseContext(), EventTracks.class);
            myIntent.putExtra("eventJSON", getIntent().getStringExtra("eventJSON"));
            startActivity(myIntent);
        }
        if (v == findViewById(R.id.CheckBoxEventDetailsAttending)) {
            // Check box clicked. This will toggle if we are attending the event or not.
            new Thread() {
                // We run in a background thread
                public void run() {
                    // Display progress bar (@TODO: Check if this works since it's not a UI thread)
                    displayProgressBar (true);

                    // Fetch state of checkbox (on or off)
                    CheckBox cb = (CheckBox)findViewById(R.id.CheckBoxEventDetailsAttending);
                    // Tell joind.in API that we attend (or unattended) this event
                    final String result = attendEvent(cb.isChecked());

                    // Display result, must be done in UI thread
                    runOnUiThread(new Runnable() {
                        public void run() {
                            // Display result from attendEvent
                            Toast toast = Toast.makeText(getBaseContext(), result, Toast.LENGTH_LONG);
                            toast.show ();
                        }
                    });

                    // Stop displaying progress bar
                    displayProgressBar (false);
                }
            }.start();
        }
    };


    // This function will send to joind.in if we attend (or unattended) specified event.
    private String attendEvent (boolean initialState) {
        String result;

        // Get the event ID.
        int eventID = this.eventJSON.optInt("ID");

        // Send data to the joind.in API
        JIRest rest = new JIRest (EventDetail.this);
        int error = rest.postXML ("event", "<request>"+JIRest.getAuthXML(this)+"<action type=\"attend\" output=\"json\"><eid>"+eventID+"</eid></action></request>");

        if (error == JIRest.OK) {
            try {
                    // When the API returns something, check if it's JSON. If so
                    // we parse the MSG key from it since it will be our value.
                    JSONObject json = new JSONObject(rest.getResult());
                    result = json.optString("msg");
            } catch (Exception e) {
                    // Incorrect JSON, just return plain result from http
                    result = rest.getResult();
            }
        } else {
            // Incorrect result, return error
            result = String.format(getString(R.string.generatelAttendingError), rest.getError());
        }

        // Check what the result was..
        if (result.compareTo("Success") == 0) {
            // Everything went as expected
            if (initialState) {
                result = getString(R.string.generalSuccessFullyAttended);
            } else {
                result = getString(R.string.generalSuccessFullyUnAttended);
            }

            // We update the event, since the even has been changed (1 more attendee)
            error = rest.postXML("event", "<request>"+JIRest.getAuthXML(this)+"<action type=\"getdetail\" output=\"json\"><event_id>"+eventID+"</event_id></action></request>");
            if (error == JIRest.OK) {
                try {
                    JSONArray json = new JSONArray(rest.getResult());
                    JSONObject json_event = json.getJSONObject(0);
                    // Update the event in the database
                    DataHelper dh = DataHelper.getInstance();
                    dh.updateEvent (eventID, json_event);
                } catch (JSONException e) { 
                    // Ignored
                }
            }
        } else {
            // No correct output from the API. Something went wrong
            result = String.format(getString(R.string.generatelAttendingError), result);
        }
        return result;
     }

}

