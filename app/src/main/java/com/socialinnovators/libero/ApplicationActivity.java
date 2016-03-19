package com.socialinnovators.libero;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.thalmic.myo.AbstractDeviceListener;
import com.thalmic.myo.Arm;
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

public class ApplicationActivity extends AppCompatActivity {

    private TextView mTextView;
    private TextView minTimer;
    private ViewFlipper viewFlipper;

    private int count;
    private boolean running = false;
    private String STATE;
    private float previous;
    private float max = 40;
    private float min =  -40;

    private Socket mSocket;
    {
        try {
            mSocket = IO.socket("http://52.90.242.84:3000");
            //mSocket = IO.socket("http://chat.socket.io");

        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    // Classes that inherit from AbstractDeviceListener can be used to receive events from Myo devices.
    // If you do not override an event, the default behavior is to do nothing.
    private DeviceListener mListener = new AbstractDeviceListener() {

        // onConnect() is called whenever a Myo has been connected.
//        @Override
//        public void onConnect(Myo myo, long timestamp) {
//            // Set the text color of the text view to cyan when a Myo connects.
//            mTextView.setTextColor(Color.CYAN);
//        }
//
//        // onDisconnect() is called whenever a Myo has been disconnected.
//        @Override
//        public void onDisconnect(Myo myo, long timestamp) {
//            // Set the text color of the text view to red when a Myo disconnects.
//            mTextView.setTextColor(Color.RED);
//        }

        // onArmSync() is called whenever Myo has recognized a Sync Gesture after someone has put it on their
        // arm. This lets Myo know which arm it's on and which way it's facing.
        @Override
        public void onArmSync(Myo myo, long timestamp, Arm arm, XDirection xDirection) {
            mTextView.setText(myo.getArm() == Arm.LEFT ? R.string.arm_left : R.string.arm_right);
        }

        // onArmUnsync() is called whenever Myo has detected that it was moved from a stable position on a person's arm after
        // it recognized the arm. Typically this happens when someone takes Myo off of their arm, but it can also happen
        // when Myo is moved around on the arm.
        @Override
        public void onArmUnsync(Myo myo, long timestamp) {
            mTextView.setText(R.string.hello_world);
        }



        // onOrientationData() is called whenever a Myo provides its current orientation,
        // represented as a quaternion.
        @Override
        public void onOrientationData(Myo myo, long timestamp, Quaternion rotation) {
            // Calculate Euler angles (roll, pitch, and yaw) from the quaternion.
            float roll = (float) Math.toDegrees(Quaternion.roll(rotation));
            float pitch = (float) Math.toDegrees(Quaternion.pitch(rotation));
            float yaw = (float) Math.toDegrees(Quaternion.yaw(rotation));



            if (STATE == "down" && yaw-previous > 40 && running) {
                count++;
                //mSocket.emit("msg", count + ":" + myo.getName());
                myo.vibrate(Myo.VibrationType.LONG);
            }

            if (yaw-previous > max) {
                STATE = "up";
            }
            else if (yaw-previous < min) {
                STATE = "down";
            }


            // Adjust roll and pitch for the orientation of the Myo on the arm.
            if (myo.getXDirection() == XDirection.TOWARD_ELBOW) {
                roll *= -1;
                pitch *= -1;
            }

            previous = yaw;
            // Next, we apply a rotation to the text view using the roll, pitch, and yaw.
//            mTextView.setRotation(roll);
//            mTextView.setRotationX(pitch);
//            mTextView.setRotationY(yaw);
            mTextView.setText(Integer.toString(count));
            Log.d("Yaw", Float.toString(yaw));
            Log.d("Count", Integer.toString(count));
            Log.d("Running", Boolean.toString(running));
        }

        // onPose() is called whenever a Myo provides a new pose.
        @Override
        public void onPose(Myo myo, long timestamp, Pose pose) {
            // Handle the cases of the Pose enumeration, and change the text of the text view
            // based on the pose we receive.
            switch (pose) {
                case UNKNOWN:
                    mTextView.setText(getString(R.string.hello_world));
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
                    mTextView.setText(getString(restTextId));
                    break;
                case FIST:
                    mTextView.setText(getString(R.string.pose_fist));
                    running = false;
                    break;
                case WAVE_IN:
                    mTextView.setText(getString(R.string.pose_wavein));
                    mSocket.emit("msg", count + ":" + myo.getName());
                    break;
                case WAVE_OUT:
                    mTextView.setText(getString(R.string.pose_waveout));
                    break;
                case FINGERS_SPREAD:
                    if(viewFlipper.getDisplayedChild() == viewFlipper.indexOfChild(findViewById(R.id.instruction))) {
                        myo.vibrate(Myo.VibrationType.LONG);
                        running = true;
                        viewFlipper.setDisplayedChild(viewFlipper.indexOfChild(findViewById(R.id.timer)));
                        countdown();
                    }
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
                myo.unlock(Myo.UnlockType.TIMED);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.applicationactivity);

        mSocket.on(Socket.EVENT_CONNECT_ERROR, onConnectError);
        mSocket.on(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);
        mSocket.on("msg", onResponse);
        mSocket.connect();

        viewFlipper = (ViewFlipper) findViewById(R.id.viewflipper);
        mTextView = (TextView) findViewById(R.id.progressview);

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
                    Log.d("user: ", user);
                    Log.d("count: ", count);
                    Toast.makeText(getApplicationContext(), count + ":" + user, Toast.LENGTH_LONG).show();
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

    }

    private void DisplayResults() {
        // TODO: Display tabulated results from server


    }

    private void countdown() {
        minTimer=(TextView)findViewById(R.id.minTimer);

        new CountDownTimer(60000, 1000) {
            public void onTick(long millisUntilFinished) {
                minTimer.setText("Seconds remaining: " + millisUntilFinished / 1000);
            }
            public void onFinish() {
                minTimer.setText("Done!");
            }

        }.start();
    }
}
