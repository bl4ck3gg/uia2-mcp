package uiautomator;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Instrumentation;
import android.app.UiAutomation;
import android.content.Context;
import android.os.Build;
import android.os.HandlerThread;
import mirror.android.app.UiAutomationConnection;
import mirror.android.app.UiAutomationM;
import proxy.Bridge;

/* loaded from: classes2.dex */
public class InstrumentShellWrapper extends Instrumentation {
    private static final long CONNECT_TIMEOUT_MILLIS = 5000;
    private static final String HANDLER_THREAD_NAME = "UiAutomatorHandlerThread";
    private static InstrumentShellWrapper sInstance;
    private final HandlerThread mHandlerThread;
    private UiAutomation mUiAutomation;
    private Object mUiAutomationConnection;

    private InstrumentShellWrapper() {
        HandlerThread handlerThread = new HandlerThread(HANDLER_THREAD_NAME);
        this.mHandlerThread = handlerThread;
        this.mUiAutomationConnection = UiAutomationConnection.ctor.newInstance();
        handlerThread.start();
    }

    public static InstrumentShellWrapper getInstance() {
        if (sInstance == null) {
            sInstance = new InstrumentShellWrapper();
        }
        return sInstance;
    }

    @Override // android.app.Instrumentation
    public Context getContext() {
        return Bridge.getInstance().getContext();
    }

    @Override // android.app.Instrumentation
    public Context getTargetContext() {
        return Bridge.getInstance().getContext();
    }

    public void destroy() {
        if (!this.mHandlerThread.isAlive()) {
            throw new IllegalStateException("Already disconnected!");
        }
        this.mHandlerThread.quit();
    }

    public void setCompressedLayoutHierarchy(boolean compressed) {
        AccessibilityServiceInfo info = this.mUiAutomation.getServiceInfo();
        if (compressed) {
            info.flags &= -3;
        } else {
            info.flags |= 2;
        }
        this.mUiAutomation.setServiceInfo(info);
    }

    @Override // android.app.Instrumentation
    public synchronized UiAutomation getUiAutomation(int flags) {
        if (this.mUiAutomation != null) {
            try {
                boolean destroyed = mirror.android.app.UiAutomation.isDestroyed.call(this.mUiAutomation, new Object[0]).booleanValue();
                if (!destroyed) {
                    return this.mUiAutomation;
                }
            } catch (Throwable ignored) {
                return this.mUiAutomation;
            }
        }
        if (this.mUiAutomationConnection == null) {
            return null;
        }
        this.mUiAutomation = mirror.android.app.UiAutomation.ctor.newInstance(this.mHandlerThread.getLooper(), this.mUiAutomationConnection);
        if (Build.VERSION.SDK_INT > 30) {
            try {
                mirror.android.app.UiAutomation.connectWithTimeout.call(this.mUiAutomation, Integer.valueOf(flags), Long.valueOf(CONNECT_TIMEOUT_MILLIS));
                return this.mUiAutomation;
            } catch (Exception e) {
                mirror.android.app.UiAutomation.destroy.call(this.mUiAutomation, new Object[0]);
                this.mUiAutomation = null;
                return null;
            }
        } else {
            mirror.android.app.UiAutomation.connect.call(this.mUiAutomation, Integer.valueOf(flags));
            return this.mUiAutomation;
        }
    }

    @Override // android.app.Instrumentation
    public UiAutomation getUiAutomation() {
        return getUiAutomation(0);
    }
}
