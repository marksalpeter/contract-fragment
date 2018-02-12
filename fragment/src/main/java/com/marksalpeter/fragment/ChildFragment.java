package com.marksalpeter.fragment;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.ValueAnimator;
import android.app.Fragment;
import android.content.res.Resources;

import java.lang.reflect.Field;

/**
 * ChildFragment fixes a critical view bug where child fragments are sometimes removed from the screen
 * before their parents are removed from the screen.
 * More Info: http://stackoverflow.com/questions/14900738/nested-fragments-disappear-during-transition-animation
 * Created by Mark Salpeter on 9/20/16.
 */
public class ChildFragment extends Fragment {

    public static String TAG = ChildFragment.class.getSimpleName();

    /**
     * sDefaultChildAnimationDuration is an arbitrary, but reasonable transition duration we can use if
     * reflection fails to obtain the parent Fragment next animation id
     */
    private static final int sDefaultChildAnimationDuration = 1000;

    @Override public Animator onCreateAnimator(int transit, boolean enter, int nextAnim) {

        // if a child fragment is being removed because its parent is being removed
        // return a fake animator that lasts the duration of the parent's animator
        Fragment removingParentFragment = getRemovingParent(getParentFragment());
        if (!enter && removingParentFragment != null) {
            // This is a workaround for the bug where child fragments disappear when
            // the parent is removed (as all children are first removed from the parent)
            // See https://code.google.com/p/android/issues/detail?id=55228
            long duration = getNextAnimatiorDuration(removingParentFragment);
            return ValueAnimator.ofFloat(0, 1).setDuration(duration);
        }

        // inflate the animator
        Animator animator = null;
        try {
            animator = AnimatorInflater.loadAnimator(getActivity(), nextAnim);
        } catch (Exception e) {}

        return animator;
    }

    /**
     * getRemovingParent returns the first fragment or parent fragment that is removing r null if it can't find one
     */
    private static Fragment getRemovingParent(Fragment fragment) {
            while (fragment != null) {
                if (fragment.isRemoving()) {
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
    private static long getNextAnimatiorDuration(Fragment fragment) {
        try {

            // attempt to get the resource ID of the next animation that will be applied to the given fragment.
            Field nextAnimField = Fragment.class.getDeclaredField("mNextAnim");
            nextAnimField.setAccessible(true);
            int nextAnimResource = nextAnimField.getInt(fragment);

            // load the animator
            Animator nextAnim = AnimatorInflater.loadAnimator(fragment.getActivity(), nextAnimResource);

            // return its duration
            return(nextAnim == null || nextAnim.getDuration() < 0) ? sDefaultChildAnimationDuration : nextAnim.getDuration();

        } catch (NoSuchFieldException|IllegalAccessException|Resources.NotFoundException ex) {
            return sDefaultChildAnimationDuration;
        }
    }
}
