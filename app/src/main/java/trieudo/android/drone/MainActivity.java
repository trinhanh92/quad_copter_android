package trieudo.android.drone;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity implements OnControlListener {
    private TextView tvX;
    private TextView tvY;
    private TextView tvZ;
    private Button btControl;

    private XYController xyController;
    //private ZController zController;

    private int xPercentage;
    private int yPercentage;
    private int zPercentage;
    private boolean controlValueChanged;
    private boolean previousCommandSent;

    private Timer timerSendCommand;
    private TimerTask timerTaskSendCommand;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // set background image for app
        RelativeLayout layoutMain = (RelativeLayout) this.findViewById(R.id.LO_Main);
        layoutMain.setBackgroundResource(R.drawable.bg);

        // get views
        this.tvX = (TextView) this.findViewById(R.id.TV_X);
        this.tvY = (TextView) this.findViewById(R.id.TV_Y);
        this.tvZ = (TextView) this.findViewById(R.id.TV_Z);
        this.btControl = (Button) this.findViewById(R.id.BT_CTRL);

        // set the Control button as "Start" when clicked
        this.btControl.setBackgroundColor(Color.GREEN);
        // register OnClick event for this button
        this.btControl.setOnClickListener(this.btControlClickListener);

        // reset percentage of headings
        this.xPercentage = 0;
        this.yPercentage = 0;
        this.zPercentage = 0;
        this.controlValueChanged = true;

        // initialize XY controller
        this.xyController = new XYController(this);
        this.xyController.setControlChangeListener(this.xyControlChangeListener);
        final CustomControl control = (CustomControl) findViewById(R.id.gasControlContainer);
        control.setControlListener(this);

        findViewById(R.id.switch_map).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                zPercentage = 0;
                control.updateProgress(0);
                Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                startActivity(intent);
            }
        });

        // initialize Z controller
        //this.zController = new ZController(this);
        //this.zController.setControlChangeListener(this.zControlChangeListener);
    }

    private XYController.ControlChangeListener xyControlChangeListener = new XYController.ControlChangeListener() {
        @Override
        public void ControlChanged(int xPercentage, int yPercentage) {
            // recheck whether control values changed
            if ((xPercentage != MainActivity.this.xPercentage) ||
                    (yPercentage != MainActivity.this.yPercentage)) {
                // update control values to screen
                tvX.setText("X = " + xPercentage + "%");
                tvY.setText("Y = " + yPercentage + "%");
                // store the changed control values
                MainActivity.this.xPercentage = xPercentage;
                MainActivity.this.yPercentage = yPercentage;
                MainActivity.this.controlValueChanged = true;
            }
        }
    };

    private ZController.ControlChangeListener zControlChangeListener = new ZController.ControlChangeListener() {
        @Override
        public void ControlChanged(int zPercentage) {
            if (zPercentage != MainActivity.this.zPercentage) {
                tvZ.setText("Z = " + zPercentage + "%");
                MainActivity.this.zPercentage = zPercentage;
                MainActivity.this.controlValueChanged = true;
            }
        }
    };

    private View.OnClickListener btControlClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            // check whether the control was started
            if (String.valueOf(btControl.getText()).compareTo("Start") == 0) {
                // initialize timer in order to check and send command (if any) every 10ms
                previousCommandSent = true;
                timerSendCommand = new Timer();
                timerTaskSendCommand = new TimerTaskSendCommand();
                timerSendCommand.schedule(timerTaskSendCommand, 0, 500);
                // change the Control button as "Stop" when clicked
                btControl.setText("Stop");
                btControl.setBackgroundColor(Color.RED);
            }
            // if the control was stopped
            else {
                // stop timer, stop sending command to the Drone
                timerSendCommand.cancel();
                // change the Control button as "Start" when clicked
                btControl.setText("Start");
                btControl.setBackgroundColor(Color.GREEN);
            }
        }
    };

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onProgressing(int percent) {
        tvZ.setText(String.format(Locale.getDefault(),"Z = %d", percent) + "%");
        if(percent != zPercentage) {
            zPercentage = percent;
            controlValueChanged = true;
        }
    }

    @Override
    public void onEnd() {

    }

    private class TimerTaskSendCommand extends TimerTask {
        private Handler handler;

        public TimerTaskSendCommand() {
            this.handler = new Handler();
        }

        @Override
        public void run() {
            handler.post(new Runnable() {
                private HttpPostTask taskMoveDrone;

                public void run() {
                    // check whether the previous command was sent and the control values was changed
                    if ((previousCommandSent == true) && (controlValueChanged)) {
                        previousCommandSent = false;
                        controlValueChanged = false;
                        // start timer, start to check and send control values to the Drone
                        this.taskMoveDrone = new HttpPostTask(MainActivity.this, getResources().getString(R.string.ServerAddressMove), 3);
                        this.taskMoveDrone.setTaskCompletionListener(taskMoveDroneCompletionListener);
                        this.taskMoveDrone.execute("x", String.valueOf(xPercentage), "y", String.valueOf(yPercentage), "z", String.valueOf(zPercentage));
                    }
                }
            });
        }
    }

    private HttpPostTask.TaskCompletionListener taskMoveDroneCompletionListener = new HttpPostTask.TaskCompletionListener() {
        @Override
        public void Completed(HttpPostTask.TaskStatus status) {
            switch (status) {
                case Successful:
                    // release mutex for sending another command to the Drone
                    previousCommandSent = true;
                    break;
                case IncorrectInfo:
                    alert("Incorrect Information", "The response from the Drone does not contain the required information");
                    // stop control
                    btControl.performClick();
                    break;
                case UnableToConnect:
                    alert("Drone Connection", "Unable to connect to the Drone. Please check the wifi connection and retry");
                    // stop control
                    btControl.performClick();
                    break;
            }
        }
    };

    private void alert(String title, String message) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle(title);
        alertDialog.setMessage(message);
        alertDialog.setIcon(android.R.drawable.ic_dialog_alert);
        alertDialog.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        alertDialog.show();
    }
}
