package com.mesalu.viv2.android_ui.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

/**
 * Model POJO for representing a server-side EnvDataSample instance.
 */
public class EnvDataSample {
    //@SerializedName("id")
    //long id;

    @SerializedName("environment")
    String envId;

    @SerializedName("captured")
    Date captureTime;

    @SerializedName("hotGlass")
    double hotGlass;

    @SerializedName("hotMat")
    double hotMat;

    @SerializedName("midGlass")
    double midGlass;

    @SerializedName("coldGlass")
    double coldGlass;

    @SerializedName("coldMat")
    double coldMat;

    public String getEnvId() {
        return envId;
    }

    public void setEnvId(String envId) {
        this.envId = envId;
    }

    public Date getCaptureTime() {
        return captureTime;
    }

    public void setCaptureTime(Date captureTime) {
        this.captureTime = captureTime;
    }

    public double getHotGlass() {
        return hotGlass;
    }

    public void setHotGlass(double hotGlass) {
        this.hotGlass = hotGlass;
    }

    public double getHotMat() {
        return hotMat;
    }

    public void setHotMat(double hotMat) {
        this.hotMat = hotMat;
    }

    public double getMidGlass() {
        return midGlass;
    }

    public void setMidGlass(double midGlass) {
        this.midGlass = midGlass;
    }

    public double getColdGlass() {
        return coldGlass;
    }

    public void setColdGlass(double coldGlass) {
        this.coldGlass = coldGlass;
    }

    public double getColdMat() {
        return coldMat;
    }

    public void setColdMat(double coldMat) {
        this.coldMat = coldMat;
    }
}
