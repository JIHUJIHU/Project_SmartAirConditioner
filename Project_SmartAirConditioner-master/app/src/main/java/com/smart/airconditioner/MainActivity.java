package com.smart.airconditioner;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.smart.airconditioner.model.Weather;
import com.smart.airconditioner.network.BluetoothClient;
import com.smart.airconditioner.network.DustInfo;
import com.smart.airconditioner.network.WeatherInfo;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    BluetoothClient client;
    DustInfo dInfo;
    WeatherInfo wInfo;

    TextView mWeather;
    TextView mDust;
    TextView mTemp;
    TextView mStatus;

    Button p1, p2, p3;

    Button timer;
    Switch onoff;
    Switch auto;



    @Override
    public void onCreate(Bundle onSaveStateInstance) {
        super.onCreate(onSaveStateInstance);
        setContentView(R.layout.activity_main);

        init();
    }

    public void init() {
        mWeather = findViewById(R.id.weather);
        mDust = findViewById(R.id.dust);
        mTemp = findViewById(R.id.temperature);
        mStatus = findViewById(R.id.status);

        auto = findViewById(R.id.auto);

        p1 = findViewById(R.id.p1);
        p2 = findViewById(R.id.p2);
        p3 = findViewById(R.id.p3);

        timer = findViewById(R.id.timer);

        onoff = findViewById(R.id.onoff);

        initInterface();
        initDustInfo();
        initWeatherInfo();
        initClient();
    }

    public void initInterface() {
        timer.setOnClickListener(this);
        p1.setOnClickListener(this);
        p2.setOnClickListener(this);
        p3.setOnClickListener(this);


        auto.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                int isCheck = isChecked ? 1 : 0;
                try {
                    handler.write(("M"+isCheck+"#").getBytes("UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                p1.setEnabled(!isChecked);
                p2.setEnabled(!isChecked);
                p3.setEnabled(!isChecked);
                onoff.setEnabled(!isChecked);
                timer.setEnabled(!isChecked);
            }
        });

        onoff.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(!isChecked) {
                    try {
                        handler.write(("P0#").getBytes("UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
                mStatus.setText("세기를 선택해주세요.");
                p1.setEnabled(isChecked);
                p2.setEnabled(isChecked);
                p3.setEnabled(isChecked);
                timer.setEnabled(isChecked);
            }

        });


    }

    public void initDustInfo() {
        dInfo = new DustInfo(this);
        dInfo.getCurrentDust();
    }

    public void notifyDustChange(String data) {
        String dust = String.format(getResources().getString(R.string.dust), data);
        mDust.setText(dust);
    }

    public void initWeatherInfo() {
        wInfo = new WeatherInfo(this);
        wInfo.getCurrentWeather();
    }

    public void notifyWeatherChange(Weather weather) { // WeatherTask 작업 끝을 알림
        int weatherId = weather.getWeaatherId();
        int resID = getResources().getIdentifier("@string/weather_" + weatherId, "string", "com.smart.airconditioner");
        String weatherString = getString(resID);
        mTemp.setText(String.valueOf(Math.round(weather.getTemperature()))+"℃");
        mWeather.setText(weatherString);
    }

    public void initClient() {
        client = BluetoothClient.getInstance();
        if (client == null) { // 블루투스를 사용할 수 없는 장비일 경우 null.
            Toast.makeText(getApplicationContext(), "블루투스를 사용할 수 없는 기기입니다.", Toast.LENGTH_LONG).show();
            finish();
        } else {
            if (!client.isEnabled())
                enableBluetooth();
            else {
                getPairedDevice();
            }
        }
    }

    public void enableBluetooth() {
        client.enableBluetooth(this, new BluetoothClient.OnBluetoothEnabledListener() {
            @Override
            public void onBluetoothEnabled(boolean success) {
                if (success) {
                    getPairedDevice();
                } else {
                    Toast.makeText(getApplicationContext(), "블루투스를 활성화하지 못했습니다.", Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        });
    }

    public void getPairedDevice() {

        final List<String> deviceListString = new ArrayList<>();
        final ArrayList<BluetoothDevice> deviceList = new ArrayList<>(); // 이미 페어링된 디바이스 리스트를 가져온다.
        Set<BluetoothDevice> pairedDevices = client.getPairedDevices();
        deviceList.addAll(pairedDevices);

        deviceList.addAll(pairedDevices); // 근처 디바이스를 스캔한다.
        client.scanDevices(this, new BluetoothClient.OnScanListener() {
            ProgressDialog pd = new ProgressDialog(MainActivity.this);

            @Override
            public void onStart() {//스캔 시작.
                pd.setMessage("블루투스 기기를 찾는중... ");
                pd.setCancelable(false);
                pd.show();
            }

            @Override
            public void onFoundDevice(BluetoothDevice bluetoothDevice) { // 스캔이 완료된 디바이스를 받아온다.
                if (deviceList.contains(bluetoothDevice)) {
                    deviceList.remove(bluetoothDevice);
                }
                deviceList.add(bluetoothDevice);
            }

            @Override
            public void onFinish() { // 스캔 종료.
                for (BluetoothDevice bd : deviceList) {
                    deviceListString.add(bd.getName());
                }
                pd.dismiss();
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setCancelable(false);
                builder.setTitle("연결할 블루투스 기기를 선택해주세요.");
                builder.setItems(deviceListString.toArray(new CharSequence[deviceListString.size()]), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        connectDevice(deviceList.get(which));
                    }
                }).show();

            }
        });
    }


    public void connectDevice(BluetoothDevice device) {
        if (!client.connect(this, device, handler)) { // 블루투스가 사용 가능한 상태가 아니려면 false 리턴.
            Toast.makeText(this, "블루투스를 사용할 수 없습니다", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        if (client != null)
            client.clear();
        super.onDestroy();
    }

    /**
     * parameter
     * M0# : 수동모드
     * M1# : 자동모드
     * P0# : 전원끄기
     * P1# : 세기1
     * P2# : 세기2
     * P3# : 세기3
     * T1#~T9# : 타이머 (1시간 단위)
     *
     * @method BluetoothStreamingHandler.send
     * @params byte[] data
     */
    BluetoothClient.BluetoothStreamingHandler handler = new BluetoothClient.BluetoothStreamingHandler() {
        @Override
        public void onError(Exception e) {
            // 에러 발생. 연결이 종료된다.
            Toast.makeText(MainActivity.this, "애러가 발생했습니다",Toast.LENGTH_SHORT).show();
            finish();
        }

        @Override
        public void onConnected() {
            Toast.makeText(MainActivity.this, "연결되었습니다..",Toast.LENGTH_SHORT).show();
            // 연결 이벤트.
        }

        @Override
        public void onDisconnected() {
            // 연결 종료 이벤트.
            Toast.makeText(MainActivity.this, "연결이 종료되었습니다.",Toast.LENGTH_SHORT).show();
            finish();
        }
        ByteBuffer mmByteBuffer = ByteBuffer.allocate(1024);

        @Override
        public void onData(byte[] buffer, int length) {

            if (length == 0) return;
            if (mmByteBuffer.position() + length >= mmByteBuffer.capacity()) {
                ByteBuffer newBuffer = ByteBuffer.allocate(mmByteBuffer.capacity() * 2);
                newBuffer.put(mmByteBuffer.array(), 0, mmByteBuffer.position());
                mmByteBuffer = newBuffer;
            }
            mmByteBuffer.put(buffer, 0, length);
            if (buffer[length - 1] == '\0') {
                String current = new String(mmByteBuffer.array(), 0, mmByteBuffer.position());
                if(!current.contains("#")) return;
                boolean isAuto;
                //Toast.makeText(MainActivity.this, current, Toast.LENGTH_SHORT).show();
                if(current.contains("A")||current.contains("C")){
                    isAuto = current.contains("A");
                    auto.setChecked(isAuto);
                    try {
                        int currentStatus = Integer.parseInt(current.trim().replace("A", "").replace("C","").replace("#","")); // 문자열에서 공백 제거후  A,C 제거
                        switch(currentStatus){
                            case 0: //꺼짐
                                mStatus.setText("세기를 선택해주세요.");
                                break;

                            case 1: //세기 1
                                mStatus.setText("1단 세기로 돌아가고 있습니다.");
                                break;

                            case 2: //세기 2
                                mStatus.setText("2단 세기로 돌아가고 있습니다.");
                                break;

                            case 3: //세기 3
                                mStatus.setText("3단 세기로 돌아가고 있습니다.");
                                break;
                        }
                    }

                    catch(NumberFormatException e){
                        Toast.makeText(MainActivity.this, "애러가 발생했습니다.", Toast.LENGTH_SHORT).show(); // 송신된 데이터에 결함 유
                    }
                }
                mmByteBuffer.clear();
            }
        }
    };


    @Override
    public void onClick(View v) {
        try {
            switch (v.getId()) {
                case R.id.p1:
                    handler.write(("P1#").getBytes("UTF-8"));
                    break;

                case R.id.p2:
                    handler.write(("P2#").getBytes("UTF-8"));
                    break;

                case R.id.p3:
                    handler.write(("P3#").getBytes("UTF-8"));
                    break;
                case R.id.timer:
                    showEditDialog();
                    break;

            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public void showEditDialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("전원 타이머 설정 (1시간 단위)");

        final EditText name = new EditText(this);

        alert.setView(name);

        alert.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String time = name.getText().toString();
                int time_int = 0;
                try {
                    time_int = Integer.parseInt(time);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "올바른 숫자가 아닙니다.", Toast.LENGTH_SHORT).show();
                }

                if (time_int > 9 || time_int < 1) {
                    Toast.makeText(MainActivity.this, "1이상, 9이하의 숫자를 입력해주세요.", Toast.LENGTH_SHORT).show();
                }
                else {
                    final String temp = mStatus.getText().toString();
                    mStatus.setText(time_int+"시간 후에 전원이 꺼집니다.");
                    try {
                        handler.write(("T" + time + "#").getBytes("UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mStatus.setText(temp);
                        }
                    }, 4000);
                }
            }
        });


        alert.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        });
        alert.show();
    }
}