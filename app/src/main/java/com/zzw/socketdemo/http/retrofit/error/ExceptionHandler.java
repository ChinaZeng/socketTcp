package com.zzw.socketdemo.http.retrofit.error;

import android.app.Activity;

import java.util.ArrayList;

public class ExceptionHandler implements IExceptionHandler {
    private ArrayList<IExceptionHandler> handlers;

    {
        handlers = new ArrayList<>();
    }

    @Override
    public boolean handle(final Activity activity, Throwable t) {
        for (IExceptionHandler handler : handlers) {
            if (handler.handle(activity, t))
                return true;
        }
        return false;
    }


}