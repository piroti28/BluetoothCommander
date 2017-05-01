/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jp.gr.java_conf.piropiroping.bluetoothcommander;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;

import static jp.gr.java_conf.piropiroping.bluetoothcommander.AsciiCode.command;

/**
 * This is the main Activity that displays the current chat session.
 */
public class MainActivity extends FragmentActivity implements AsciiDialogFragment.AsciiDialogListener,
        CommandAdditionFragment.NoticeDialogListener,
        FileTransferFragment.FileListListener {

    // Debugging
    private static final String TAG = "BluetoothChat";
    private static final boolean D = true;
    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;
    // Layout
    private LinearLayout commandLayout;    // コマンド追加レイアウト
    // Layout Views
    private ListView mRecieveDataView;  // 受信データ
    private ListView mSendDataView;     // 送信データ
    private EditText mOutEditText;      // 入力データ
    private Button mSendButton;         // 送信ボタン
    private Button mAsciiDialog;        // 制御コードボタン
    private ImageButton plusButton;     // コマンド追加ボタン
    private ArrayList<Button> savedCommand; // コマンドリスト
    // Adapter of ListView
    private ArrayAdapter<String> mRecieveDataAdapter;   // 受信アダプター
    private ArrayAdapter<String> mSendDataAdapter;      // 送信アダプター
    // Name of the connected device
    private String mConnectedDeviceName = null;
    // String buffer for outgoing messages
    private StringBuffer mOutStringBuffer;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothService mChatService = null;
    // Command delete design (TBD)
    private static boolean delState = false;
    private int currentX;
    private int currentY;
    private int viewX;
    private int viewY;
    // Const
    public static final String STX = new String(Character.toChars(0x0002));
    public static final String ETX = new String(Character.toChars(0x0003));

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (D) Log.e(TAG, "+++ ON CREATE +++");

        // Set up the window layout
        setContentView(R.layout.activity_main);

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        commandLayout = (LinearLayout) findViewById(R.id.command_layout);
        savedCommand = new ArrayList();
        plusButton = (ImageButton)findViewById(R.id.plus_button);
        plusButton.setImageResource(R.drawable.plus_button);


        AdView mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);


    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        for (int i = 0; i < savedCommand.size(); i++) {
            ViewGroup.LayoutParams lp = savedCommand.get(i).getLayoutParams();
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) lp;
            mlp.setMargins(3, 3, 3, 3);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (D) Log.e(TAG, "++ ON START ++");

        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else {
            if (mChatService == null) setupChat();
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if (D) Log.e(TAG, "+ ON RESUME +");

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothService.STATE_NONE) {
                // Start the Bluetooth chat services
                mChatService.start();
            }
        }
    }

    private void setupChat() {
        Log.d(TAG, "setupChat()");

        // Initialize the array adapter for the conversation thread
        mRecieveDataAdapter = new ArrayAdapter<String>(this, R.layout.recieve_message);
        mRecieveDataView = (ListView) findViewById(R.id.recieve_data);
        mRecieveDataView.setAdapter(mRecieveDataAdapter);

        // Initialize the array adapter for the send data
        mSendDataAdapter = new ArrayAdapter<String>(this, R.layout.send_message);
        mSendDataView = (ListView) findViewById(R.id.send_data);
        mSendDataView.setAdapter(mSendDataAdapter);

        // Initialize the compose field with a listener for the return key
        mOutEditText = (EditText) findViewById(R.id.edit_command);
//        mOutEditText.setOnEditorActionListener(mWriteListener);

        // Initialize the send button with a listener that for click events
        mSendButton = (Button) findViewById(R.id.button_send);
        mSendButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                String message = mOutEditText.getText().toString();
                byte[] data = convertStringToHex(message);
                sendMessage(data);
            }
        });

        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothService(this, mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");

        /* ASCIIコードダイアログ表示リスナー */
        mAsciiDialog = (Button) findViewById(R.id.dialog_ascii);
        mAsciiDialog.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                AsciiDialogFragment asciiDialog = new AsciiDialogFragment();
                asciiDialog.show(getFragmentManager(), "ascii");
            }
        });

        /* コマンド追加ボタンリスナー */
        plusButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                int viewNum = commandLayout.getChildCount() - 1;    // コマンド保存数
                if (viewNum > 4) {
                    Toast.makeText(getApplicationContext(), "コマンドが保存できるのは５つまでです。", Toast.LENGTH_SHORT).show();
                    return;
                }
                CommandAdditionFragment commandAddition = new CommandAdditionFragment();
                commandAddition.show(getFragmentManager(), "command");
            }
        });

        String[] fileList = fileList();
        for (int i = 0; i < fileList().length; i++) {
            setCommandButtonListener(fileList[i], i);
        }
    }

    // The dialog fragment receives a reference to this Activity through the
    // Fragment.onAttach() callback, which it uses to call the following methods
    // defined by the NoticeDialogFragment.NoticeDialogListener interface
    @Override
    public void onDialogPositiveClick(DialogFragment dialog, EditText commandName) {
        // User touched the dialog's positive button
        String name = commandName.getText().toString();     // コマンド名
        String data = mOutEditText.getText().toString();    // コマンドデータ

        int viewNum = savedCommand.size();    // コマンド保存数
        for (int i = 0; i < viewNum; i++) {
            String savedName = savedCommand.get(i).getText().toString();
            if (savedName.equals(name)) {     // すでに同じコマンド名が登録されていたらreturn
                Toast.makeText(getApplicationContext(), "That command name has already been registered.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        /* ローカルにコマンド保存 */
        try {
            OutputStream out = openFileOutput(name, MODE_PRIVATE);
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, "UTF-8"));
            writer.append(data);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        setCommandButtonListener(name, viewNum);
    }

    private void setCommandButtonListener(String commandName, int commandVal) {
        Button commandButton = new Button(this);    // 新規追加コマンド
        commandButton.setText(commandName);
        commandButton.setTextSize(20);
        commandButton.setBackgroundResource(R.drawable.command_button_background);
        savedCommand.add(commandVal, commandButton);      // 保存したコマンドを挿入
        commandLayout.removeAllViews();         // 表示されているコマンドをクリア
        for (int i = 0; i < commandVal+1; i++) { // 再表示(新規追加Button + PlusButton)
            commandLayout.addView(savedCommand.get(i));
        }
        commandLayout.addView(plusButton);

        /* 追加されたコマンドボタンのクリックリスナー */
        commandButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Button button = (Button) view;
                String name = button.getText().toString();
                try {
                    InputStream in = openFileInput(name);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                    String s;
                    mOutEditText.getText().clear();
                    while ((s = reader.readLine()) != null) {
                        mOutEditText.append(s);
                    }
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                setCommandData();   // ASCIIコードをBitmap変換
            }
        });

        /* 長押しでコマンド削除 */
        commandButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(final View view) {
                Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                vibrator.vibrate(20);
                delState = true;    // 長押し状態ON

                final Button button = (Button) view;
                final String name = button.getText().toString();

                AlertDialog.Builder deleteCommandDialog = new AlertDialog.Builder(MainActivity.this);
                deleteCommandDialog.setTitle("このコマンドを消去しますか？");
                deleteCommandDialog.setMessage(name);
                deleteCommandDialog.setPositiveButton("はい", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // OK button pressed
                        deleteFile(name);
                        commandLayout.removeView(button);
                        savedCommand.remove(button);
                    }
                });
                deleteCommandDialog.setNegativeButton("いいえ", null);
                deleteCommandDialog.show();
                return false;
            }
        });
        /* Command delete design (TBD) */
