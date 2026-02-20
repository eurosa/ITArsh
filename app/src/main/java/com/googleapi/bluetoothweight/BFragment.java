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

    private AppCompatButton  button4a,button5a;
    private TextView txtDisplayTwoView;

    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //return inflater.inflate(R.layout.fragment_train_main, container, false);
        View view = inflater.inflate(R.layout.fragment_b, container, false);

        button4a = view.findViewById(R.id.button4a);
        button5a = view.findViewById(R.id.button5a);
        button5a.setVisibility(View.GONE);

        MainActivity mainActivity = (MainActivity) getActivity();




        button4a.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {





            }
        });

        button5a.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {



            }
        });





        return view;
    }


    public void setMainTextViewData(TextView txtDisplayTwoView, String txt){
        txtDisplayTwoView.setText(txt);

    }
}
