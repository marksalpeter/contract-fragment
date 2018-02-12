package com.marksalpeter.fragment;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.ValueAnimator;
import android.content.res.Resources;
import android.util.Log;

import java.lang.reflect.Field;

/**
 * Fragment fixes an animation bug where child fragments are sometimes removed from the screen
 * before their parents are removed from the screen.
 * More Info: http://stackoverflow.com/questions/14900738/nested-fragments-disappear-during-transition-animation
 * Created by Mark Salpeter on 9/20/16.
 */
public class Fragment extends android.app.Fragment {

    public static String TAG = Fragment.class.getSimpleName();

    /**
     * sDefaultChildAnimationDuration is an arbitrary, but reasonable transition duration we can use if
     * reflection fails to obtain the parent Fragment next animation id
     */
    private static final int sDefaultChildAnimationDuration = 1000;


    /**
     * onCreateAnimator is overridden to fix the following animation bug:
     * http://stackoverflow.com/questions/14900738/nested-fragments-disappear-during-transition-animation
     */
    @Override public Animator onCreateAnimator(int transit, boolean enter, int nextAnim) {
        if(!enter) {
            android.app.Fragment removingParentFragment = getRemovingParentFragment();
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
}
