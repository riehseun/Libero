package com.socialinnovators.libero;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.facebook.FacebookSdk;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.thalmic.myo.AbstractDeviceListener;
import com.thalmic.myo.DeviceListener;
import com.thalmic.myo.Hub;
import com.thalmic.myo.Myo;
import com.thalmic.myo.Pose;
import com.thalmic.myo.Quaternion;
import com.thalmic.myo.XDirection;
import com.thalmic.myo.scanner.ScanActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;



public class ApplicationActivity extends AppCompatActivity {

    private TextView minTimer;
    private TextView minTimerSecs;
    private TextView counterView;
    private TextView opprogress;
    private ViewFlipper viewFlipper;

    private TextView opponent;
    private boolean running;
    private int count = 0;
    private String opCount;

    private TextView youRep;
    private TextView opRep;

    private ImageView win;
    private ImageView lose;
    private ImageView tie;

    // Roll Parameter
    private float rollLocalMax;
    private float rollLocalMin;
    private final float lowRange = 70.0f;
    private float prevRoll;
    private String prevRollState;
    private String currentRollState;

    // Y Paramater
    private float prevY;
    private final float yLimit = 0.05f;

    private static List<String> yaws = new ArrayList<>();
    private static List<String> pitches = new ArrayList<>();
    private static List<String> rolls = new ArrayList<>();

