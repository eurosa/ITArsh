package com.googleapi.bluetoothweight;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;

import java.util.Arrays;

public class DFragment extends Fragment {

    private AppCompatButton button1d,button2d;
    private TextView txtDisplayTwoView;

    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // return inflater.inflate(R.layout.fragment_train_main, container, false);
        View view = inflater.inflate(R.layout.fragment_d, container, false);
        button1d = view.findViewById(R.id.button1d);
        button2d = view.findViewById(R.id.button2d);



        MainActivity mainActivity = (MainActivity) getActivity();


        button1d.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                setMainTextViewData(txtDisplayTwoView, "+P");
                byte[] txBuffer = new byte[8];
                Arrays.fill(txBuffer, (byte) 0);
                // Prepare main controller data
                txBuffer[0] =  (byte) '$';
                txBuffer[1] = (byte) 'B';
                txBuffer[2] = (byte) 'T';
                txBuffer[3] = (byte) '1';
                txBuffer[4] = (byte) 'S';
                txBuffer[5] = (byte) '+';
                txBuffer[6] = (byte) '9';
                txBuffer[7] = (byte) ';';

                mainActivity.bluetoothManager.sendControllerData(txBuffer);
            }
        });

        button2d.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                setMainTextViewData(txtDisplayTwoView, "-P");
                byte[] txBuffer = new byte[8];
                Arrays.fill(txBuffer, (byte) 0);
                // Prepare main controller data
                txBuffer[0] =  (byte) '$';
                txBuffer[1] = (byte) 'B';
                txBuffer[2] = (byte) 'T';
                txBuffer[3] = (byte) '1';
                txBuffer[4] = (byte) 'S';
                txBuffer[5] = (byte) '-';
                txBuffer[6] = (byte) '9';
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
