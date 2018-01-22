package com.cn.pedometer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cn.bean.StepEntity;
import com.cn.bean.StepInfo;
import com.cn.calendar.BeforeOrAfterCalendarView;
import com.cn.db.StepDataDao;
import com.cn.utils.JsonUtils;
import com.cn.utils.TimeUtil;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.today.step.lib.ISportStepInterface;
import com.today.step.lib.TodayStepManager;
import com.today.step.lib.TodayStepService;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static String TAG = "MainActivity";

    private static final int REFRESH_STEP_WHAT = 0;

    //循环取当前时刻的步数中间的间隔时间
    private long TIME_INTERVAL_REFRESH = 500;

    private Handler mDelayHandler = new Handler(new TodayStepCounterCall());
    private int mStepSum = 0;

    private ISportStepInterface iSportStepInterface;
    private TextView kmTextView;
    private TextView kmTimeTv;
    private TextView stepTextView;
    private TextView stepsTimeTv;
    private TextView movement_current_kaluli;
    private TextView movement_current_time;
    private LinearLayout showCurrentKaluli;
    private LinearLayout showCurrentTime;

    private String curSelDate;//当前时间
    private DecimalFormat df = new DecimalFormat("#.##");
    /**
     * 屏幕长度和宽度
     */
    public static int screenWidth, screenHeight;

    private BeforeOrAfterCalendarView calenderView;
    private LinearLayout movementCalenderLl;

    private StepDataDao stepDataDao;//计步数据库
    private List<StepEntity> stepEntityList = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //初始化计步模块
        TodayStepManager.init(getApplication());
        //初始化布局
        initView();
        initTime();
        initData();
        initListener();

        //开启计步Service，同时绑定Activity进行aidl通信
        Intent intent = new Intent(this, TodayStepService.class);
        startService(intent);
        bindService(intent, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                //Activity和Service通过aidl进行通信
                iSportStepInterface = ISportStepInterface.Stub.asInterface(service);
                try {
                    mStepSum = iSportStepInterface.getCurrentTimeSportStep();
                    updateStepCount();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                mDelayHandler.sendEmptyMessageDelayed(REFRESH_STEP_WHAT, TIME_INTERVAL_REFRESH);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        }, Context.BIND_AUTO_CREATE);

    }

    /**
     * 初始化布局
     */
    private void initView() {
        movementCalenderLl = (LinearLayout) findViewById(R.id.movement_records_calender_ll);
        stepTextView = (TextView) findViewById(R.id.movement_total_steps_tv);
        kmTextView = (TextView) findViewById(R.id.movement_total_km_tv);
        kmTimeTv = (TextView) findViewById(R.id.movement_total_km_time_tv);
        stepsTimeTv = (TextView) findViewById(R.id.movement_total_steps_time_tv);
        movement_current_kaluli = (TextView) findViewById(R.id.movement_current_kaluli);
        movement_current_time = (TextView) findViewById(R.id.movement_current_time);
        showCurrentKaluli = (LinearLayout) findViewById(R.id.showCurrentKaluli);
        showCurrentTime = (LinearLayout) findViewById(R.id.showCurrentTime);
        showCurrentKaluli.setVisibility(View.INVISIBLE);
        showCurrentTime.setVisibility(View.INVISIBLE);
    }

    /**
     * 初始化当前时间
     */
    private void initTime() {
        //设置时间
        curSelDate = TimeUtil.getCurrentDate();
        String time = TimeUtil.getWeekStr(curSelDate);
        kmTimeTv.setText(time);
        stepsTimeTv.setText(time);
    }

    /**
     * 初始化数据
     */
    private void initData() {
        WindowManager windowManager = getWindowManager();
        Display display = windowManager.getDefaultDisplay();
        screenWidth = display.getWidth();
        screenHeight = display.getHeight();
        //放到获取宽度之后
        calenderView = new BeforeOrAfterCalendarView(this);
        movementCalenderLl.addView(calenderView);
        getRecordList();
    }

    /**
     * 监听日历点击事件
     */
    private void initListener() {
        calenderView.setOnBoaCalenderClickListener(new BeforeOrAfterCalendarView.BoaCalenderClickListener() {
            @Override
            public void onClickToRefresh(int position, String curDate) {
                //获取当前选中的时间
                curSelDate = curDate;
                //如果是今天则更新数据
                if (curSelDate.equals(TimeUtil.getCurrentDate())) {
                    updateStepCount();
                } else {
                    //根据日期去取数据
                    setDatas(curSelDate);
                    showCurrentKaluli.setVisibility(View.INVISIBLE);
                    showCurrentTime.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    class TodayStepCounterCall implements Handler.Callback {

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case REFRESH_STEP_WHAT: {
                    //如果是今天，则刷新数据
                    try {
                        if (curSelDate.equals(TimeUtil.getCurrentDate())) {
                            //每隔500毫秒获取一次计步数据刷新UI
                            if (null != iSportStepInterface) {
                                int step = 0;
                                try {
                                    step = iSportStepInterface.getCurrentTimeSportStep();
                                } catch (RemoteException e) {
                                    e.printStackTrace();
                                }
                                if (mStepSum != step) {
                                    mStepSum = step;
                                    updateStepCount();
                                }
                                mDelayHandler.sendEmptyMessageDelayed(REFRESH_STEP_WHAT, TIME_INTERVAL_REFRESH);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
            return false;
        }
    }

    /**
     * 更新当前计步
     */
    private void updateStepCount() {
        kmTextView.setText(countTotalKM(mStepSum));
        stepTextView.setText(String.valueOf(mStepSum));
        initTime();
        saveStepData();
//        showCurrentInfo();
    }

    /**
     * 设置记录数据
     */
    private void setDatas(String curSelectedDate) {
        StepEntity stepEntity = stepDataDao.getCurDataByDate(curSelectedDate);
        if (stepEntity != null) {
            int steps = Integer.parseInt(stepEntity.getSteps());
            //获取全局的步数
            stepTextView.setText(String.valueOf(steps));
            //计算总公里数
            kmTextView.setText(countTotalKM(steps));
        } else {
            //获取全局的步数
            stepTextView.setText("0");
            //计算总公里数
            kmTextView.setText("0");
        }

        //设置时间
        String time = TimeUtil.getWeekStr(curSelDate);
        kmTimeTv.setText(time);
        stepsTimeTv.setText(time);
    }

    /**
     * 获取全部运动历史纪录
     */
    private void getRecordList() {
        //获取数据库实例
        stepDataDao = new StepDataDao(this);
        stepEntityList.clear();
        stepEntityList.addAll(stepDataDao.getAllDatas());
        if (stepEntityList.size() >= 7) {

        }
    }

    /**
     * 保存当天的数据到数据库中
     */
    private void saveStepData() {
        //查询数据库中的数据
        StepEntity entity = stepDataDao.getCurDataByDate(curSelDate);
        //为空则说明还没有该天的数据，有则说明已经开始当天的计步了
        if (entity == null) {
            //没有则新建一条数据
            entity = new StepEntity();
            entity.setCurDate(curSelDate);
            entity.setSteps(String.valueOf(mStepSum));

            stepDataDao.addNewData(entity);
        } else {
            //有则更新当前的数据
            entity.setSteps(String.valueOf(mStepSum));
            stepDataDao.updateCurData(entity);
        }
    }

    /**
     * 简易计算公里数，假设一步大约有0.6米
     *
     * @param steps 用户当前步数
     * @return
     */
    private String countTotalKM(int steps) {
        if(steps>0){
            double totalMeters = steps * 0.6;
            //保留两位有效数字
            return df.format(totalMeters / 1000);
        }
        return "";
    }

    /**
     * 计步信息显示
     */
    private void showCurrentInfo() {
        if (curSelDate.equals(TimeUtil.getCurrentDate())) {
            showCurrentTime.setVisibility(View.VISIBLE);
            showCurrentKaluli.setVisibility(View.VISIBLE);
            if (null != iSportStepInterface) {
                try {
                    String stepArray = iSportStepInterface.getTodaySportStepArray();
                    //gson对象
                    Gson gson = new Gson();
                    //定义一个集合存放遍历的json数组
                    ArrayList<StepInfo> stepInfoList = new ArrayList<>();
                    //遍历json数组
                    for (JsonElement stepString : JsonUtils.jsonParse(stepArray)) {
                        StepInfo stepInfo = gson.fromJson(stepString, StepInfo.class);
                        stepInfoList.add(stepInfo);
                    }
                    String currentTime = TimeUtil.stampToDate(String.valueOf(stepInfoList.get(stepInfoList.size() - 1).getSportDate()));
                    movement_current_kaluli.setText(stepInfoList.get(stepInfoList.size() - 1).getKaluli());
                    movement_current_time.setText(currentTime);

                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        } else {
            showCurrentKaluli.setVisibility(View.INVISIBLE);
            showCurrentTime.setVisibility(View.INVISIBLE);
        }
    }
}
