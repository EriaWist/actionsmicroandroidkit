package com.actionsmicro.androidkit.ezcast.filter;

import java.util.ArrayList;

/**
 * Created by laicc on 2015/10/30.
 */
public class AndFilter<T> implements FilterInterface<T>{

    private ArrayList<FilterInterface> filters;

    public AndFilter() {
        filters = new ArrayList<FilterInterface>();
    }

    public void addFilter(FilterInterface filter) {
        filters.add(filter);
    }
    public void removeFilter(FilterInterface filter) {
        filters.remove(filter);
    }

    @Override
    public boolean accept(T object) {
        for (FilterInterface filter:filters) {
            if (!filter.accept(object)) {
                return false;
            }
        }
        return true;
    }
}
