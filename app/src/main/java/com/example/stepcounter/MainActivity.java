package com.example.stepcounter;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.tv.TvContentRating;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.mikhaellopez.circularprogressbar.CircularProgressBar;
import java.text.DecimalFormat;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    CircularProgressBar circularProgressBar;
    TextView stepsTV;
    TextView setGoalTV;
    Button startStop;
    SensorManager sensorManager;
    Sensor stepCounterSensor;
    Boolean sensorActivation=false;
    int totalStep=0;
    TextView distance;
    Chronometer timeInMinChrono;
    ConstraintLayout constraintLayout;
    private long pauseOffSet;

    ActivityResultLauncher<String> activityRecognizerResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
        @Override
        public void onActivityResult(Boolean result) {
            if(result){
                String state;
                if(sensorActivation){
                    sensorActivation=false;
                    state="START";
                    startStop.setText(state);
                    storeInSharedPreferences();
                    //chronometer
                    timeInMinChrono.stop();
                    pauseOffSet=SystemClock.elapsedRealtime()-timeInMinChrono.getBase();
                    if(stepCounterSensor!=null){
                        sensorManager.unregisterListener(MainActivity.this,stepCounterSensor);
                    }
                }
                else{
                    sensorActivation=true;
                    state="STOP";
                    startStop.setText(state);
                    //chronometer
                    timeInMinChrono.setBase(SystemClock.elapsedRealtime()-pauseOffSet);
                    timeInMinChrono.start();
                    if(stepCounterSensor!=null){
                        sensorManager.registerListener(MainActivity.this,stepCounterSensor,SensorManager.SENSOR_DELAY_NORMAL);
                    }

                }
            }
            else{
                showAlertDialog("Permission Denied","So the app can't recognize your activity");
            }
        }
    });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().setNavigationBarColor(ContextCompat.getColor(this,R.color.black));
        getWindow().setStatusBarColor(ContextCompat.getColor(this,R.color.black));

        initialize();

        setGoalTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openTheDialog();
            }
        });

        startStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activityRecognizerResultLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION);
            }
        });

        stepsTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MainActivity.this, "Tap long to reset", Toast.LENGTH_SHORT).show();
            }
        });
        stepsTV.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                resetData();
                return false;
            }
        });

    }




    public void initialize(){
        constraintLayout=findViewById(R.id.constraintLayout);
        circularProgressBar=findViewById(R.id.circularProgressBar);
        stepsTV=findViewById(R.id.stepsTV);
        setGoalTV=findViewById(R.id.setGoalTV);
        startStop=findViewById(R.id.startStop);
        distance=findViewById(R.id.distance);
        timeInMinChrono=findViewById(R.id.timeInMinChrono);
        sensorManager=(SensorManager) getSystemService(SENSOR_SERVICE);
        if(sensorManager!=null){
            stepCounterSensor=sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        }
        retrieveFromSharedPreferences();

    }


    public void openTheDialog(){
        Dialog dialog=new Dialog(this);
        dialog.setContentView(R.layout.set_goal_dialog);
        EditText dialogET=dialog.findViewById(R.id.enterGoal);
        Button dialogBtnSave=dialog.findViewById(R.id.saveBtnGoal);
        dialogBtnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!dialogET.getText().toString().equals("")) {
                    setGoalTV.setText("");
                    setGoalTV.setText(dialogET.getText().toString());
                    float maxProgress = Float.parseFloat(setGoalTV.getText().toString());
                    circularProgressBar.setProgressMax(maxProgress);
                    resetData(Integer.parseInt(dialogET.getText().toString()));
                    dialog.dismiss();
                }
                else{
                    Toast.makeText(MainActivity.this, "Fill up the text field", Toast.LENGTH_SHORT).show();
                }
            }
        });
        dialog.create();
        dialog.show();

    }


    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(sensorEvent.sensor.getType()==Sensor.TYPE_STEP_DETECTOR){
            totalStep++;
            String values=""+totalStep+"";
            double distanceInKM= totalStep*2.4*0.3048/1000;
            distance.setText(new DecimalFormat("##.##").format(distanceInKM));
            stepsTV.setText(values);
            circularProgressBar.setProgressWithAnimation((float) totalStep);
        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }


    private void showAlertDialog(String title,String message){
        AlertDialog.Builder alertDialog=new AlertDialog.Builder(this);
        alertDialog.setTitle(title);
        alertDialog.setMessage(message);
        alertDialog.setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        alertDialog.create();
        alertDialog.show();
    }


    public void storeInSharedPreferences(){
        SharedPreferences sp=getSharedPreferences("STEP_RECORD",MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt("TOTAL_STEPS",totalStep);
        editor.putInt("setGoal",Integer.parseInt(setGoalTV.getText().toString()));
        editor.apply();

    }


    @SuppressLint("SetTextI18n")
    public void retrieveFromSharedPreferences(){
        SharedPreferences sp=getSharedPreferences("STEP_RECORD",MODE_PRIVATE);
        if(sp.contains("TOTAL_STEPS")){
            totalStep=sp.getInt("TOTAL_STEPS",-1);
            double evaluateDist=totalStep*2.4*0.3048/1000;
            distance.setText(new DecimalFormat("##.##").format(evaluateDist));
            setGoalTV.setText(""+sp.getInt("setGoal",2600)+"");
            circularProgressBar.setProgressWithAnimation((float)(totalStep));
            circularProgressBar.setProgressMax((float) sp.getInt("setGoal",2600));
            stepsTV.setText(""+(totalStep)+"");

        }
    }


    @SuppressLint("SetTextI18n")
    public void resetData(){
        SharedPreferences sp=getSharedPreferences("STEP_RECORD",MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        totalStep=0;
        distance.setText(new DecimalFormat("##.##").format(0.0));
        editor.putInt("TOTAL_STEPS",totalStep);
        editor.putInt("setGoal",2600);
        editor.apply();
        circularProgressBar.setProgressWithAnimation((float) (totalStep));
        circularProgressBar.setProgressMax((float) 2600);
        stepsTV.setText(""+(int)(totalStep)+"");
        setGoalTV.setText("2600");
        //Chronometer
        timeInMinChrono.setBase(SystemClock.elapsedRealtime());
        pauseOffSet=0;
    }


    @SuppressLint("SetTextI18n")
    public void resetData(int Goal){
        SharedPreferences sp=getSharedPreferences("STEP_RECORD",MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        totalStep=0;
        editor.putInt("TOTAL_STEPS",totalStep);
        editor.putInt("setGoal",Goal);
        editor.apply();

        distance.setText(new DecimalFormat("##.##").format(0.0));
        circularProgressBar.setProgressWithAnimation((float) (totalStep));
        circularProgressBar.setProgressMax((float) Goal);
        stepsTV.setText(""+(int)(totalStep)+"");
        setGoalTV.setText(""+Goal+"");
        //Chronometer
        timeInMinChrono.setBase(SystemClock.elapsedRealtime());
        pauseOffSet=0;
    }
}