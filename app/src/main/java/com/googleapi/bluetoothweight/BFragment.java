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

public class BFragment extends Fragment {

    private AppCompatButton button1b,button2b,button3b,button4b,button5b;
    private TextView txtDisplayTwoView;

    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //return inflater.inflate(R.layout.fragment_train_main, container, false);
        View view = inflater.inflate(R.layout.fragment_b, container, false);
        button1b = view.findViewById(R.id.button1b);
        button2b = view.findViewById(R.id.button2b);
        button3b = view.findViewById(R.id.button3b);
        button4b = view.findViewById(R.id.button4b);
        button5b = view.findViewById(R.id.button5b);


        MainActivity mainActivity = (MainActivity) getActivity();


        button1b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                setMainTextViewData(txtDisplayTwoView, "-1");
                byte[] txBuffer = new byte[8];
                Arrays.fill(txBuffer, (byte) 0);
                // Prepare main controller data
                txBuffer[0] =  (byte) '$';
                txBuffer[1] = (byte) 'B';
                txBuffer[2] = (byte) 'T';
                txBuffer[3] = (byte) '1';
                txBuffer[4] = (byte) 'S';
                txBuffer[5] = (byte) '-';
                txBuffer[6] = (byte) '1';
                txBuffer[7] = (byte) ';';

                mainActivity.bluetoothManager.sendControllerData(txBuffer);
            }
        });

        button2b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                setMainTextViewData(txtDisplayTwoView, "-2");
                byte[] txBuffer = new byte[8];
                Arrays.fill(txBuffer, (byte) 0);
                // Prepare main controller data
                txBuffer[0] =  (byte) '$';
                txBuffer[1] = (byte) 'B';
                txBuffer[2] = (byte) 'T';
                txBuffer[3] = (byte) '1';
                txBuffer[4] = (byte) 'S';
                txBuffer[5] = (byte) '-';
                txBuffer[6] = (byte) '2';
                txBuffer[7] = (byte) ';';

                mainActivity.bluetoothManager.sendControllerData(txBuffer);
            }
        });

        button3b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                setMainTextViewData(txtDisplayTwoView, "-3");
                byte[] txBuffer = new byte[8];
                Arrays.fill(txBuffer, (byte) 0);
                // Prepare main controller data
                txBuffer[0] =  (byte) '$';
                txBuffer[1] = (byte) 'B';
                txBuffer[2] = (byte) 'T';
                txBuffer[3] = (byte) '1';
                txBuffer[4] = (byte) 'S';
                txBuffer[5] = (byte) '-';
                txBuffer[6] = (byte) '3';
                txBuffer[7] = (byte) ';';

                mainActivity.bluetoothManager.sendControllerData(txBuffer);
            }
        });

        button4b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                setMainTextViewData(txtDisplayTwoView, "-4");
                byte[] txBuffer = new byte[8];
                Arrays.fill(txBuffer, (byte) 0);
                // Prepare main controller data
                txBuffer[0] =  (byte) '$';
                txBuffer[1] = (byte) 'B';
                txBuffer[2] = (byte) 'T';
                txBuffer[3] = (byte) '1';
                txBuffer[4] = (byte) 'S';
                txBuffer[5] = (byte) '-';
                txBuffer[6] = (byte) '4';
                txBuffer[7] = (byte) ';';

                mainActivity.bluetoothManager.sendControllerData(txBuffer);
            }
        });

        button5b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                setMainTextViewData(txtDisplayTwoView, "-5");
                byte[] txBuffer = new byte[8];
                Arrays.fill(txBuffer, (byte) 0);
                // Prepare main controller data
                txBuffer[0] =  (byte) '$';
                txBuffer[1] = (byte) 'B';
                txBuffer[2] = (byte) 'T';
                txBuffer[3] = (byte) '1';
                txBuffer[4] = (byte) 'S';
                txBuffer[5] = (byte) '-';
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
