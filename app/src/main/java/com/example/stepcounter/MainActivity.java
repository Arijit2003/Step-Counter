package com.example.stepcounter;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.mikhaellopez.circularprogressbar.CircularProgressBar;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    CircularProgressBar circularProgressBar;
    TextView stepsTV;
    TextView setGoalTV;
    Button startStop;
    SensorManager sensorManager;
    Sensor stepCounterSensor;
    Boolean sensorActivation=false;
    int currentStep;
    int previousStep=0;
    int totalStep;

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

                    if(stepCounterSensor!=null){
                        sensorManager.unregisterListener(MainActivity.this,stepCounterSensor);
                    }

                }
                else{
                    sensorActivation=true;
                    state="STOP";
                    startStop.setText(state);

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
                resetData();
            }
        });



    }




    public void initialize(){
        circularProgressBar=findViewById(R.id.circularProgressBar);
        stepsTV=findViewById(R.id.stepsTV);
        setGoalTV=findViewById(R.id.setGoalTV);
        startStop=findViewById(R.id.startStop);
        sensorManager=(SensorManager) getSystemService(SENSOR_SERVICE);
        if(sensorManager!=null){
            stepCounterSensor=sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
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
        if(sensorEvent.sensor.getType()==Sensor.TYPE_STEP_COUNTER){

            currentStep= (int) sensorEvent.values[0];
            totalStep=currentStep-previousStep;
            String values=""+totalStep+"";
            stepsTV.setText(values);
            circularProgressBar.setProgress((float) totalStep);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
    void showAlertDialog(String title,String message){
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
        editor.putInt("currentStep", currentStep);
        editor.putInt("previousStep",previousStep);
        editor.putInt("setGoal",Integer.parseInt(setGoalTV.getText().toString()));
        editor.apply();

    }
    @SuppressLint("SetTextI18n")
    public void retrieveFromSharedPreferences(){
        SharedPreferences sp=getSharedPreferences("STEP_RECORD",MODE_PRIVATE);
        if(sp.contains("currentStep")){
            currentStep=sp.getInt("currentStep",-1);
            previousStep=sp.getInt("previousStep",-1);

            setGoalTV.setText(""+sp.getInt("setGoal",2600)+"");
            circularProgressBar.setProgress((float)(currentStep-previousStep));
            circularProgressBar.setProgressMax((float) sp.getInt("setGoal",2600));
            stepsTV.setText(""+(currentStep-previousStep)+"");
        }
    }
    @SuppressLint("SetTextI18n")
    public void resetData(){
        SharedPreferences sp=getSharedPreferences("STEP_RECORD",MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        previousStep=currentStep;
        editor.putInt("currentStep",currentStep);
        editor.putInt("previousStep",previousStep);
        editor.putInt("setGoal",2600);
        editor.apply();
        circularProgressBar.setProgress((float) (currentStep-previousStep));
        stepsTV.setText(""+(int)(currentStep-previousStep)+"");
        setGoalTV.setText("2600");
    }
    @SuppressLint("SetTextI18n")
    public void resetData(int Goal){
        SharedPreferences sp=getSharedPreferences("STEP_RECORD",MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        previousStep=currentStep;
        editor.putInt("currentStep",currentStep);
        editor.putInt("previousStep",previousStep);
        editor.putInt("setGoal",Goal);
        editor.apply();
        circularProgressBar.setProgress((float) (currentStep-previousStep));
        stepsTV.setText(""+(int)(currentStep-previousStep)+"");
        setGoalTV.setText(""+Goal+"");
    }
}