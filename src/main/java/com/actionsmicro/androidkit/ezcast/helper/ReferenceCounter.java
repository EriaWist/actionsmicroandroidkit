package com.actionsmicro.androidkit.ezcast.helper;

import java.util.HashMap;
import java.util.Map;

import com.actionsmicro.androidkit.ezcast.ApiBuilder;

public abstract class ReferenceCounter<T, K> {
	private Map<K, T> reg = new HashMap<K, T>();
	private HashMap<T, Integer> referenceCount = new HashMap<T, Integer>(); 
	public synchronized T create(ApiBuilder<?> apiBuilder) {
		T apiImp = null; 
		if (reg.containsKey(getKey(apiBuilder))) {
			apiImp = reg.get(getKey(apiBuilder));
			referenceCount.put(apiImp, referenceCount.get(apiImp) + 1);
		} else {
			apiImp = createInstance(apiBuilder);
			referenceCount.put(apiImp, 1);
			reg.put(getKey(apiBuilder), apiImp);
		}
		return apiImp;
	}
	protected abstract K getKey(ApiBuilder<?> apiBuilder);
	
	protected abstract T createInstance(ApiBuilder<?> apiBuilder);
	
	public synchronized void release(T apiImp, ApiBuilder<?> apiBuilder) {
		if (referenceCount.containsKey(apiImp)) {
			int refCount = referenceCount.get(apiImp) - 1;
			if (refCount == 0) {
				reg.remove(getKey(apiBuilder));
				referenceCount.remove(apiImp);
				releaseInstance(apiImp, apiBuilder);
			} else {
				referenceCount.put(apiImp, refCount);
			}
		}
	}
	protected abstract void releaseInstance(T apiImp, ApiBuilder<?> apiBuilder);
}
