package com.wzx.app.fastui;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.AnimRes;
import android.support.annotation.AnimatorRes;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;

import com.wzx.app.fastui.utils.ComnUtil;

public class SwitchHelper {

    private static final String TAG = SwitchHelper.class.getSimpleName();

    private static int[] globalAnims = new int[]{0,0,0,0};

    /**
     * 设置全局公共动画，其中参数的意义是
     1）enter 指定向栈中放入新的Fragment时的动画

     2）exit 指定向栈中弹出当前栈顶的Fragment时的动画

     3）popEnter 指定由于当前栈顶Fragment弹出而显示底层的Fragment时的动画

     4）popExit 指定当前栈顶的Fragment被弹出时的动画
     *
     * @param enter
     * @param exit
     * @param popEnter
     * @param popExit
     */
    public static void setGlobalCustomAnimations(@AnimatorRes @AnimRes int enter,
                                                 @AnimatorRes @AnimRes int exit, @AnimatorRes @AnimRes int popEnter,
                                                 @AnimatorRes @AnimRes int popExit) {
        globalAnims = new int[]{enter, exit, popEnter, popExit};
    }


    /**调用入口
     * @param activity
     * @return
     */
    public static SwitchCard with(FragmentActivity activity) {
        return new SwitchCard(activity);
    }

    static boolean commit(SwitchCard card) {
        if (card.getTargetFragment() == null) {
            Log.e(TAG, "no match target fragment");
            return false;
        }
        String hostName = card.getHostName();
        if (hostName.equals(card.getCurActivity().getClass().getName())) {
            UIContainer container = SwitchContainerManager.getUIContainer(card.getCurActivity());
            if (container == null) {
                Log.e(TAG, "the activity " + card.getCurActivity().getClass().getName() + " missing the container");
                return false;
            }
            SwitchFragment stackFragment = container.isSwitchLast(card.getTargetFragment());
            boolean switchLast = stackFragment != null;
            if (switchLast) {
                //如果目标fragment就在栈顶
                if (container.getCurFragment() == stackFragment) {
                    container.getCurFragment().onNewBundle(card.getTargetBundle());
                    return true;
                }
                card.setTargetFragment(stackFragment);
            }
            // 添加参数
            if (card.getTargetBundle() != null) {
                if (switchLast) {
                    card.getTargetFragment().onNewBundle(card.getTargetBundle());
                } else {
                    card.getTargetFragment().setArguments(card.getTargetBundle());
                }
            }
            FragmentTransaction transaction = container.getFragmentManager().beginTransaction();
            if (container.getStackSize() != 0 && card.isUseAnim()) {
                int enter = card.getEnterAnim() == 0 ? globalAnims[switchLast ? 2 : 0] : card.getEnterAnim();
                int exit = card.getExitAnim() == 0 ? globalAnims[switchLast ? 3 : 1] : card.getExitAnim();
                if (enter != 0 && exit != 0) {
                    transaction.setCustomAnimations(enter, exit);
                }
            }
            if (!switchLast) {
                if (container.getCurFragment() != null) {
                    if (card.isFinishCurrent()) {
                        transaction.remove(container.getCurFragment());
                    } else {
                        transaction.hide(container.getCurFragment());
                    }
                }
                container.addToStack(card.getTargetFragment(), transaction);
            } else {
                container.popStack(card.getTargetFragment(), transaction);
            }
            transaction.commit();
        } else {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(card.getCurActivity().getPackageName(), hostName));
            ComnUtil.setTargetToIntent(intent, card.getTargetFragment().getClass().getName(), card.getTargetBundle());
            card.getCurActivity().startActivity(intent);
            if (card.isFinishCurrent()){
                SwitchHelper.goBack(card.getCurActivity(),card.getTargetBundle(),false);
            }
        }
        return true;
    }

    public static boolean goBack(FragmentActivity activity, Bundle bundle) {
       return goBack(activity, bundle, true);
    }

    public static boolean goBack(FragmentActivity activity, Bundle bundle, boolean useAnim) {
        UIContainer container = SwitchContainerManager.getUIContainer(activity);
        if (container != null) {
            SwitchFragment switchLastFragment = container.getSwitchLastFragment();
            if (switchLastFragment != null) {
                with(activity).target(switchLastFragment,bundle).animEnable(useAnim).commit();
                return true;
            }else {
                container.getActivity().finish();
            }
        }
        return false;
    }
}
