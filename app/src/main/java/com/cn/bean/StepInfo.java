package com.cn.bean;

/**
 * Created by vicmob_yf002 on 2017/10/26.
 */

public class StepInfo {

    /**
     * today : 2017-10-26
     * sportDate : 1508988359978
     * stepNum : 0
     * km : 0.00
     * kaluli : 0.0
     */

    private String today;
    private long sportDate;
    private int stepNum;
    private String km;
    private String kaluli;

    public String getToday() {
        return today;
    }

    public void setToday(String today) {
        this.today = today;
    }

    public long getSportDate() {
        return sportDate;
    }

    public void setSportDate(long sportDate) {
        this.sportDate = sportDate;
    }

    public int getStepNum() {
        return stepNum;
    }

    public void setStepNum(int stepNum) {
        this.stepNum = stepNum;
    }

    public String getKm() {
        return km;
    }

    public void setKm(String km) {
        this.km = km;
    }

    public String getKaluli() {
        return kaluli;
    }

    public void setKaluli(String kaluli) {
        this.kaluli = kaluli;
    }
}
