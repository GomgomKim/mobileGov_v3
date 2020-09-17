package kr.go.mobile.iff.sample;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }

    public void onClickDoc(View view) {
        Intent _intent = new Intent(MainActivity.this, DocActivity.class);
        startActivity(_intent);
    }

    public void onClickDoc2(View view) {
        Intent _intent = new Intent(MainActivity.this, DocActivity2.class);
        startActivity(_intent);
    }

    public void onClickDoc3(View view) {
        Intent _intent = new Intent(MainActivity.this, DocActivity3.class);
        startActivity(_intent);
    }
}