//        commandButton.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View view, MotionEvent motionEvent) {
//                int x = (int) motionEvent.getRawX();
//                int y = (int) motionEvent.getRawY();
//
//                switch (motionEvent.getAction()) {
//                    case MotionEvent.ACTION_DOWN:
//                        viewX = view.getLeft();
//                        viewY = view.getTop();
//                        currentX = x;
//                        currentY = y;
//                        break;
//                    case MotionEvent.ACTION_MOVE:
//                        LinearLayout topLayout = (LinearLayout) findViewById(R.id.top_layout);
////                        topLayout.addView(view);
//                        if (delState == true) {
//                            int ajustX = currentX - x;
//                            int ajustY = currentY - y;
//                            viewX = viewX - ajustX;
//                            viewY = viewY - ajustY;
//                            view.layout(viewX, viewY, viewX + view.getWidth(), viewY + view.getHeight());
//                            currentX = x;
//                            currentY = y;
//                        }
//                        break;
//                    case MotionEvent.ACTION_UP:
//                        delState = false;
//                        // if(ゴミ箱ボタンに入れたら)
//                        //      コマンドデータ削除
//                        // else
//                        //      元のxy座標に描画
//
//                        break;
//
//                }
//                return false;
//            }
//        });
    }

    private void setCommandData() {
        String data = mOutEditText.getText().toString();
        String[] charData = new String[data.length()];
        for (int i = 0; i < charData.length; i++) {
            charData[i] = String.valueOf(data.charAt(i));
        }
        for (int pos = 0; pos < charData.length; pos++) {
            StringBuilder sb = new StringBuilder();
            if (charData[pos].equals(STX)) {
                int start = pos;
                while (true) {
                    sb.append(charData[pos]);
                    if (charData[pos].equals(ETX)) {
                        int stop = pos;
                        String command = new String(sb);
                        convertBitmapCommand(command, start, stop);
                        break;
                    } else {
                        pos++;
                    }
                }
            }
        }
    }

    private void convertBitmapCommand(String command, int start, int stop) {
        //描画するテキスト
        Paint objPaint = new Paint();
        Bitmap objBitmap;
        Canvas objCanvas;
        int textSize = 40;
        int textWidth = textSize * command.length(), textHeight = textSize;

        objPaint.setAntiAlias(true);
        objPaint.setColor(Color.BLUE);
        objPaint.setTextSize(textSize);
        Paint.FontMetrics fm = objPaint.getFontMetrics();
        objPaint.getTextBounds(command, 0, command.length(), new Rect(0, 0, textWidth, textHeight));

        //テキストの表示範囲を設定
        textWidth = (int) objPaint.measureText(command);
        textHeight = (int) (Math.abs(fm.top) + fm.bottom);
        objBitmap = Bitmap.createBitmap(textWidth, textHeight, Bitmap.Config.ARGB_8888);

        //キャンバスからビットマップを取得
        objCanvas = new Canvas(objBitmap);
        objCanvas.drawText(command, 0, Math.abs(fm.top), objPaint);

        Editable editable = mOutEditText.getText();
        SpannableStringBuilder ssb = (SpannableStringBuilder) editable;
        ImageSpan is = new ImageSpan(getApplicationContext(), objBitmap);
        ssb.setSpan(is, start, stop + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }


    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        // User touched the dialog's negative button
        // 何もしない
    }

    /* String型をbyte[]型へ変換 */
    private static byte[] convertStringToHex(String message) {
        byte[] data = new byte[message.length()];
        String[] buf = new String[message.length()];
        int count = 0;

        /* String型をString[]型に変換 */
        for (int i = 0; i < buf.length; i++) {
            buf[i] = String.valueOf(message.charAt(i));
        }

        for (int i = 0; i < buf.length; i++) {
            if (buf[i].equals(STX)) {
                StringBuilder sb = new StringBuilder();
                while (true) {    // 制御コード文字列を取得
                    if (buf[++i].equals(ETX)) {
                        break;
                    } else {
                        sb.append(buf[i]);
                    }
                }
                String st = new String(sb);
                for (int num = 0; num < command.length; num++) {
                    String code = command[num];
                    if (code.equals(st)) {
                        data[count++] = (byte) num;  // ASCIIコードをbyteにキャストして格納
                    }
                }
            } else {
                byte[] bufCode = buf[i].getBytes();
                data[count++] = bufCode[0];
            }
        }
        data = Arrays.copyOf(data, count);  // nullを除外して整形
        return data;
    }

    /* ASCIIコマンドボタンリスナー */
    @Override
    public void onSelectCommandClick(View view) {
        Button selectedCommand = (Button) view;
        String commandName = selectedCommand.getText().toString();
        Editable editable = mOutEditText.getText();
        int start = mOutEditText.getSelectionStart();
        int end = mOutEditText.getSelectionEnd();
        editable.replace(Math.min(start, end), Math.max(start, end), STX + commandName + ETX);
        setCommandData();
    }


    @Override
    public void onDialogFileListClick(String name, byte[] data) {
        sendMessage(data);
        Toast.makeText(this, "Command File " + name + " sent.", Toast.LENGTH_SHORT);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (D) Log.e(TAG, "- ON PAUSE -");
    }

    @Override
    public void onStop() {
        super.onStop();
        if (D) Log.e(TAG, "-- ON STOP --");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (mChatService != null) mChatService.stop();
        if (D) Log.e(TAG, "--- ON DESTROY ---");
    }

    private void ensureDiscoverable() {
        if (D) Log.d(TAG, "ensure discoverable");
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    /**
     * Sends a message.
     *
     * @param message A string of text to send.
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            byte[] send = message.getBytes();

            // Get the message bytes and tell the BluetoothChatService to write
            mChatService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            mOutEditText.setText(mOutStringBuffer);
        }
    }


    /* sendMessage function for HEX */
    private void sendMessage(byte[] message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length > 0) {
            mChatService.write(message);

            // Reset out string buffer to zero and clear the edit text field
//            mOutStringBuffer.setLength(0);
//            mOutEditText.setText(mOutStringBuffer);
        }
    }


    // The action listener for the EditText widget, to listen for the return key
