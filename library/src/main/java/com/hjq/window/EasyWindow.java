package com.hjq.window;

import android.accessibilityservice.AccessibilityService;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.FloatRange;
import android.support.annotation.IdRes;
import android.support.annotation.IntRange;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Px;
import android.support.annotation.StringRes;
import android.support.transition.Slide.GravityFlag;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.hjq.window.draggable.AbstractWindowDraggableRule;
import com.hjq.window.draggable.MovingWindowDraggableRule;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *    author : Android 轮子哥
 *    github : https://github.com/getActivity/EasyWindow
 *    time   : 2019/01/04
 *    desc   : 悬浮窗框架
 *    doc    : https://developer.android.google.cn/reference/android/view/WindowManager.html
 *             https://developer.android.google.cn/reference/kotlin/android/view/WindowManager.LayoutParams?hl=en
 */
@SuppressWarnings({"unchecked", "unused", "UnusedReturnValue"})
public class EasyWindow<X extends EasyWindow<?>> implements Runnable,
        ScreenOrientationMonitor.OnScreenOrientationCallback {

    @NonNull
    private static final Handler HANDLER = new Handler(Looper.getMainLooper());

    @NonNull
    private static final List<EasyWindow<?>> sWindowInstanceSet = new ArrayList<>();

    /**
     * 基于 Activity 创建一个 EasyWindow 实例
     */
    @SuppressWarnings("rawtypes")
    public static EasyWindow with(@NonNull Activity activity) {
        return new EasyWindow(activity);
    }

    /**
     * 基于全局创建一个 EasyWindow 实例，需要悬浮窗权限
     */
    @SuppressWarnings("rawtypes")
    public static EasyWindow with(@NonNull Application application) {
        return new EasyWindow(application);
    }

    /**
     * 创建一个无障碍 EasyWindow 实例
     *
     * API 22 及以上版本需要开启无障碍功能，API 22 以下需要有悬浮窗权限
     * 备注：由于悬浮窗是 Android 6.0 版本才出现的，目前有两种解决方案（任选其一即可）：
     * 1. 直接不判断和申请悬浮窗权限，在 Android 6.0 是不会出现崩溃的，顶多悬浮窗不展示
     * 2. 可以考虑使用 XXPermissions 判断和申请悬浮窗权限，里面兼容了大部分国产旧系统的悬浮窗权限判断和申请
     */
    @SuppressWarnings("rawtypes")
    public static EasyWindow with(@NonNull AccessibilityService service) {
        return new EasyWindow(service);
    }

    /**
     * 取消所有正在显示的悬浮窗
     */
    public static synchronized void cancelAllWindow() {
        Iterator<EasyWindow<?>> iterator = sWindowInstanceSet.iterator();
        while (iterator.hasNext()) {
            EasyWindow<?> easyWindow = iterator.next();
            if (easyWindow == null) {
                continue;
            }
            easyWindow.cancel();
        }
    }

    /**
     * 取消特定类名的悬浮窗
     */
    public static synchronized void cancelWindowByClass(@Nullable Class<? extends EasyWindow<?>> clazz) {
        if (clazz == null) {
            return;
        }
        Iterator<EasyWindow<?>> iterator = sWindowInstanceSet.iterator();
        while (iterator.hasNext()) {
            EasyWindow<?> easyWindow = iterator.next();
            if (easyWindow == null) {
                continue;
            }
            if (!clazz.equals(easyWindow.getClass())) {
                continue;
            }
            easyWindow.cancel();
        }
    }

    /**
     * 取消特定标记的悬浮窗
     */
    public static synchronized void cancelWindowByTag(@Nullable String tag) {
        if (tag == null) {
            return;
        }
        Iterator<EasyWindow<?>> iterator = sWindowInstanceSet.iterator();
        while (iterator.hasNext()) {
            EasyWindow<?> easyWindow = iterator.next();
            if (easyWindow == null) {
                continue;
            }
            if (!tag.equals(easyWindow.getTag())) {
                continue;
            }
            easyWindow.cancel();
        }
    }

    /**
     * 显示所有已取消但未回收的悬浮窗
     */
    public static synchronized void showAllWindow() {
        Iterator<EasyWindow<?>> iterator = sWindowInstanceSet.iterator();
        while (iterator.hasNext()) {
            EasyWindow<?> easyWindow = iterator.next();
            if (easyWindow == null) {
                continue;
            }
            easyWindow.show();
        }
    }

    /**
     * 显示特定类名已取消但未回收的悬浮窗
     */
    public static synchronized void showWindowByClass(@Nullable Class<? extends EasyWindow<?>> clazz) {
        if (clazz == null) {
            return;
        }
        Iterator<EasyWindow<?>> iterator = sWindowInstanceSet.iterator();
        while (iterator.hasNext()) {
            EasyWindow<?> easyWindow = iterator.next();
            if (easyWindow == null) {
                continue;
            }
            if (!clazz.equals(easyWindow.getClass())) {
                continue;
            }
            easyWindow.show();
        }
    }

    /**
     * 显示特定标记已取消但未回收的悬浮窗
     */
    public static synchronized void showWindowByTag(@Nullable String tag) {
        if (tag == null) {
            return;
        }
        Iterator<EasyWindow<?>> iterator = sWindowInstanceSet.iterator();
        while (iterator.hasNext()) {
            EasyWindow<?> easyWindow = iterator.next();
            if (easyWindow == null) {
                continue;
            }
            if (!tag.equals(easyWindow.getTag())) {
                continue;
            }
            easyWindow.show();
        }
    }

    /**
     * 回收所有正在显示的悬浮窗
     */
    public static synchronized void recycleAllWindow() {
        Iterator<EasyWindow<?>> iterator = sWindowInstanceSet.iterator();
        while (iterator.hasNext()) {
            EasyWindow<?> easyWindow = iterator.next();
            if (easyWindow == null) {
                continue;
            }
            // 这里解释一下，为什么要使用迭代器移除，如果不那么做的话
            // easyWindow.recycle 方法里面会再移除一次
            // 当前又是一个 while 循环，可能会出现角标越界的情况
            iterator.remove();
            easyWindow.recycle();
        }
    }

    /**
     * 回收特定类名的悬浮窗
     */
    public static synchronized void recycleWindowByClass(@Nullable Class<? extends EasyWindow<?>> clazz) {
        if (clazz == null) {
            return;
        }
        Iterator<EasyWindow<?>> iterator = sWindowInstanceSet.iterator();
        while (iterator.hasNext()) {
            EasyWindow<?> easyWindow = iterator.next();
            if (easyWindow == null) {
                continue;
            }
            if (!clazz.equals(easyWindow.getClass())) {
                continue;
            }
            // 这里解释一下，为什么要使用迭代器移除，如果不那么做的话
            // easyWindow.recycle 方法里面会再移除一次
            // 当前又是一个 while 循环，可能会出现角标越界的情况
            iterator.remove();
            easyWindow.recycle();
        }
    }

    /**
     * 回收特定标记的悬浮窗
     */
    public static synchronized void recycleWindowByTag(@Nullable String tag) {
        if (tag == null) {
            return;
        }

        Iterator<EasyWindow<?>> iterator = sWindowInstanceSet.iterator();
        while (iterator.hasNext()) {
            EasyWindow<?> easyWindow = iterator.next();
            if (easyWindow == null) {
                continue;
            }
            if (!tag.equals(easyWindow.getTag())) {
                continue;
            }
            // 这里解释一下，为什么要使用迭代器移除，如果不那么做的话
            // easyWindow.recycle 方法里面会再移除一次
            // 当前又是一个 while 循环，可能会出现角标越界的情况
            iterator.remove();
            easyWindow.recycle();
        }
    }

    /**
     * 判断当前是否有悬浮窗正在显示
     */
    public static synchronized boolean existAnyWindowShowing() {
        Iterator<EasyWindow<?>> iterator = sWindowInstanceSet.iterator();
        while (iterator.hasNext()) {
            EasyWindow<?> easyWindow = iterator.next();
            if (easyWindow == null) {
                continue;
            }

            if (easyWindow.isShowing()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断当前是否有特定类名的悬浮窗正在显示
     */
    public static synchronized boolean existWindowShowingByClass(@Nullable Class<? extends EasyWindow<?>> clazz) {
        if (clazz == null) {
            return false;
        }
        Iterator<EasyWindow<?>> iterator = sWindowInstanceSet.iterator();
        while (iterator.hasNext()) {
            EasyWindow<?> easyWindow = iterator.next();
            if (easyWindow == null) {
                continue;
            }
            if (!clazz.equals(easyWindow.getClass())) {
                continue;
            }
            if (easyWindow.isShowing()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断当前是否有特定标记的悬浮窗正在显示
     */
    public static synchronized boolean existWindowShowingByTag(@Nullable String tag) {
        if (tag == null) {
            return false;
        }

        Iterator<EasyWindow<?>> iterator = sWindowInstanceSet.iterator();
        while (iterator.hasNext()) {
            EasyWindow<?> easyWindow = iterator.next();
            if (easyWindow == null) {
                continue;
            }
            if (!tag.equals(easyWindow.getTag())) {
                continue;
            }
            if (easyWindow.isShowing()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取所有的悬浮窗
     */
    @NonNull
    public static synchronized List<EasyWindow<?>> getAllWindowInstance() {
        return sWindowInstanceSet;
    }

    /**
     * 获取特定类名的悬浮窗
     */
    @Nullable
    public static synchronized <X extends EasyWindow<?>> X getWindowInstanceByClass(@Nullable Class<X> clazz) {
        if (clazz == null) {
            return null;
        }
        for (EasyWindow<?> easyWindow : sWindowInstanceSet) {
            if (easyWindow == null) {
                continue;
            }
            if (!clazz.equals(easyWindow.getClass())) {
                continue;
            }
            return (X) easyWindow;
        }
        return null;
    }

    /**
     * 获取特定标记的悬浮窗
     */
    @Nullable
    public static synchronized EasyWindow<?> getWindowInstanceByTag(@Nullable String tag) {
        if (tag == null) {
            return null;
        }
        Iterator<EasyWindow<?>> iterator = sWindowInstanceSet.iterator();
        while (iterator.hasNext()) {
            EasyWindow<?> easyWindow = iterator.next();
            if (easyWindow == null) {
                continue;
            }
            if (!tag.equals(easyWindow.getTag())) {
                continue;
            }
            return easyWindow;
        }
        return null;
    }

    /** 上下文 */
    @Nullable
    private Context mContext;
    /** 根布局 */
    @Nullable
    private ViewGroup mDecorView;
    /** 悬浮窗 */
    @NonNull
    private WindowManager mWindowManager;
    /** 悬浮窗参数 */
    @NonNull
    private WindowManager.LayoutParams mWindowParams;

    /** 当前是否已经显示 */
    private boolean mShowing;
    /** 悬浮窗显示时长 */
    private int mDuration;
    /** 悬浮窗标记 */
    @Nullable
    private String mTag;
    /** 窗口生命周期管理 */
    private WindowLifecycleControl mWindowLifecycleControl;
    /** 自定义拖动处理 */
    @Nullable
    private AbstractWindowDraggableRule mWindowDraggableRule;
    /** 吐司显示和取消监听 */
    @Nullable
    private OnWindowLifecycle mOnWindowLifecycle;

    /** 屏幕旋转监听 */
    @Nullable
    private ScreenOrientationMonitor mScreenOrientationMonitor;

    /** 更新任务 */
    private final Runnable mUpdateRunnable = this::update;

    /**
     * 创建一个局部悬浮窗
     */
    @SuppressWarnings("deprecation")
    public EasyWindow(@NonNull Activity activity) {
        this((Context) activity);

        Window window = activity.getWindow();
        View decorView = window.getDecorView();
        WindowManager.LayoutParams params = activity.getWindow().getAttributes();
        if ((params.flags & WindowManager.LayoutParams.FLAG_FULLSCREEN) != 0 ||
                (decorView.getSystemUiVisibility() & View.SYSTEM_UI_FLAG_FULLSCREEN) != 0) {
            // 如果当前 Activity 是全屏模式，那么需要添加这个标记，否则会导致 WindowManager 在某些机型上移动不到状态栏的位置上
            // 如果不想让状态栏显示的时候把 WindowManager 顶下来，可以添加 FLAG_LAYOUT_IN_SCREEN，但是会导致软键盘无法调整窗口位置
            addWindowFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // 如果是 Android 9.0，则需要对刘海屏进行适配，否则也会导致 WindowManager 移动不到刘海屏的位置上面
            setLayoutInDisplayCutoutMode(params.layoutInDisplayCutoutMode);
        }

        if (params.systemUiVisibility != 0) {
            setSystemUiVisibility(params.systemUiVisibility);
        }

        if (decorView.getSystemUiVisibility() != 0) {
            mDecorView.setSystemUiVisibility(decorView.getSystemUiVisibility());
        }

        // 跟随 Activity 的生命周期
        mWindowLifecycleControl = new WindowLifecycleControl(this, activity);
        // 注册 Activity 生命周期监听
        mWindowLifecycleControl.register();
    }

    /**
     * 创建一个全局悬浮窗
     */
    @SuppressWarnings("deprecation")
    public EasyWindow(@NonNull Application application) {
        this((Context) application);

        // 设置成全局的悬浮窗，注意需要先申请悬浮窗权限，推荐使用：https://github.com/getActivity/XXPermissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setWindowType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        } else {
            setWindowType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        }
    }

    /**
     * 创建一个无障碍悬浮窗
     */
    @SuppressWarnings("deprecation")
    public EasyWindow(@NonNull AccessibilityService service) {
        this((Context) service);

        if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP_MR1) {
            setWindowType(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY);
        } else {
            setWindowType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        }
    }

    private EasyWindow(@NonNull Context context) {
        mContext = context;
        mDecorView = new WindowLayout(context);
        mWindowManager = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE));
        // 配置一些默认的参数
        mWindowParams = new WindowManager.LayoutParams();
        mWindowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        mWindowParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        mWindowParams.format = PixelFormat.TRANSLUCENT;
        mWindowParams.windowAnimations = android.R.style.Animation_Toast;
        mWindowParams.packageName = context.getPackageName();
        // 设置触摸外层布局（除 WindowManager 外的布局，默认是 WindowManager 显示的时候外层不可触摸）
        // 需要注意的是设置了 FLAG_NOT_TOUCH_MODAL 必须要设置 FLAG_NOT_FOCUSABLE，否则就会导致用户按返回键无效
        mWindowParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        // 将当前实例添加到静态集合中
        sWindowInstanceSet.add(this);
    }

    /**
     * 设置悬浮窗口标记
     */
    public X setTag(@Nullable String tag) {
        mTag = tag;
        return (X) this;
    }

    /**
     * 设置悬浮窗宽度
     */
    public X setWidth(int width) {
        mWindowParams.width = width;
        if (mDecorView == null) {
            return (X) this;
        }
        if (mDecorView.getChildCount() > 0) {
            View contentView = mDecorView.getChildAt(0);
            ViewGroup.LayoutParams layoutParams = contentView.getLayoutParams();
            if (layoutParams != null && layoutParams.width != width) {
                layoutParams.width = width;
                contentView.setLayoutParams(layoutParams);
            }
        }
        postUpdate();
        return (X) this;
    }

    /**
     * 设置悬浮窗高度
     */
    public X setHeight(int height) {
        mWindowParams.height = height;
        if (mDecorView == null) {
            return (X) this;
        }
        if (mDecorView.getChildCount() > 0) {
            View contentView = mDecorView.getChildAt(0);
            ViewGroup.LayoutParams layoutParams = contentView.getLayoutParams();
            if (layoutParams != null && layoutParams.height != height) {
                layoutParams.height = height;
                contentView.setLayoutParams(layoutParams);
            }
        }
        postUpdate();
        return (X) this;
    }

    /**
     * 设置悬浮窗显示的重心
     */
    public X setGravity(@GravityFlag int gravity) {
        mWindowParams.gravity = gravity;
        postUpdate();
        post(() -> {
            if (mWindowDraggableRule != null) {
                mWindowDraggableRule.refreshLocationCoordinate();
            }
        });
        return (X) this;
    }

    /**
     * 设置水平偏移量
     */
    public X setXOffset(@Px int px) {
        mWindowParams.x = px;
        postUpdate();
        post(() -> {
            if (mWindowDraggableRule != null) {
                mWindowDraggableRule.refreshLocationCoordinate();
            }
        });
        return (X) this;
    }

    /**
     * 设置垂直偏移量
     */
    public X setYOffset(@Px int px) {
        mWindowParams.y = px;
        postUpdate();
        post(() -> {
            if (mWindowDraggableRule != null) {
                mWindowDraggableRule.refreshLocationCoordinate();
            }
        });
        return (X) this;
    }

    /**
     * 设置悬浮窗外层是否可触摸
     */
    public X setOutsideTouchable(boolean touchable) {
        int flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        if (touchable) {
            addWindowFlags(flags);
        } else {
            removeWindowFlags(flags);
        }
        postUpdate();
        return (X) this;
    }

    /**
     * 设置悬浮窗背景阴影强度
     *
     * @param amount        阴影强度值，填写 0 到 1 之间的值
     */
    public X setBackgroundDimAmount(@FloatRange(from = 0.0, to = 1.0) float amount) {
        if (amount < 0 || amount > 1) {
            throw new IllegalArgumentException("amount must be a value between 0 and 1");
        }
        mWindowParams.dimAmount = amount;
        int flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        if (amount != 0) {
            addWindowFlags(flags);
        } else {
            removeWindowFlags(flags);
        }
        postUpdate();
        return (X) this;
    }

    /**
     * 添加窗口标记
     */
    public X addWindowFlags(int flags) {
        mWindowParams.flags |= flags;
        postUpdate();
        return (X) this;
    }

    /**
     * 移除窗口标记
     */
    public X removeWindowFlags(int flags) {
        mWindowParams.flags &= ~flags;
        postUpdate();
        return (X) this;
    }

    /**
     * 设置窗口标记
     */
    public X setWindowFlags(int flags) {
        mWindowParams.flags = flags;
        postUpdate();
        return (X) this;
    }

    /**
     * 是否存在某个窗口标记
     */
    public boolean hasWindowFlags(int flags) {
        return (mWindowParams.flags & flags) != 0;
    }

    /**
     * 设置悬浮窗的显示类型
     */
    public X setWindowType(int type) {
        mWindowParams.type = type;
        postUpdate();
        return (X) this;
    }

    /**
     * 设置动画样式
     */
    public X setAnimStyle(int id) {
        mWindowParams.windowAnimations = id;
        postUpdate();
        return (X) this;
    }

    /**
     * 设置软键盘模式
     *
     * {@link WindowManager.LayoutParams#SOFT_INPUT_STATE_UNSPECIFIED}：没有指定状态,系统会选择一个合适的状态或依赖于主题的设置
     * {@link WindowManager.LayoutParams#SOFT_INPUT_STATE_UNCHANGED}：不会改变软键盘状态
     * {@link WindowManager.LayoutParams#SOFT_INPUT_STATE_HIDDEN}：当用户进入该窗口时，软键盘默认隐藏
     * {@link WindowManager.LayoutParams#SOFT_INPUT_STATE_ALWAYS_HIDDEN}：当窗口获取焦点时，软键盘总是被隐藏
     * {@link WindowManager.LayoutParams#SOFT_INPUT_ADJUST_RESIZE}：当软键盘弹出时，窗口会调整大小
     * {@link WindowManager.LayoutParams#SOFT_INPUT_ADJUST_PAN}：当软键盘弹出时，窗口不需要调整大小，要确保输入焦点是可见的
     */
    public X setSoftInputMode(int mode) {
        mWindowParams.softInputMode = mode;
        // 如果设置了不能触摸，则擦除这个标记，否则会导致无法弹出输入法
        removeWindowFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        postUpdate();
        return (X) this;
    }

    /**
     * 设置悬浮窗 Token
     */
    public X setWindowToken(@Nullable IBinder token) {
        mWindowParams.token = token;
        postUpdate();
        return (X) this;
    }

    /**
     * 设置悬浮窗透明度
     *
     * @param alpha         值为 1 表示完全不透明，值为 0 表示完全透明
     */
    public X setWindowAlpha(@FloatRange(from = 0.0, to = 1.0) float alpha) {
        mWindowParams.alpha = alpha;
        postUpdate();
        return (X) this;
    }

    /**
     * 设置垂直间距
     */
    public X setVerticalMargin(float verticalMargin) {
        mWindowParams.verticalMargin = verticalMargin;
        postUpdate();
        return (X) this;
    }

    /**
     * 设置水平间距
     */
    public X setHorizontalMargin(float horizontalMargin) {
        mWindowParams.horizontalMargin = horizontalMargin;
        postUpdate();
        return (X) this;
    }

    /**
     * 设置位图格式
     */
    public X setBitmapFormat(int format) {
        mWindowParams.format = format;
        postUpdate();
        return (X) this;
    }

    /**
     * 设置状态栏的可见性
     */
    @SuppressWarnings("deprecation")
    public X setSystemUiVisibility(int systemUiVisibility) {
        mWindowParams.systemUiVisibility = systemUiVisibility;
        postUpdate();
        return (X) this;
    }

    /**
     * 设置垂直权重
     */
    public X setVerticalWeight(float verticalWeight) {
        mWindowParams.verticalWeight = verticalWeight;
        postUpdate();
        return (X) this;
    }

    /**
     * 设置挖孔屏下的显示模式
     *
     * {@link WindowManager.LayoutParams#LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT}：默认模式
     * {@link WindowManager.LayoutParams#LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES}：内容会延伸至屏幕短边的挖孔区域
     * {@link WindowManager.LayoutParams#LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER}：内容不会延伸到挖孔区域
     * {@link WindowManager.LayoutParams#LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS}：内容会延伸到挖孔区域
     *
     * 文档地址：https://developer.android.com/develop/ui/views/layout/display-cutout?hl=zh-cn
     */
    public X setLayoutInDisplayCutoutMode(int mode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            mWindowParams.layoutInDisplayCutoutMode = mode;
            postUpdate();
        }
        return (X) this;
    }

    /**
     * 设置悬浮窗在哪个显示屏上显示
     */
    public X setPreferredDisplayModeId(int id) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mWindowParams.preferredDisplayModeId = id;
            postUpdate();
        }
        return (X) this;
    }

    /**
     * 设置悬浮窗标题
     */
    public X setWindowTitle(@Nullable CharSequence title) {
        if (title == null) {
            title = "";
        }
        mWindowParams.setTitle(title);
        postUpdate();
        return (X) this;
    }

    /**
     * 设置屏幕的亮度
     *
     * @param screenBrightness      小于 0 的值（默认值）表示使用首选的屏幕亮度，0 到 1 将亮度从暗调整为全亮
     *
     * {@link WindowManager.LayoutParams#BRIGHTNESS_OVERRIDE_NONE}：表示当前窗口的亮度值未被覆盖，并且应使用正常亮度策略
     * {@link WindowManager.LayoutParams#BRIGHTNESS_OVERRIDE_OFF}：当前窗口位于前面时，屏幕或按钮背光亮度应设置为最低值
     * {@link WindowManager.LayoutParams#BRIGHTNESS_OVERRIDE_FULL}：当前窗口位于前面时，屏幕或按钮背光亮度应设置为最高值
     */
    public X setScreenBrightness(@FloatRange(from = -1.0, to = 1.0) float screenBrightness) {
        mWindowParams.screenBrightness = screenBrightness;
        postUpdate();
        return (X) this;
    }

    /**
     * 设置键盘背光的亮度
     *
     * @param buttonBrightness      小于 0 的值（默认值）表示使用标准键盘背光行为，0 到 1 将亮度从暗调整为全亮。
     *
     * {@link WindowManager.LayoutParams#BRIGHTNESS_OVERRIDE_NONE}：表示当前窗口的亮度值未被覆盖，并且应使用正常亮度策略
     * {@link WindowManager.LayoutParams#BRIGHTNESS_OVERRIDE_OFF}：当前窗口位于前面时，屏幕或按钮背光亮度应设置为最低值
     * {@link WindowManager.LayoutParams#BRIGHTNESS_OVERRIDE_FULL}：当前窗口位于前面时，屏幕或按钮背光亮度应设置为最高值
     */
    public X setButtonBrightness(@FloatRange(from = -1.0, to = 1.0) float buttonBrightness) {
        mWindowParams.buttonBrightness = buttonBrightness;
        postUpdate();
        return (X) this;
    }

    /**
     * 设置悬浮窗的刷新率
     */
    public X setPreferredRefreshRate(float preferredRefreshRate) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mWindowParams.preferredRefreshRate = preferredRefreshRate;
            postUpdate();
        }
        return (X) this;
    }

    /**
     * 设置悬浮窗的颜色模式
     */
    public X setColorMode(int colorMode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mWindowParams.setColorMode(colorMode);
            postUpdate();
        }
        return (X) this;
    }

    /**
     * 设置悬浮窗背后的高斯模糊半径大小（Android 12 才有的）
     *
     * @param blurBehindRadius          高斯模糊半径大小（以像素为单位）
     */
    public X setBlurBehindRadius(@IntRange(from = 0) int blurBehindRadius) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            mWindowParams.setBlurBehindRadius(blurBehindRadius);
            addWindowFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
            postUpdate();
        }
        return (X) this;
    }

    /**
     * 设置悬浮窗屏幕方向
     *
     * 自适应：{@link ActivityInfo#SCREEN_ORIENTATION_UNSPECIFIED}
     * 横屏：{@link ActivityInfo#SCREEN_ORIENTATION_LANDSCAPE}
     * 竖屏：{@link ActivityInfo#SCREEN_ORIENTATION_PORTRAIT}
     */
    public X setScreenOrientation(int orientation) {
        mWindowParams.screenOrientation = orientation;
        postUpdate();
        return (X) this;
    }

    /**
     * 重新设置 WindowManager 参数集
     */
    public X setWindowParams(@NonNull WindowManager.LayoutParams params) {
        mWindowParams = params;
        postUpdate();
        return (X) this;
    }

    /**
     * 设置随意拖动
     */
    public X setWindowDraggableRule() {
        return setWindowDraggableRule(new MovingWindowDraggableRule());
    }

    /**
     * 设置拖动规则
     *
     * @param draggableRule         拖拽规则对象
     */
    public X setWindowDraggableRule(@Nullable AbstractWindowDraggableRule draggableRule) {
        mWindowDraggableRule = draggableRule;
        if (draggableRule != null) {
            // 如果当前是否设置了不可触摸，如果是就擦除掉这个标记
            removeWindowFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            // 如果当前是否设置了可移动窗口到屏幕之外，如果是就擦除这个标记
            removeWindowFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

            if (isShowing()) {
                update();
                draggableRule.start(this);
            }
        }

        Resources resources = null;
        Configuration configuration = null;
        if (mContext != null) {
            resources = mContext.getResources();
        }

        if (resources != null) {
            configuration = resources.getConfiguration();
        }

        if (configuration != null) {
            if (mScreenOrientationMonitor == null) {
                mScreenOrientationMonitor = new ScreenOrientationMonitor(configuration.orientation);
            }
            mScreenOrientationMonitor.registerCallback(mContext, this);
        }

        return (X) this;
    }

    /**
     * 限定显示时长
     */
    public X setDuration(@IntRange(from = 0) int duration) {
        mDuration = duration;
        if (isShowing() && mDuration != 0) {
            removeCallbacks(this);
            postDelayed(this, mDuration);
        }
        return (X) this;
    }

    /**
     * 指定 WindowManager 对象
     */
    public X setWindowManager(@NonNull WindowManager windowManager) {
        mWindowManager = windowManager;
        return (X) this;
    }

    /**
     * 设置生命周期监听
     */
    public X setOnWindowLifecycle(@Nullable OnWindowLifecycle listener) {
        mOnWindowLifecycle = listener;
        return (X) this;
    }

    /**
     * 设置根布局（一般情况下推荐使用 {@link #setContentView} 方法来填充布局）
     */
    public X setDecorView(@NonNull ViewGroup viewGroup) {
        mDecorView = viewGroup;
        return (X) this;
    }

    /**
     * 设置内容布局
     */
    public X setContentView(@LayoutRes int layoutId) {
        return setContentView(layoutId, null);
    }

    public X setContentView(@LayoutRes int layoutId, @Nullable OnWindowLayoutInflateListener listener) {
        if (mContext == null || mDecorView == null) {
            return (X) this;
        }
        View view = LayoutInflater.from(mContext).inflate(layoutId, mDecorView, false);
        if (listener != null) {
            listener.onWindowLayoutInflateFinished(this, view, layoutId, mDecorView);
        }
        return setContentView(view);
    }

    public X setContentView(@NonNull View view) {
        if (mDecorView == null) {
            return (X) this;
        }

        if (mDecorView.getChildCount() > 0) {
            mDecorView.removeAllViews();
        }
        mDecorView.addView(view);

        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams marginLayoutParams = ((ViewGroup.MarginLayoutParams) layoutParams);
            // 清除 Margin，因为 WindowManager 没有这一属性可以设置，并且会跟根布局相冲突
            marginLayoutParams.topMargin = 0;
            marginLayoutParams.bottomMargin = 0;
            marginLayoutParams.leftMargin = 0;
            marginLayoutParams.rightMargin = 0;
        }

        // 如果当前没有设置重心，就自动获取布局重心
        if (mWindowParams.gravity == Gravity.NO_GRAVITY) {
            if (layoutParams instanceof FrameLayout.LayoutParams) {
                int gravity = ((FrameLayout.LayoutParams) layoutParams).gravity;
                if (gravity != FrameLayout.LayoutParams.UNSPECIFIED_GRAVITY) {
                    mWindowParams.gravity = gravity;
                }
            } else if (layoutParams instanceof LinearLayout.LayoutParams) {
                int gravity = ((LinearLayout.LayoutParams) layoutParams).gravity;
                if (gravity != FrameLayout.LayoutParams.UNSPECIFIED_GRAVITY) {
                    mWindowParams.gravity = gravity;
                }
            }

            if (mWindowParams.gravity == Gravity.NO_GRAVITY) {
                // 默认重心是居中
                mWindowParams.gravity = Gravity.CENTER;
            }
        }

        if (layoutParams != null) {
            if (mWindowParams.width == WindowManager.LayoutParams.WRAP_CONTENT &&
                    mWindowParams.height == WindowManager.LayoutParams.WRAP_CONTENT) {
                // 如果当前 Dialog 的宽高设置了自适应，就以布局中设置的宽高为主
                mWindowParams.width = layoutParams.width;
                mWindowParams.height = layoutParams.height;
            } else {
                // 如果当前通过代码动态设置了宽高，则以动态设置的为主
                layoutParams.width = mWindowParams.width;
                layoutParams.height = mWindowParams.height;
            }
        }

        postUpdate();
        return (X) this;
    }

    public void showAsDropDown(@NonNull View anchorView) {
        showAsDropDown(anchorView, Gravity.BOTTOM);
    }

    public void showAsDropDown(@NonNull View anchorView, int showGravity) {
        showAsDropDown(anchorView, showGravity, 0 , 0);
    }

    /**
     * 将悬浮窗显示在某个 View 下方（和 PopupWindow 同名方法作用类似）
     *
     * @param anchorView            锚点 View
     * @param showGravity           显示重心
     * @param xOff                  水平偏移
     * @param yOff                  垂直偏移
     */
    public void showAsDropDown(@NonNull View anchorView, int showGravity, int xOff, int yOff) {
        if (mDecorView == null) {
            return;
        }

        if (mDecorView.getChildCount() == 0) {
            throw new IllegalArgumentException("WindowParams and view cannot be empty");
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            // 适配布局反方向
            showGravity = Gravity.getAbsoluteGravity(showGravity,
                    anchorView.getResources().getConfiguration().getLayoutDirection());
        }

        int[] anchorViewLocation = new int[2];
        anchorView.getLocationOnScreen(anchorViewLocation);

        Rect windowVisibleRect = new Rect();
        anchorView.getWindowVisibleDisplayFrame(windowVisibleRect);

        mWindowParams.gravity = Gravity.TOP | Gravity.START;
        mWindowParams.x = anchorViewLocation[0] - windowVisibleRect.left + xOff;
        mWindowParams.y = anchorViewLocation[1] - windowVisibleRect.top + yOff;

        if ((showGravity & Gravity.LEFT) == Gravity.LEFT) {
            int rootViewWidth = mDecorView.getWidth();
            if (rootViewWidth == 0) {
                rootViewWidth = mDecorView.getMeasuredWidth();
            }
            if (rootViewWidth == 0) {
                mDecorView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                rootViewWidth = mDecorView.getMeasuredWidth();
            }
            mWindowParams.x -= rootViewWidth;
        } else if ((showGravity & Gravity.RIGHT) == Gravity.RIGHT) {
            mWindowParams.x += anchorView.getWidth();
        }

        if ((showGravity & Gravity.TOP) == Gravity.TOP) {
            int rootViewHeight = mDecorView.getHeight();
            if (rootViewHeight == 0) {
                rootViewHeight = mDecorView.getMeasuredHeight();
            }
            if (rootViewHeight == 0) {
                mDecorView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                rootViewHeight = mDecorView.getMeasuredHeight();
            }
            mWindowParams.y -= rootViewHeight;
        } else if ((showGravity & Gravity.BOTTOM) == Gravity.BOTTOM) {
            mWindowParams.y += anchorView.getHeight();
        }

        show();
    }

    /**
     * 显示悬浮窗
     */
    public void show() {
        if (mDecorView == null) {
            return;
        }

        if (mDecorView.getChildCount() == 0) {
            throw new IllegalArgumentException("WindowParams and view cannot be empty");
        }

        // 如果当前已经显示则进行更新
        if (mShowing) {
            update();
            return;
        }

        if (mContext instanceof Activity) {
            Activity activity = ((Activity) mContext);
            if (activity.isFinishing() ||
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 &&
                            activity.isDestroyed())) {
                return;
            }
        }

        try {
            // 如果 View 已经被添加的情况下，就先把 View 移除掉
            if (mDecorView.getParent() != null) {
                mWindowManager.removeViewImmediate(mDecorView);
            }
            mWindowManager.addView(mDecorView, mWindowParams);
            // 当前已经显示
            mShowing = true;
            // 如果当前限定了显示时长
            if (mDuration != 0) {
                removeCallbacks(this);
                postDelayed(this, mDuration);
            }
            // 如果设置了拖拽规则
            if (mWindowDraggableRule != null) {
                mWindowDraggableRule.start(this);
            }

            // 回调监听
            if (mOnWindowLifecycle != null) {
                mOnWindowLifecycle.onWindowShow(this);
            }

        } catch (NullPointerException | IllegalStateException |
                IllegalArgumentException | WindowManager.BadTokenException e) {
            // 如果这个 View 对象被重复添加到 WindowManager 则会抛出异常
            // java.lang.IllegalStateException: View has already been added to the window manager.
            e.printStackTrace();
        }
    }

    /**
     * 销毁悬浮窗
     */
    public void cancel() {
        if (!mShowing) {
            return;
        }

        try {

            // 如果当前 WindowManager 没有附加这个 View 则会抛出异常
            // java.lang.IllegalArgumentException: View not attached to window manager
            mWindowManager.removeViewImmediate(mDecorView);

            // 移除销毁任务
            removeCallbacks(this);

            // 回调监听
            if (mOnWindowLifecycle != null) {
                mOnWindowLifecycle.onWindowCancel(this);
            }

        } catch (NullPointerException | IllegalArgumentException | IllegalStateException e) {
            e.printStackTrace();
        } finally {
            // 当前没有显示
            mShowing = false;
        }
    }

    /**
     * 延迟更新悬浮窗
     */
    public void postUpdate() {
        if (!isShowing()) {
            return;
        }
        removeCallbacks(mUpdateRunnable);
        post(mUpdateRunnable);
    }

    /**
     * 更新悬浮窗
     */
    public void update() {
        if (!isShowing()) {
            return;
        }
        try {
            // 更新 WindowManger 的显示
            mWindowManager.updateViewLayout(mDecorView, mWindowParams);
            if (mOnWindowLifecycle == null) {
                return;
            }
            mOnWindowLifecycle.onWindowUpdate(this);
        } catch (IllegalArgumentException e) {
            // 当 WindowManager 已经消失时调用会发生崩溃
            // IllegalArgumentException: View not attached to window manager
            e.printStackTrace();
        }
    }

    /**
     * 回收释放
     */
    public void recycle() {
        if (isShowing()) {
            cancel();
        }
        if (mScreenOrientationMonitor != null) {
            mScreenOrientationMonitor.unregisterCallback(mContext);
            mScreenOrientationMonitor = null;
        }
        if (mOnWindowLifecycle != null) {
            mOnWindowLifecycle.onWindowRecycle(this);
            mOnWindowLifecycle = null;
        }
        if (mWindowLifecycleControl != null) {
            mWindowLifecycleControl.unregister();
            mWindowLifecycleControl = null;
        }
        if (mWindowDraggableRule != null) {
            mWindowDraggableRule.recycle();
            mWindowDraggableRule = null;
        }
        mContext = null;
        mDecorView = null;
        // 将当前实例从静态集合中移除
        sWindowInstanceSet.remove(this);
    }

    /**
     * 获取窗口可见性
     */
    public int getWindowVisibility() {
        if (mDecorView == null) {
            return View.GONE;
        }
        return mDecorView.getVisibility();
    }

    /**
     * 设置窗口是否可见
     *
     * @param visibility            窗口可见性类型，有三种类型：
     *                              {@link View#VISIBLE}
     *                              {@link View#INVISIBLE}
     *                              {@link View#GONE}
     */
    public X setWindowVisibility(int visibility) {
        if (mDecorView == null) {
            return (X) this;
        }
        if (getWindowVisibility() == visibility) {
            return (X) this;
        }
        mDecorView.setVisibility(visibility);
        if (mOnWindowLifecycle != null) {
            mOnWindowLifecycle.onWindowVisibilityChanged(this, visibility);
        }
        return (X) this;
    }

    /**
     * 当前是否已经显示
     */
    public boolean isShowing() {
        return mShowing;
    }

    /**
     * 获取 WindowManager 对象
     */
    @NonNull
    public WindowManager getWindowManager() {
        return mWindowManager;
    }

    /**
     * 获取 WindowManager 参数集
     */
    @NonNull
    public WindowManager.LayoutParams getWindowParams() {
        return mWindowParams;
    }

    /**
     * 获取当前的拖拽规则对象（可能为空）
     */
    @Nullable
    public AbstractWindowDraggableRule getWindowDraggableRule() {
        return mWindowDraggableRule;
    }

    /**
     * 获取上下文对象
     */
    @Nullable
    public Context getContext() {
        return mContext;
    }

    /**
     * 获取根布局
     */
    @Nullable
    public ViewGroup getDecorView() {
        return mDecorView;
    }

    /**
     * 获取内容布局（可能为空）
     */
    @Nullable
    public View getContentView() {
        if (mDecorView == null) {
            return null;
        }
        if (mDecorView.getChildCount() == 0) {
            return null;
        }
        return mDecorView.getChildAt(0);
    }

    /**
     * 获取当前窗口 View 宽度
     */
    public int getViewWidth() {
        ViewGroup decorView = getDecorView();
        if (decorView == null) {
            return 0;
        }
        return decorView.getWidth();
    }

    /**
     * 获取当前窗口 View 高度
     */
    public int getViewHeight() {
        ViewGroup decorView = getDecorView();
        if (decorView == null) {
            return 0;
        }
        return decorView.getHeight();
    }

    /**
     * 根据 ViewId 获取 View（返回可能为空）
     */
    @Nullable
    public <V extends View> V findViewById(int id) {
        if (mDecorView == null) {
            return null;
        }
        return mDecorView.findViewById(id);
    }

    /**
     * 跳转 Activity
     */
    public void startActivity(@Nullable Class<? extends Activity> clazz) {
        if (clazz == null) {
            return;
        }
        startActivity(new Intent(mContext, clazz));
    }

    public void startActivity(@Nullable Intent intent) {
        if (intent == null || mContext == null) {
            return;
        }
        if (!(mContext instanceof Activity)) {
            // 如果当前的上下文不是 Activity，调用 startActivity 必须加入新任务栈的标记
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        mContext.startActivity(intent);
    }

    /**
     * 设置可见状态
     */
    public X setVisibility(@IdRes int viewId, int visibility) {
        View view = findViewById(viewId);
        if (view != null) {
            view.setVisibility(visibility);
        }
        return (X) this;
    }

    /**
     * 设置文本
     */
    public X setText(@IdRes int viewId, @StringRes int stringId) {
        if (mContext == null) {
            return (X) this;
        }
        return setText(viewId, mContext.getResources().getString(stringId));
    }

    public X setText(@IdRes int viewId, CharSequence text) {
        if (text == null) {
            text = "";
        }
        TextView textView = findViewById(viewId);
        if (textView != null) {
            textView.setText(text);
        }
        return (X) this;
    }

    /**
     * 设置字体颜色
     */
    public X setTextColor(@IdRes int viewId, @ColorInt int textColor) {
        TextView textView = findViewById(viewId);
        if (textView != null) {
            textView.setTextColor(textColor);
        }
        return (X) this;
    }

    /**
     * 设置字体大小
     */
    public X setTextSize(@IdRes int viewId, float textSize) {
        return setTextSize(viewId, TypedValue.COMPLEX_UNIT_SP, textSize);
    }

    public X setTextSize(@IdRes int viewId, int unit, float textSize) {
        TextView textView = findViewById(viewId);
        if (textView != null) {
            textView.setTextSize(unit, textSize);
        }
        return (X) this;
    }

    /**
     * 设置提示
     */
    public X setHint(@IdRes int viewId, @StringRes int stringId) {
        if (mContext == null) {
            return (X) this;
        }
        return setHint(viewId, mContext.getResources().getString(stringId));
    }

    public X setHint(@IdRes int viewId, CharSequence text) {
        TextView textView = findViewById(viewId);
        if (textView != null) {
            textView.setHint(text);
        }
        return (X) this;
    }

    /**
     * 设置提示文本颜色
     */
    public X setHintColor(@IdRes int viewId, @ColorInt int hintTextColor) {
        TextView textView = findViewById(viewId);
        if (textView != null) {
            textView.setHintTextColor(hintTextColor);
        }
        return (X) this;
    }

    /**
     * 设置背景
     */
    @SuppressWarnings("deprecation")
    public X setBackground(@IdRes int viewId, @DrawableRes int backgroundDrawableId) {
        if (mContext == null) {
            return (X) this;
        }
        Drawable drawable;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            drawable = mContext.getDrawable(backgroundDrawableId);
        } else {
            drawable = mContext.getResources().getDrawable(backgroundDrawableId);
        }
        return setBackground(viewId, drawable);
    }

    @SuppressWarnings("deprecation")
    public X setBackground(@IdRes int viewId, Drawable backgroundDrawable) {
        View view = findViewById(viewId);
        if (view != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                view.setBackground(backgroundDrawable);
            } else {
                view.setBackgroundDrawable(backgroundDrawable);
            }
        }
        return (X) this;
    }

    /**
     * 设置图片
     */
    @SuppressWarnings("deprecation")
    public X setImageDrawable(@IdRes int viewId, @DrawableRes int imageDrawableId) {
        if (mContext == null) {
            return (X) this;
        }
        Drawable drawable;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            drawable = mContext.getDrawable(imageDrawableId);
        } else {
            drawable = mContext.getResources().getDrawable(imageDrawableId);
        }
        return setImageDrawable(viewId, drawable);
    }

    public X setImageDrawable(@IdRes int viewId, Drawable imageDrawable) {
        ImageView imageView = findViewById(viewId);
        if (imageView != null) {
            imageView.setImageDrawable(imageDrawable);
        }
        return (X) this;
    }

    @NonNull
    public Handler getHandler() {
        return HANDLER;
    }

    @Nullable
    public String getTag() {
        return mTag;
    }

    /**
     * 延迟执行
     */
    public boolean post(Runnable runnable) {
        return postDelayed(runnable, 0);
    }

    /**
     * 延迟一段时间执行
     */
    public boolean postDelayed(Runnable runnable, long delayMillis) {
        if (delayMillis < 0) {
            delayMillis = 0;
        }
        return postAtTime(runnable, SystemClock.uptimeMillis() + delayMillis);
    }

    /**
     * 在指定的时间执行
     */
    public boolean postAtTime(Runnable runnable, long uptimeMillis) {
        // 发送和这个 WindowManager 相关的消息回调
        return HANDLER.postAtTime(runnable, this, uptimeMillis);
    }

    /**
     * 移除消息回调
     */
    public void removeCallbacks(Runnable runnable) {
        HANDLER.removeCallbacks(runnable);
    }

    public void removeCallbacksAndMessages() {
        HANDLER.removeCallbacksAndMessages(this);
    }

    /**
     * 设置点击事件
     */
    public X setOnClickListener(@Nullable OnViewClickListener<? extends View> listener) {
        return setOnClickListener(mDecorView, listener);
    }

    public X setOnClickListener(@IdRes int id, @Nullable OnViewClickListener<? extends View> listener) {
        return setOnClickListener(findViewById(id), listener);
    }

    private X setOnClickListener(@Nullable View view, @Nullable OnViewClickListener<? extends View> listener) {
        if (view == null) {
            return (X) this;
        }

        if (listener == null) {
            view.setOnClickListener(null);
            return (X) this;
        }

        // 如果当前是否设置了不可触摸，如果是就擦除掉
        removeWindowFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        view.setClickable(true);
        view.setOnClickListener(new ViewClickWrapper(this, listener));
        return (X) this;
    }

    /**
     * 设置长按事件
     */
    public X setOnLongClickListener(@Nullable OnViewLongClickListener<? extends View> listener) {
        return setOnLongClickListener(mDecorView, listener);
    }

    public X setOnLongClickListener(@IdRes int id, @Nullable OnViewLongClickListener<? extends View> listener) {
        return setOnLongClickListener(findViewById(id), listener);
    }

    private X setOnLongClickListener(@Nullable View view, @Nullable OnViewLongClickListener<? extends View> listener) {
        if (view == null) {
            return (X) this;
        }

        if (listener == null) {
            view.setOnLongClickListener(null);
            return (X) this;
        }

        // 如果当前是否设置了不可触摸，如果是就擦除掉
        removeWindowFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        view.setClickable(true);
        view.setOnLongClickListener(new ViewLongClickWrapper(this, listener));
        return (X) this;
    }

    /**
     * 设置触摸事件
     */
    public X setOnTouchListener(@Nullable OnViewTouchListener<? extends View> listener) {
        return setOnTouchListener(mDecorView, listener);
    }

    public X setOnTouchListener(@IdRes int id, @Nullable OnViewTouchListener<? extends View> listener) {
        return setOnTouchListener(findViewById(id), listener);
    }

    private X setOnTouchListener(@Nullable View view, @Nullable OnViewTouchListener<? extends View> listener) {
        if (view == null) {
            return (X) this;
        }

        if (listener == null) {
            view.setOnTouchListener(null);
            return (X) this;
        }

        // 当前是否设置了不可触摸，如果是就擦除掉
        removeWindowFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        view.setEnabled(true);
        view.setOnTouchListener(new ViewTouchWrapper(this, listener));
        return (X) this;
    }

    /**
     * {@link Runnable}
     */
    @Override
    public void run() {
        cancel();
    }

    /**
     * {@link ScreenOrientationMonitor.OnScreenOrientationCallback}
     */
    @Override
    public void onScreenOrientationChange(int newOrientation) {
        if (!isShowing()) {
            return;
        }
        if (mWindowDraggableRule == null) {
            return;
        }
        mWindowDraggableRule.onScreenOrientationChange();
    }
}