package com.openel.interfaces;

/**
 * Created by caisz on 2017/1/3.
 */
public class MyTestDemo implements TestDemo {

    private String myname;

    public MyTestDemo() {
        this.myname = "caisz";
    }

    public String getMyname() {
        return myname;
    }

    public void setMyname(String myname) {
        this.myname = myname;
    }

    @Override
    public String name() {
        return this.myname;
    }
}
