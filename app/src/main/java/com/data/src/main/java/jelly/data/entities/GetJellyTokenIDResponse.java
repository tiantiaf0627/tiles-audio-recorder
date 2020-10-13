package com.data.src.main.java.jelly.data.entities;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by tiantianfeng on 11/30/17.
 */

public class GetJellyTokenIDResponse {

    @SerializedName("errors")
    @Expose
    private String[] errors;

    @SerializedName("success")
    @Expose
    private String state;

    @SerializedName("message")
    @Expose
    private String returnMessage;


    @SerializedName("result")
    @Expose
    private String returnResult;


    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getResult() {
        return returnResult;
    }

    public void setResult(String returnResult) {
        this.returnResult = returnResult;
    }

    public String getReturnMessage() {
        return returnMessage;
    }

    public void setReturnMessage(String roomNumber) {
        this.returnMessage = roomNumber;
    }


    @Override
    public String toString() {
        return "state = " + this.state + " result = " + returnResult;
    }

}
