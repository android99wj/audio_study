package com.wj.audio.utils;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rx.Observable;
import rx.functions.Func1;
import rx.subjects.PublishSubject;

/**
 * FileName: RxPermissions
 * description: 6.0权限管理
 * Author: 丁博洋
 * Date: 2016/9/10
 */
public class RxPermissions {

    private static final String TAG = "RxPermissions";
    static RxPermissions sSingleton;

    public static RxPermissions getInstance(Context ctx) {
        if (sSingleton == null) {
            sSingleton = new RxPermissions(ctx.getApplicationContext());
        }
        return sSingleton;
    }

    private Context mCtx;

    // 包含所有当前请求权限。
    private Map<String, PublishSubject<Permission>> mSubjects = new HashMap<>();
    private boolean mLogging;

    private RxPermissions(Context ctx) {
        mCtx = ctx;
    }

    public void setLogging(boolean logging) {
        mLogging = logging;
    }

    private void log(String message) {
        if (mLogging) {
            Log.d(TAG, message);
        }
    }

    /**
     * 如果从来没有请求的一个或多个权限,调用相关的框架方法要求用户如果他允许权限
     */
    public Observable.Transformer<Object, Boolean> ensure(final String... permissions) {
        return o -> request(o, permissions)
                .buffer(permissions.length)
                .flatMap(new Func1<List<Permission>, Observable<Boolean>>() {
                    @Override
                    public Observable<Boolean> call(List<Permission> permissions1) {
                        if (permissions1.isEmpty()) {
                            return Observable.empty();
                        }
                        // 返回true,所有权限授予。
                        for (Permission p : permissions1) {
                            if (!p.granted) {
                                return Observable.just(false);
                            }
                        }
                        return Observable.just(true);
                    }
                });
    }

    /**
     * 如果从来没有请求的一个或多个权限,调用相关的框架方法要求用户如果他允许权限
     */
    private Observable.Transformer<Object, Permission> ensureEach(final String... permissions) {
        return new Observable.Transformer<Object, Permission>() {
            @Override
            public Observable<Permission> call(Observable<Object> o) {
                return request(o, permissions);
            }
        };
    }

    /**
     * 立即请求权限
     * 必须在应用程序的初始化阶段调用
     */
    public Observable<Boolean> request(final String... permissions) {
        return Observable.just(null).compose(ensure(permissions));
    }

    /**
     * 立即请求权限,每一个请求权限
     * 必须在应用程序的初始化阶段调用
     */
    public Observable<Permission> requestEach(final String... permissions) {
        return Observable.just(null).compose(ensureEach(permissions));
    }

    private Observable<Permission> request(final Observable<?> trigger,
                                           final String... permissions) {
        if (permissions == null || permissions.length == 0) {
            throw new IllegalArgumentException("RxPermissions.request/requestEach requires at least one input permission");
        }
        return oneOf(trigger, pending(permissions))
                .flatMap(new Func1<Object, Observable<Permission>>() {
                    @Override
                    public Observable<Permission> call(Object o) {
                        return request_(permissions);
                    }
                });
    }

    private Observable<?> pending(final String... permissions) {
        for (String p : permissions) {
            if (!mSubjects.containsKey(p)) {
                return Observable.empty();
            }
        }
        return Observable.just(null);
    }

    private Observable<?> oneOf(Observable<?> trigger, Observable<?> pending) {
        if (trigger == null) {
            return Observable.just(null);
        }
        return Observable.merge(trigger, pending);
    }

    @TargetApi(Build.VERSION_CODES.M)
    private Observable<Permission> request_(final String... permissions) {

        List<Observable<Permission>> list = new ArrayList<>(permissions.length);
        List<String> unrequestedPermissions = new ArrayList<>();

        // 在多个权限的情况下,我们为每个人创造一个可观测的。
        // 最后,可见结合独特的响应。
        for (String permission : permissions) {
            log("Requesting permission " + permission);
            if (isGranted(permission)) {
                // 返回一个对象授予许可。
                list.add(Observable.just(new Permission(permission, true)));
                continue;
            }

            if (isRevoked(permission)) {
                //返回一个拒绝对象。
                list.add(Observable.just(new Permission(permission, false)));
                continue;
            }

            PublishSubject<Permission> subject = mSubjects.get(permission);
            // 如果不存在创建一个新的主题
            if (subject == null) {
                unrequestedPermissions.add(permission);
                subject = PublishSubject.create();
                mSubjects.put(permission, subject);
            }

            list.add(subject);
        }

        if (!unrequestedPermissions.isEmpty()) {
            startShadowActivity(unrequestedPermissions
                    .toArray(new String[unrequestedPermissions.size()]));
        }
        return Observable.concat(Observable.from(list));
    }

    public Observable<Boolean> shouldShowRequestPermissionRationale(final Activity activity,
                                                                    final String... permissions) {
        if (!isMarshmallow()) {
            return Observable.just(false);
        }
        return Observable.just(shouldShowRequestPermissionRationale_(activity, permissions));
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean shouldShowRequestPermissionRationale_(final Activity activity,
                                                          final String... permissions) {
        for (String p : permissions) {
            if (!isGranted(p) && !activity.shouldShowRequestPermissionRationale(p)) {
                return false;
            }
        }
        return true;
    }

    private void startShadowActivity(String[] permissions) {
        log("startShadowActivity " + TextUtils.join(", ", permissions));
        Intent intent = new Intent(mCtx, ShadowActivity.class);
        intent.putExtra("permissions", permissions);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mCtx.startActivity(intent);
    }

    /**
     * 返回true,如果已经获得许可。
     */
    private boolean isGranted(String permission) {
        return !isMarshmallow() || isGranted_(permission);
    }

    /**
     * 返回true,如果政策许可被撤销。
     */
    private boolean isRevoked(String permission) {
        return isMarshmallow() && isRevoked_(permission);
    }

    private boolean isMarshmallow() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean isGranted_(String permission) {
        return mCtx.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean isRevoked_(String permission) {
        return mCtx.getPackageManager().isPermissionRevokedByPolicy(permission, mCtx.getPackageName());
    }

    void onRequestPermissionsResult(int requestCode,
                                    String permissions[], int[] grantResults) {
        for (int i = 0, size = permissions.length; i < size; i++) {
            log("onRequestPermissionsResult  " + permissions[i]);
            // 找到相应的主题
            PublishSubject<Permission> subject = mSubjects.get(permissions[i]);
            if (subject == null) {
                throw new IllegalStateException("RxPermissions.onRequestPermissionsResult invoked but didn't find the corresponding permission request.");
            }
            mSubjects.remove(permissions[i]);
            boolean granted = grantResults[i] == PackageManager.PERMISSION_GRANTED;
            subject.onNext(new Permission(permissions[i], granted));
            subject.onCompleted();
        }
    }
}
