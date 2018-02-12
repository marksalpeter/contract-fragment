package com.marksalpeter.fragment;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.app.Fragment;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;

import java.lang.reflect.Field;

/**
 * DialogFragment is a dialog fragment with the fragment animation bug fix applied and an immersive mode
 * bug fix applied (when
 * read more about the bug fixes here:
 *  - http://stackoverflow.com/questions/14900738/nested-fragments-disappear-during-transition-animation
 *  - https://stackoverflow.com/questions/32758559/maintain-immersive-mode-when-dialogfragment-is-shown
 * Created by Mark on 11/9/16.
 */
public abstract class DialogFragment extends android.app.DialogFragment implements ViewTreeObserver.OnGlobalLayoutListener {

    private static String TAG = DialogFragment.class.getSimpleName();

    /**
     * sDefaultChildAnimationDuration is an arbitrary, but reasonable transition duration we can use if
     * reflection fails to obtain the parent Fragment next animation id
     */
    private static final int sDefaultChildAnimationDuration = 1000;

    private Dialog mDialog;
    private Handler mHandler;
    private boolean mIsImmersive;

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {

        // check to see if the dialog is immersive
        mIsImmersive = (getActivity().getWindow().getDecorView().getSystemUiVisibility() & View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY) == View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        if (!mIsImmersive) {
            Log.d(TAG, getActivity().getWindow().getDecorView().getSystemUiVisibility() + " is not immersive");
            return super.onCreateDialog(savedInstanceState);
        }

        // create a handler
        mHandler = new Handler();

        // create a full screen dialog
        mDialog = super.onCreateDialog(savedInstanceState);
        mDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        mDialog.getWindow().setDimAmount(.8f);

        // hide the status bar and enter full screen mode
        mDialog.getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        // dismiss the keyguard
        mDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        mDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        mDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Set the IMMERSIVE flag.
        // Set the content to appear under the system bars so that the content
        // doesn't resize when the system bars hide and show.
        mDialog.getWindow().getDecorView().getRootView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        // immersive hack 1: set the dialog to not focusable (makes navigation ignore us adding the window)
        mDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);

        // prevent the popup from being canceled
        mDialog.setCanceledOnTouchOutside(false);

        // pass the touch events back to the parent activity for dispatch
        mDialog.getWindow().getDecorView().getRootView().setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                getActivity().dispatchTouchEvent(event);
                return false;
            }
        });

        return mDialog;
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
    }

    @Override public void onGlobalLayout() {
        if (!isDialog() || !mIsImmersive) {
            return;
        }
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                hideUI();
            }
        }, 500);
    }

    @Override public void onResume() {
        super.onResume();
        if (!isDialog() || !mIsImmersive) {
            return;
        }

        // immersive hack 2: Clear the not focusable flag from the window and show the keyboard
        mDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);

        // immersive hack 3: show the soft keyboard immediately  if an edit text is visible
        EditText firstEditText = getFirstEditText((ViewGroup) mDialog.getWindow().getDecorView());
        if (firstEditText != null) {
            mDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            firstEditText.setSelection(firstEditText.getText().length());
        }
    }

    /**
     * onCreateAnimator is overridden to fix the following animation bug:
     * http://stackoverflow.com/questions/14900738/nested-fragments-disappear-during-transition-animation
     */
    @Override public Animator onCreateAnimator(int transit, boolean enter, int nextAnim) {
        if(!enter) {
            Fragment removingParentFragment = getRemovingParentFragment();
            if (removingParentFragment != null) {
                return ValueAnimator.ofFloat(0f, 1f).setDuration(getNextAnimatorDuration(removingParentFragment));
            }
        }
        return super.onCreateAnimator(transit, enter, nextAnim);
    }


    /**
     * getRemovingParentFragment returns the first fragment or parent fragment that is removing r null if it can't find one
     */
    private android.app.Fragment getRemovingParentFragment() {
        android.app.Fragment fragment = getParentFragment();
        while (fragment != null) {
            if (fragment.isRemoving()) {
                Log.v(TAG, fragment.toString() + " is removing");
                return fragment;
            }
            fragment = fragment.getParentFragment();
        }
        return null;
    }

    /**
     * getNextAnimationDuration returns the "mNextAnim" animators duration
     * TODO: this needs a bug fix, but its not mission critical unless people are adding fragment transition
     */
    private static long getNextAnimatorDuration(android.app.Fragment fragment) {
        try {

            // attempt to get the resource ID of the next animation that will be applied to the given fragment.
            Field nextAnimField = android.app.Fragment.class.getDeclaredField("mNextAnim");
            nextAnimField.setAccessible(true);
            int nextAnimResourceID = nextAnimField.getInt(fragment);

            Log.v(TAG, "nextAnimResourceID: " + nextAnimResourceID);
            if (nextAnimResourceID < 1) {
                return sDefaultChildAnimationDuration;
            }

            // load the animator
            Animator nextAnim = AnimatorInflater.loadAnimator(fragment.getActivity(), nextAnimResourceID);

            Log.v(TAG, "nextAnim.getDuration(): " + nextAnim.getDuration());

            // return its duration
            return(nextAnim == null || nextAnim.getDuration() < 0) ? sDefaultChildAnimationDuration : nextAnim.getDuration();

        } catch (NoSuchFieldException|IllegalAccessException|Resources.NotFoundException ex) {
            Log.e(TAG, ex.getMessage(), ex);
            return sDefaultChildAnimationDuration;
        }
    }

    /**
     * getFirstEditText returns the first edit text in the parents hierarchy
     */
    private EditText getFirstEditText(ViewGroup parent) {
        if (parent == null) {
            return null;
        }
        EditText editText = null;
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            if (child instanceof EditText && child.getVisibility() == View.VISIBLE) {
                editText = (EditText) child;
                break;
            } else if (child instanceof ViewGroup) {
                editText = getFirstEditText((ViewGroup) child);
                if (editText != null) {
                    break;
                }
            }
        }
        return editText;
    }

    private void hideUI() {
        if (!isDialog()) {
            return;
        }

        // hide the status bar and enter full screen mode
        mDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // dismiss the keyguard
        mDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        mDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

        mDialog.getWindow().getDecorView().getRootView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        mHandler.removeCallbacksAndMessages(null);
    }

    /**
     * isDialog returns true if the fragment had been rendered in a dialog window
     */
    private boolean isDialog() {
        return mDialog != null && mDialog.getWindow() != null;
    }

    /**
     * isImmersive returns true if the dialog was initialized in immersive mode
     */
    protected boolean isImmersive() {
        return mIsImmersive;
    }

}
