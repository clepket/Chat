package pt.isec.Components;

import pt.isec.Server.Threads.UserHandlerThread;
import pt.isec.Server.Threads.UserNotificationThread;

public class UserThreads {
    private UserHandlerThread userHandler;
    private UserNotificationThread userNotification;

    public UserThreads(UserHandlerThread userHandler, UserNotificationThread userNotification) {
        this.userHandler = userHandler;
        this.userNotification = userNotification;
    }

    public void closeThreads() {
        if (userHandler != null)
            userHandler.closeThread();
        if (userNotification != null) {
            userNotification.closeThread();
            synchronized (userNotification) {
                userNotification.notify();
            }
        }
    }

    public void runNotifier(boolean status) {
        synchronized (userNotification) {
            if (status) {
                notifier().notify();
                return;
            }
            try {
                notifier().wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void runReceiver(boolean status) {
        synchronized (userHandler) {
            if (status) {
                receiver().notify();
                return;
            }
            try {
                receiver().wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendNotification(Notification notification) throws InterruptedException {
        notifier().setNotification(notification);
        runNotifier(true);
    }

    public UserHandlerThread receiver() {
        return userHandler;
    }

    public UserNotificationThread notifier() {
        return userNotification;
    }
}
