package com.actionsmicro.androidkit.ezcast.filter;

/**
 * Created by laicc on 2015/10/30.
 */
public interface FilterInterface<T> {
    boolean accept(T object);
}