    private Socket mSocket;
    {
        try {
            mSocket = IO.socket("http://52.90.242.84:3000");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    // Classes that inherit from AbstractDeviceListener can be used to receive events from Myo devices.
    // If you do not override an event, the default behavior is to do nothing.
    private DeviceListener mListener = new AbstractDeviceListener() {
        /*
        @Override
        public void onAccelerometerData (Myo myo, long timestamp, Vector3 accelerometer) {
            float x = (float)accelerometer.x();
            float y = (float)accelerometer.y();
            float z = (float)accelerometer.z();

            System.out.println(running);

            if (y < 0 && prevY > 0) {

            }
            if (y > 0 && prevY < 0 && Math.abs(y) > yLimit) {
                mSocket.emit("msg", count + ":" + myo.getName());
                myo.vibrate(Myo.VibrationType.LONG);
                count++;
            }
            prevY = y;

            counterView.setText(Integer.toString(count));

            xs.add(Float.toString(x));
            ys.add(Float.toString(y));
            zs.add(Float.toString(z));
        }
        */

        // onOrientationData() is called whenever a Myo provides its current orientation,
        // represented as a quaternion.
        @Override
        public void onOrientationData(Myo myo, long timestamp, Quaternion rotation) {
            // Calculate Euler angles (roll, pitch, and yaw) from the quaternion.
            float roll = (float) Math.toDegrees(Quaternion.roll(rotation));
            float pitch = (float) Math.toDegrees(Quaternion.pitch(rotation));
            float yaw = (float) Math.toDegrees(Quaternion.yaw(rotation));

            // Calculate increasing/decreasing for Roll and Yaw
            if (roll > prevRoll) {
                currentRollState = "roll increasing";
            }
            if (roll < prevRoll) {
                currentRollState = "roll decreasing";
            }

            // Calculate local min/max for Roll and Yaw
            if (currentRollState == "roll increasing" && prevRollState == "roll decreasing") {
                rollLocalMin = roll;
                //System.out.println("Local Min/Max Diff: " + Math.abs(rollLocalMin-rollLocalMax));
                //System.out.println(yAccel);
                if (Math.abs(rollLocalMin-rollLocalMax) > lowRange && running == true) {
                    mSocket.emit("msg", count + ":" + myo.getName());
                    myo.vibrate(Myo.VibrationType.LONG);
                    count++;
                }
            }
            if (currentRollState == "roll decreasing" && prevRollState == "roll increasing") {
                rollLocalMax = roll;
            }

            // Set states for Roll and Yaw
            prevRoll = roll;
            prevRollState = currentRollState;

            // Adjust roll and pitch for the orientation of the Myo on the arm.
            if (myo.getXDirection() == XDirection.TOWARD_ELBOW) {
                roll *= -1;
                pitch *= -1;
            }

            counterView.setText(Integer.toString(count));

            yaws.add(Float.toString(yaw));
            pitches.add(Float.toString(pitch));
            rolls.add(Float.toString(roll));
        }

        // onPose() is called whenever a Myo provides a new pose.
        @Override
        public void onPose(Myo myo, long timestamp, Pose pose) {
            // Handle the cases of the Pose enumeration, and change the text of the text view
            // based on the pose we receive.
            switch (pose) {
                case UNKNOWN:
                    break;
                case REST:
                case DOUBLE_TAP:
                    int restTextId = R.string.hello_world;
                    switch (myo.getArm()) {
                        case LEFT:
                            restTextId = R.string.arm_left;
                            break;
                        case RIGHT:
                            restTextId = R.string.arm_right;
                            break;
                    }

                    break;
                case FIST:
                    /*
                    for (String a : yaws) {
                        System.out.println("yaw: " + a);
                    }
                    for (String b : pitches) {
                        System.out.println("pitch: " + b);
                    }
                    */
                    for (String c: rolls) {
                        System.out.println("roll: " + c);
                    }
                    /*
                    for (String x : xs) {
                        System.out.println("x: " + x);
                    }
                    for (String y : ys) {
                        System.out.println("y: " + y);
                    }
                    for (String z : zs) {
                        System.out.println("z: " + z);
                    }
                    System.out.println(count);
                    */
                    break;
                case WAVE_IN:

                    break;
                case WAVE_OUT:

                    break;
                case FINGERS_SPREAD:
                    break;
            }

            if (pose != Pose.UNKNOWN && pose != Pose.REST) {
                // Tell the Myo to stay unlocked until told otherwise. We do that here so you can
                // hold the poses without the Myo becoming locked.
                myo.unlock(Myo.UnlockType.HOLD);

                // Notify the Myo that the pose has resulted in an action, in this case changing
                // the text on the screen. The Myo will vibrate.
                myo.notifyUserAction();
            } else {
                // Tell the Myo to stay unlocked only for a short period. This allows the Myo to
                // stay unlocked while poses are being performed, but lock after inactivity.
                myo.unlock(Myo.UnlockType.HOLD);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.applicationactivity);

        //Initialize Facebook SDK
        FacebookSdk.sdkInitialize(getApplicationContext());

        mSocket.on(Socket.EVENT_CONNECT_ERROR, onConnectError);
        mSocket.on(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);
        mSocket.on("msg", onResponse);
        mSocket.connect();

        viewFlipper = (ViewFlipper) findViewById(R.id.viewflipper);
        counterView = (TextView) findViewById(R.id.progressview);
        minTimerSecs = (TextView) findViewById(R.id.minTimerSecs);
        counterView.setId(android.R.id.mask);
        opprogress = (TextView) findViewById(R.id.opProgressview);
        // First, we initialize the Hub singleton with an application identifier.
        Hub hub = Hub.getInstance();
        if (!hub.init(this, getPackageName())) {
            // We can't do anything with the Myo device if the Hub can't be initialized, so exit.
            Toast.makeText(this, "Couldn't initialize Hub", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Disable standard Myo locking policy. All poses will be delivered.
        hub.setLockingPolicy(Hub.LockingPolicy.NONE);

        // Next, register for DeviceListener callbacks.
        hub.addListener(mListener);
    }

    private Emitter.Listener onConnectError = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "Unable to connect to NodeJS server", Toast.LENGTH_LONG).show();
                }
            });
        }
    };

    private Emitter.Listener onResponse = new Emitter.Listener() {
        @Override
        public void call(final Object...args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    String user;
                    String count;
                    try {
                        user = data.getString("users");
                        count = data.getString("counts");
                    } catch (JSONException e) {
                        return;
                    }
                    //Toast.makeText(getApplicationContext(), user + " has " + count + " push up(s)", Toast.LENGTH_LONG).show();
                    opprogress=(TextView)findViewById(R.id.opProgressview);
                    opprogress.setText(count + " Repetitions");

                    opCount = count;
                }
            });
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mSocket.off(Socket.EVENT_CONNECT_ERROR, onConnectError);
        mSocket.off(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);
        mSocket.off("msg", onResponse);
        mSocket.disconnect();

        // We don't want any callbacks when the Activity is gone, so unregister the listener.
        Hub.getInstance().removeListener(mListener);

        if (isFinishing()) {
            // The Activity is finishing, so shutdown the Hub. This will disconnect from the Myo.
            Hub.getInstance().shutdown();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (R.id.action_scan == id) {
            onScanActionSelected();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void onScanActionSelected() {
        // Launch the ScanActivity to scan for Myos to connect to.
        Intent intent = new Intent(this, ScanActivity.class);
        startActivity(intent);
    }


    public void GetStarted(View view) {
        viewFlipper.showNext();
        countdown();
        running = true;

    }

    private void DisplayResults() {

        if (opCount != null){
            int opcount = Integer.parseInt(opCount);
            if (opcount < count)        win= (ImageView) findViewById(R.id.win);
            else if (opcount == count) {
                win = (ImageView) findViewById(R.id.tie);
            }
            else if (opcount > count)         win= (ImageView) findViewById(R.id.lose);

            youRep= (TextView) findViewById(R.id.youRep);
            //youRep = counterView;
            final int finalCount = count;
            youRep.setText(count + " Repetitions");

            opRep= (TextView) findViewById(R.id.opRep);
            //youRep = counterView;
            final int finalOpcount = opcount;
            opRep.setText(opcount + " Repetitions");
        }

        youRep= (TextView) findViewById(R.id.youRep);
        //youRep = counterView;
        youRep.setText(count + " Repetitions");


        //leaderboard();
    }


    public void GoHome(View view) {
        viewFlipper.setDisplayedChild(viewFlipper.indexOfChild(findViewById(R.id.start)));

    }

    private void countdown() {
        minTimer=(TextView)findViewById(R.id.minTimer);

        new CountDownTimer(4000, 1000) {
            public void onTick(long millUntilFinished) {
                minTimer.setText("Begin in: " + (millUntilFinished / 1000));
            }
            public void onFinish() {
                minTimer.setText("Seconds remaining: ");
                new CountDownTimer(10000, 1000) {
                    public void onTick(long millisUntilFinished) {
                        minTimerSecs.setText(Long.toString(millisUntilFinished/1000));
                    }
                    public void onFinish() {
                        minTimer.setText("Done!");
                        minTimerSecs.setText(null);
                        viewFlipper.showNext();
                        DisplayResults();
                    }

                }.start();
            }

        }.start();
    }
}
