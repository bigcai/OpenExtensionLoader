package com.openel.interfaces;

import com.openel.annotations.Activate;

/**
 * Created by caisz on 2017/1/3.
 */
public class SecondTestDemo implements TestDemo {

    private String myname;

    public SecondTestDemo() {
        this.myname = "caisz2";
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
