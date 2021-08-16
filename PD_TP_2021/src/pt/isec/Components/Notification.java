package pt.isec.Components;

import java.io.Serializable;

public class Notification implements Serializable {
    private int code; //direct message | channel message | etc
    private String info = "";
    private String info2 = "";

    public Notification(int code) {
        this.code = code;
    }

    public Notification(int code, String info) {
        this.code = code;
        this.info = info;
    }

    public Notification(int code, String info, String info2) {
        this.code = code;
        this.info = info;
        this.info2 = info2;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public void setInfo2(String info2) {
        this.info2 = info2;
    }

    public int getCode() {
        return code;
    }

    public String getInfo() {
        return info;
    }

    public String getInfo2() {
        return info2;
    }
}
