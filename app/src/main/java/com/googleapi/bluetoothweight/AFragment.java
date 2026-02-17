package com.googleapi.bluetoothweight;

import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;


import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.Arrays;


public class AFragment extends Fragment {


    private AppCompatButton button4a,button5a;
    private TextView txtDisplayTwoView;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //return inflater.inflate(R.layout.fragment_train_main, container, false);
        View view = inflater.inflate(R.layout.fragment_a, container, false);
       // button1a = view.findViewById(R.id.button1a);
       // button2a = view.findViewById(R.id.button2a);
       // button3a = view.findViewById(R.id.button3a);
        button4a = view.findViewById(R.id.button4a);
        button5a = view.findViewById(R.id.button5a);

        //Retrieve the data entered in the edit texts
        MainActivity mainActivity = (MainActivity) getActivity();



        button4a.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                setMainTextViewData(txtDisplayTwoView, "+4");
                byte[] txBuffer = new byte[8];
                Arrays.fill(txBuffer, (byte) 0);

                // Prepare main controller data
                txBuffer[0] =  (byte) '$';
                txBuffer[1] = (byte) 'B';
                txBuffer[2] = (byte) 'T';
                txBuffer[3] = (byte) '1';
                txBuffer[4] = (byte) 'S';
                txBuffer[5] = (byte) '+';
                txBuffer[6] = (byte) '4';
                txBuffer[7] = (byte) ';';

                mainActivity.bluetoothManager.sendControllerData(txBuffer);
            }
        });

        button5a.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                setMainTextViewData(txtDisplayTwoView, "+5");
                byte[] txBuffer = new byte[8];
                Arrays.fill(txBuffer, (byte) 0);

                // Prepare main controller data
                txBuffer[0] =  (byte) '$';
                txBuffer[1] = (byte) 'B';
                txBuffer[2] = (byte) 'T';
                txBuffer[3] = (byte) '1';
                txBuffer[4] = (byte) 'S';
                txBuffer[5] = (byte) '+';
                txBuffer[6] = (byte) '5';
                txBuffer[7] = (byte) ';';

                mainActivity.bluetoothManager.sendControllerData(txBuffer);
            }
        });



        return view;
    }

    public void setMainTextViewData(TextView txtDisplayTwoView, String txt){
        txtDisplayTwoView.setText(txt);

    }


}
