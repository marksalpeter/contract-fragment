package com.marksalpeter.fragment;

import android.app.Activity;
import android.app.Fragment;

import java.lang.reflect.ParameterizedType;

/**
 * ContractDialogFragment is a dialog fragment whos parent fragment or parent activity must implement an interface
 * Created by Mark Salpeter. Original concepts taken from Jake Thwarton
 * see https://gist.github.com/JakeWharton/2621173
 */
public abstract class ContractDialogFragment<T> extends ImmersiveDialogFragment {

    public final static String TAG = ContractDialogFragment.class.getSimpleName();

    private T mContract;

    @Override public void onAttach(Activity activity) {
        Fragment parentFragment = getParentFragment();
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

    public final T getContract() {
        return mContract;
    }

}
