package com.googleapi.bluetoothweight;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;

import java.util.Arrays;

public class CFragment extends Fragment {

    private AppCompatButton button1c,button2c,button3c,buttonr, buttonOffc;
    private TextView txtDisplayTwoView;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // return inflater.inflate(R.layout.fragment_train_main, container, false);
        View view = inflater.inflate(R.layout.fragment_c, container, false);
        button1c = view.findViewById(R.id.button1c);
        button2c = view.findViewById(R.id.button2c);
        button3c = view.findViewById(R.id.button3c);




        MainActivity mainActivity = (MainActivity) getActivity();


        button1c.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                setMainTextViewData(txtDisplayTwoView, "R6");
                byte[] txBuffer = new byte[8];
                Arrays.fill(txBuffer, (byte) 0);
                // Prepare main controller data
                txBuffer[0] =  (byte) '$';
                txBuffer[1] = (byte) 'B';
                txBuffer[2] = (byte) 'T';
                txBuffer[3] = (byte) '1';
                txBuffer[4] = (byte) 'S';
                txBuffer[5] = (byte) '+';
                txBuffer[6] = (byte) '6';
                txBuffer[7] = (byte) ';';

                mainActivity.bluetoothManager.sendControllerData(txBuffer);
            }
        });

        button2c.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                setMainTextViewData(txtDisplayTwoView, "R7");
                byte[] txBuffer = new byte[8];
                Arrays.fill(txBuffer, (byte) 0);
                // Prepare main controller data
                txBuffer[0] =  (byte) '$';
                txBuffer[1] = (byte) 'B';
                txBuffer[2] = (byte) 'T';
                txBuffer[3] = (byte) '1';
                txBuffer[4] = (byte) 'S';
                txBuffer[5] = (byte) '+';
                txBuffer[6] = (byte) '7';
                txBuffer[7] = (byte) ';';

                mainActivity.bluetoothManager.sendControllerData(txBuffer);
            }
        });

        button3c.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                setMainTextViewData(txtDisplayTwoView, "R8");
                byte[] txBuffer = new byte[8];
                Arrays.fill(txBuffer, (byte) 0);
                // Prepare main controller data
                txBuffer[0] =  (byte) '$';
                txBuffer[1] = (byte) 'B';
                txBuffer[2] = (byte) 'T';
                txBuffer[3] = (byte) '1';
                txBuffer[4] = (byte) 'S';
                txBuffer[5] = (byte) '+';
                txBuffer[6] = (byte) '8';
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