//    private TextView.OnEditorActionListener mWriteListener =
//            new TextView.OnEditorActionListener() {
//                public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
//                    // If the action is a key-up event on the return key, send the message
//                    if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
//                        String message = view.getText().toString();
//                        sendMessage(message);
//                    }
//                    if (D) Log.i(TAG, "END onEditorAction");
//                    return true;
//                }
//            };

    private final void setStatus(int resId) {
        final ActionBar actionBar = getActionBar();
        actionBar.setSubtitle(resId);
    }

    private final void setStatus(CharSequence subTitle) {
        final ActionBar actionBar = getActionBar();
        actionBar.setSubtitle(subTitle);
    }

    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    if (D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                            mRecieveDataAdapter.clear();
                            mSendDataAdapter.clear();
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            break;
                        case BluetoothService.STATE_LISTEN:
                        case BluetoothService.STATE_NONE:
                            setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    for (int i = 0; i < command.length; i++) {
                        writeMessage = writeMessage.replaceAll(command[i], String.valueOf(i));
                    }
                    byte[] writeAsciiCode = new byte[writeMessage.length()];
                    try {
                        writeAsciiCode = writeMessage.getBytes("US-ASCII");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    StringBuilder writeHex = new StringBuilder();
                    for (int i = 0; i < writeAsciiCode.length; i++) {
                        String hexCode = Integer.toHexString(writeAsciiCode[i]);
                        if (hexCode.length() == 1) {
                            hexCode = "0" + hexCode;
                        }
                        writeHex.append(hexCode + " ");
                    }
                    mSendDataAdapter.add(new String(writeHex));
                    break;
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    byte[] readAsciiCode = new byte[readMessage.length()];
                    try {
                        readAsciiCode = readMessage.getBytes("US-ASCII");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    StringBuilder readHex = new StringBuilder();
                    for (int i = 0; i < readAsciiCode.length; i++) {
                        String hexCode = Integer.toHexString(readAsciiCode[i]);
                        if (hexCode.length() == 1) {
                            hexCode = "0" + hexCode;
                        }
                        readHex.append(hexCode + " ");
                    }
                    mRecieveDataAdapter.add(new String(readHex));
                    break;
                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, true);
                }
                break;
            case REQUEST_CONNECT_DEVICE_INSECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, false);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupChat();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }

    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mChatService.connect(device, secure);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent serverIntent = null;
        switch (item.getItemId()) {
            case R.id.secure_connect_scan:  // invisible
                // Launch the DeviceListActivity to see devices and do scan
                serverIntent = new Intent(this, DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
                return true;
            case R.id.insecure_connect_scan:
                // Launch the DeviceListActivity to see devices and do scan
                serverIntent = new Intent(this, DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
                return true;
            case R.id.discoverable:
                // Ensure this device is discoverable by others
                ensureDiscoverable();
                return true;
        }
        return false;
    }

}
