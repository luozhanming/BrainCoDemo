package cn.com.ava.braincodemo;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class HttpSettingActivity extends AppCompatActivity implements View.OnClickListener {


    private Button tvBack, tvSave;
    private EditText etIP, etHost;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_http_setting);
        tvBack = findViewById(R.id.tv_back);
        tvSave = findViewById(R.id.tv_save);
        etIP = findViewById(R.id.et_ip);
        etHost = findViewById(R.id.et_host);
        tvSave.setOnClickListener(this);
        tvBack.setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_save:
                if (TextUtils.isEmpty(etIP.getText().toString()) || TextUtils.isEmpty(etHost.getText().toString())) {
                    Toast.makeText(this, "请填写好信息再保存", Toast.LENGTH_SHORT).show();
                    return;
                }
                final SharedPreferences http = getSharedPreferences("http", MODE_PRIVATE);
                final SharedPreferences.Editor edit = http.edit();
                edit.putString("ip", etIP.getText().toString());
                edit.putString("host", etHost.getText().toString());
                edit.commit();
                finish();
                break;
            case R.id.tv_back:
                finish();
                break;
        }
    }
}
