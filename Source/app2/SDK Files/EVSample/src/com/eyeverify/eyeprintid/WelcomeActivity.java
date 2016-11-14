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

    private int prime;
    private Random random;
    private double k1, n1;


    //  public int deg[];
    public int init() {

        //final int prime = getPrime();
        // coeff(3);

        //      Shamir(t2, no2);
        Button bu = (Button) findViewById(R.id.button);
        bu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText no = (EditText) findViewById(R.id.editText2);
                EditText t = (EditText) findViewById(R.id.editText3);
                EditText m = (EditText) findViewById(R.id.editText5);
             //   TextView y=(TextView)findViewById(R.id.textView);
                String no1 = no.getText().toString();
                String t1 = t.getText().toString();
                String m1 = m.getText().toString();
                int no2 = Integer.parseInt(no1);
                int t2 = Integer.parseInt(t1);
                int m2 = Integer.parseInt(m1);
                //    final BigInteger secret=new BigInteger("12");
                //int secrt= R.id.editText5;
//        Shamir(t2, no2);

                //  SecretShare[] shares = split(m);
                polym(m2, t2, no2);
            }
        });
        //   final double result = combine(shares, prime);
        // quad(t);

        return 0;
    }

    public int getprime(int d) {
        double random = Math.random() * 50 + (d);
        double prime1 = random;
        int p=0;
        int prime = (int) prime1;
        for (int j = 2; j < prime; j++) {
            if (prime % j == 0) {
               p=1;
            }
        }
        if(p==1)
        {
            return 0;

    }
        else
            return prime;

    }


    public double polym(int h, int l, int n) {

        int list[] = new int[l];
        // Random rand = new Random();

        // int  n = rand.nextInt(50) + 1;
        for (int i = 0; i < l - 1; i++) {
            Random rand = new Random();

            int o = rand.nextInt(100) + 1;


            list[i] = o;

        }


        //polym(list[]);
        //   return 0;

        // double l;
        int r;

        TextView t= (TextView)findViewById(R.id.textView);
        int p = getprime(h + 1);
        t.setText("Prime number:"+p);
        int i, j;
        int k = list.length;
        int res = l;
        if (res > 0) {
            for (i = 1; i <= n; i++) {
                int m = l - 1;
                res = h;
                while (m > 0) {
                    for (j = l - 1; j > 0; j--) {

                        // int v=j;


                        int temp = (int) Math.pow(i, j);
                        int d = list[m - 1] * temp;
                        m--;
                        res += d;
                        int share=(((res%p)+p)%p);
                        int v = j;
                        if (v - 1 == 0) {
                            t.append("Shares" + i + "::" + share+"" +
                                    "");

                        }

                    }
                }

            }

        } else if (res == 0) {
            t.append("THE SECRET IS:" + h);
        } else {
            t.append("INVALID OUTPUT");
        }

        return 0;
    }

    public int Shamir(final double k, final int n) {
        //Random random;

        k1 = k;
        n1 = n;

        //  random = new Random();
        return 0;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        init();
Button g=(Button)findViewById(R.id.button3);
        g.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i=new Intent(WelcomeActivity.this,InstructionsActivity.class);
                startActivity(i);
            }
        });
    }
}
