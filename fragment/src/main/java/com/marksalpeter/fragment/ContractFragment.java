package com.marksalpeter.fragment;

import android.app.Activity;

import java.lang.reflect.ParameterizedType;

/**
 * ContractFragment is a fragment whos parent fragment or parent activity must implement the interface T
 * Created by Mark Salpeter. Original concepts taken from Jake Wharton
 * see https://gist.github.com/JakeWharton/2621173
 */
public abstract class ContractFragment<T> extends Fragment {

    public final static String TAG = Fragment.class.getSimpleName();

    private T mContract;

    @Override
    public void onAttach(Activity activity) {
        android.app.Fragment parentFragment = getParentFragment();
        if (parentFragment != null && isContractImplemented(parentFragment.getClass())) {
            mContract = (T) getParentFragment();
        } else if (isContractImplemented(activity.getClass())) {
            mContract = (T) activity;
        } else {
            String exception = "";
            if (parentFragment != null) {
                exception += parentFragment.getClass().getSimpleName() + " and ";
            }
            exception += activity.getClass().getSimpleName() + " does not implement " + getClass().getSimpleName() + "'s contract interface.";
             throw new IllegalStateException(exception);
        }
        super.onAttach(activity);
    }

    private boolean isContractImplemented(Class<?> c) {
        return ((Class)((ParameterizedType)getClass().getGenericSuperclass()).getActualTypeArguments()[0]).isAssignableFrom(c);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mContract = null;
    }

    /**
     * getContract returns the interface of type T that must be
     * implemented on either the parent fragment or the parent activity
     */
    public final T getContract() {
        return mContract;
    }

}
