package com.alibaba.idst.nls.demo;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.idst.util.NlsClient;
import com.alibaba.idst.util.SpeechRecognizerWithRecorder;
import com.alibaba.idst.util.SpeechRecognizerWithRecorderCallback;

import java.lang.ref.WeakReference;

public class SpeechRecognizerWithRecorderActivity extends AppCompatActivity {
    private static final String TAG="AliSpeechDemo";

    private EditText mFullEdit;
    private EditText mResultEdit;
    private NlsClient client;
    private SpeechRecognizerWithRecorder speechRecognizer;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speech_recognizer);
        Button button = (Button) findViewById(R.id.button);
        mFullEdit = (EditText) findViewById(R.id.editText2);
        mResultEdit = (EditText) findViewById(R.id.editText);

        button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        startRecognizer(view);
                        break;
                    }
                    case MotionEvent.ACTION_MOVE: {
                        break;
                    }
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_UP: {
                        stopRecognizer(view);
                        break;
                    }
                }
                return false;
            }
        });

        //第一步，创建client实例，client只需要创建一次，可以用它多次创建recognizer
        client = new NlsClient();
    }

    @Override
    public void onDestroy(){
        if (speechRecognizer != null){
            speechRecognizer.stop();
        }

        // 最终，退出时释放client
        client.release();
        super.onDestroy();
    }

    // 此方法内启动录音和识别逻辑，为了代码简单便于理解，没有加防止用户重复点击的逻辑，用户应该在真实使用场景中注意
    public void startRecognizer(View view){
        mFullEdit.setText("");
        mResultEdit.setText("");

        //UI在主线程更新
        Handler handler= new MyHandler(this);
        // 第二步，新建识别回调类
        SpeechRecognizerWithRecorderCallback callback = new MyCallback(handler);

        // 第三步，创建识别request
        speechRecognizer = client.createRecognizerWithRecorder(callback);

        // 第四步，设置相关参数
        // Token有有效期，请使用https://help.aliyun.com/document_detail/72153.html 动态生成token
        speechRecognizer.setToken("");
        // 请使用阿里云语音服务管控台(https://nls-portal.console.aliyun.com/)生成您的appkey
        speechRecognizer.setAppkey("");
        // 设置返回中间结果，更多参数请参考官方文档
        // 开启ITN
        speechRecognizer.enableInverseTextNormalization(true);
        // 开启标点
        speechRecognizer.enablePunctuationPrediction(false);
        // 不返回中间结果
        speechRecognizer.enableIntermediateResult(false);
        // 设置打开服务端VAD
        speechRecognizer.enableVoiceDetection(true);
        speechRecognizer.setMaxStartSilence(3000);
        speechRecognizer.setMaxEndSilence(600);
        // 设置定制模型和热词
        // speechRecognizer.setCustomizationId("yourCustomizationId");
        // speechRecognizer.setVocabularyId("yourVocabularyId");
        speechRecognizer.start();
    }

    public void stopRecognizer(View view){
        // 第八步，停止本次识别
        speechRecognizer.stop();
    }

    // 语音识别回调类，得到语音识别结果后在这里处理
    // 注意不要在回调方法里执行耗时操作
    private static class MyCallback implements SpeechRecognizerWithRecorderCallback {

        private Handler handler;

        public MyCallback(Handler handler) {
            this.handler = handler;
        }

        // 识别开始
        @Override
        public void onRecognizedStarted(String msg, int code)
        {
            Log.d(TAG,"OnRecognizedStarted " + msg + ": " + String.valueOf(code));
        }

        // 请求失败
        @Override
        public void onTaskFailed(String msg, int code)
        {
            Log.d(TAG,"OnTaskFailed " + msg + ": " + String.valueOf(code));
            handler.sendEmptyMessage(0);
        }

        // 识别返回中间结果，只有开启相关选项时才会回调
        @Override
        public void onRecognizedResultChanged(final String msg, int code)
        {
            Log.d(TAG,"OnRecognizedResultChanged " + msg + ": " + String.valueOf(code));
            Message message = new Message();
            message.obj = msg;
            handler.sendMessage(message);
        }

        // 第七步，识别结束，得到最终完整结果
        @Override
        public void onRecognizedCompleted(final String msg, int code)
        {
            Log.d(TAG,"OnRecognizedCompleted " + msg + ": " + String.valueOf(code));
            Message message = new Message();
            message.obj = msg;
            handler.sendMessage(message);
        }

        // 请求结束，关闭连接
        @Override
        public void onChannelClosed(String msg, int code) {
            Log.d(TAG, "OnChannelClosed " + msg + ": " + String.valueOf(code));
        }

        // 手机采集的语音数据的回调
        @Override
        public void onVoiceData(byte[] bytes, int i) {

        }

        // 手机采集的语音音量大小的回调
        @Override
        public void onVoiceVolume(int i) {

        }
    };

    // 根据识别结果更新界面的代码
    private static class MyHandler extends Handler {
        private final WeakReference<SpeechRecognizerWithRecorderActivity> mActivity;

        public MyHandler(SpeechRecognizerWithRecorderActivity activity) {
            mActivity = new WeakReference<SpeechRecognizerWithRecorderActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.obj == null) {
                Log.i(TAG, "Empty message received.");
                return;
            }
            String rawResult = (String)msg.obj;
            String result = null;
            if (!rawResult.equals("")){
                JSONObject jsonObject = JSONObject.parseObject(rawResult);
                if (jsonObject.containsKey("payload")){
                    result = jsonObject.getJSONObject("payload").getString("result");
                }
            }
            mActivity.get().mFullEdit.setText(rawResult);
            mActivity.get().mResultEdit.setText(result);
        }
    }
}
