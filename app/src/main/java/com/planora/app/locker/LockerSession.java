package com.planora.app.locker;

/** Remembers that the App Locker password was entered, until the user leaves the app. */
public final class LockerSession {
    private LockerSession() {}
    public static volatile boolean authed = false;
}
