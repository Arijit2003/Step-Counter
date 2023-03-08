package com.example.stepcounter;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuPopupHelper;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.tv.TvContentRating;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.mikhaellopez.circularprogressbar.CircularProgressBar;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
    TextView calBurn;
    private long pauseOffSet;

    double calories=0.0;
    //1--KCAL &&  0--CAL
    int energyFlag=0;

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
                Toast.makeText(MainActivity.this, "Long tap to set the goal", Toast.LENGTH_SHORT).show();
            }
        });

        setGoalTV.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                openTheDialog();
                return false;
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

        calBurn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PopupMenu popupMenu=new PopupMenu(MainActivity.this,calBurn);
                popupMenu.inflate(R.menu.walking_mode);
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @SuppressLint("UseCompatLoadingForDrawables")
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        switch (menuItem.getItemId()){
                            case R.id.kcalMenuItem:{
                                Drawable image=getDrawable(R.drawable.kilocalories);
                                image.setBounds(0,0,image.getIntrinsicWidth(),image.getIntrinsicHeight());

                                Drawable imageDropDown=getDrawable(R.drawable.drop_down);
                                imageDropDown.setBounds(0,0,imageDropDown.getIntrinsicWidth(),imageDropDown.getIntrinsicHeight());

                                calBurn.setCompoundDrawables(image,null,imageDropDown,null);
                                Toast.makeText(MainActivity.this, "Energy unit: kcal", Toast.LENGTH_SHORT).show();
                                popupMenu.dismiss();
                                energyFlag=1;
                                storeInSharedPreferences();
                                break;
                            }
                            case R.id.calorieMenuItem:{
                                Drawable image=getDrawable(R.drawable.calorie);
                                image.setBounds(0,0,image.getIntrinsicWidth(),image.getIntrinsicHeight());

                                Drawable imageDropDown=getDrawable(R.drawable.drop_down);
                                imageDropDown.setBounds(0,0,imageDropDown.getIntrinsicWidth(),imageDropDown.getIntrinsicHeight());

                                calBurn.setCompoundDrawables(image,null,imageDropDown,null);
                                Toast.makeText(MainActivity.this, "Energy unit: cal", Toast.LENGTH_SHORT).show();
                                popupMenu.dismiss();
                                energyFlag=0;
                                storeInSharedPreferences();
                                break;
                            }

                        }

                        return true;
                    }
                });
                popupMenu.show();

                //Below try catch block is used to force menu icon to appear with the text.
                try {
                    Field[] fields = popupMenu.getClass().getDeclaredFields();
                    for (Field field : fields) {
                        if ("mPopup".equals(field.getName())) {
                            field.setAccessible(true);
                            Object menuPopupHelper = field.get(popupMenu);
                            Class<?> classPopupHelper = Class.forName(menuPopupHelper
                                    .getClass().getName());
                            Method setForceIcons = classPopupHelper.getMethod(
                                    "setForceShowIcon", boolean.class);
                            setForceIcons.invoke(menuPopupHelper, true);
                            break;
                        }
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
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
        calBurn=findViewById(R.id.calBurn);
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
            if(energyFlag==0) {
                calories = distanceInKM * 62.1372;
            }
            else{
                calories = distanceInKM * 62.1372/1000;
            }
            calBurn.setText(new DecimalFormat("##.##").format(calories));
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
        editor.putInt("ENERGY_FLAG",energyFlag);
        editor.apply();

    }


    @SuppressLint("SetTextI18n")
    public void retrieveFromSharedPreferences(){
        SharedPreferences sp=getSharedPreferences("STEP_RECORD",MODE_PRIVATE);
        if(sp.contains("TOTAL_STEPS")){
            totalStep=sp.getInt("TOTAL_STEPS",-1);
            double evaluateDist=totalStep*2.4*0.3048/1000;
            energyFlag=sp.getInt("ENERGY_FLAG",0);
            if(energyFlag==0) {
                calories = evaluateDist * 62.1372;

                Drawable image=getDrawable(R.drawable.calorie);
                image.setBounds(0,0,image.getIntrinsicWidth(),image.getIntrinsicHeight());
                Drawable imageDropDown=getDrawable(R.drawable.drop_down);
                imageDropDown.setBounds(0,0,imageDropDown.getIntrinsicWidth(),imageDropDown.getIntrinsicHeight());
                calBurn.setCompoundDrawables(image,null,imageDropDown,null);
            }
            else{
                calories = evaluateDist * 62.1372/1000;

                Drawable image=getDrawable(R.drawable.kilocalories);
                image.setBounds(0,0,image.getIntrinsicWidth(),image.getIntrinsicHeight());
                Drawable imageDropDown=getDrawable(R.drawable.drop_down);
                imageDropDown.setBounds(0,0,imageDropDown.getIntrinsicWidth(),imageDropDown.getIntrinsicHeight());
                calBurn.setCompoundDrawables(image,null,imageDropDown,null);

            }
            calBurn.setText(new DecimalFormat("##.##").format(calories));
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
        editor.putInt("ENERGY_FLAG",0);
        editor.apply();

        Drawable image=getDrawable(R.drawable.calorie);
        image.setBounds(0,0,image.getIntrinsicWidth(),image.getIntrinsicHeight());
        Drawable imageDropDown=getDrawable(R.drawable.drop_down);
        imageDropDown.setBounds(0,0,imageDropDown.getIntrinsicWidth(),imageDropDown.getIntrinsicHeight());
        calBurn.setCompoundDrawables(image,null,imageDropDown,null);

        circularProgressBar.setProgressWithAnimation((float) (totalStep));
        circularProgressBar.setProgressMax((float) 2600);
        stepsTV.setText(""+(int)(totalStep)+"");
        setGoalTV.setText("2600");
        //Chronometer
        timeInMinChrono.setBase(SystemClock.elapsedRealtime());
        pauseOffSet=0;
        calories=0.0;
        calBurn.setText(new DecimalFormat("##.##").format(calories));
        energyFlag=0;

    }


    @SuppressLint("SetTextI18n")
    public void resetData(int Goal){
        SharedPreferences sp=getSharedPreferences("STEP_RECORD",MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        totalStep=0;
        editor.putInt("TOTAL_STEPS",totalStep);
        editor.putInt("setGoal",Goal);
        editor.putInt("ENERGY_FLAG",0);
        editor.apply();

        Drawable image=getDrawable(R.drawable.calorie);
        image.setBounds(0,0,image.getIntrinsicWidth(),image.getIntrinsicHeight());
        Drawable imageDropDown=getDrawable(R.drawable.drop_down);
        imageDropDown.setBounds(0,0,imageDropDown.getIntrinsicWidth(),imageDropDown.getIntrinsicHeight());
        calBurn.setCompoundDrawables(image,null,imageDropDown,null);

        distance.setText(new DecimalFormat("##.##").format(0.0));
        circularProgressBar.setProgressWithAnimation((float) (totalStep));
        circularProgressBar.setProgressMax((float) Goal);
        stepsTV.setText(""+(int)(totalStep)+"");
        setGoalTV.setText(""+Goal+"");
        //Chronometer
        timeInMinChrono.setBase(SystemClock.elapsedRealtime());
        pauseOffSet=0;
        calories=0.0;
        calBurn.setText(new DecimalFormat("##.##").format(calories));
        energyFlag=0;

    }
}