package com.eyeverify.eyeprintid;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.eyeverify.eyeprintid.BaseActivity;
import com.eyeverify.eyeprintid.EVCaptureActivity;

import java.util.Random;

public class WelcomeActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        Button b2=(Button)findViewById(R.id.button2);
        b2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                init();
            }
        });
    }
    public int init()
    {
        EditText o=(EditText)findViewById(R.id.editText1);
        EditText p=(EditText)findViewById(R.id.editText2);
        EditText q=(EditText)findViewById(R.id.editText3);
        EditText z=(EditText)findViewById(R.id.editText4);
        String o1=o.getText().toString();
        String o2=p.getText().toString();
        String o3=q.getText().toString();
        String o7=z.getText().toString();
        int o4= Integer.parseInt(o1);
        int o5= Integer.parseInt(o2);
        int o6= Integer.parseInt(o3);
        int o8= Integer.parseInt(o7);
        EditText b9=(EditText)findViewById(R.id.editText7);
        b9.setText("Secret");
        int num=1,x=0,denom,t=3;
        int accum=0;
        int res[]=new int[]{o4,o5,o6};
        for(int i=1;i<=t;i++)
        {

            int k=i;
            num=res[k-1];
            denom=1;
            for(int j=1;j<=t;j++)
            {
                if (i!=j)

                {
                    int u=-j;
                    num*=u;
                    denom*=(i-j);
                }


            }
            accum+=(num)/(denom);
            int a=(((accum % o8)+o8)%o8);
        }
        b9.append(": "+accum);

        return 0;

    }

}