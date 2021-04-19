package de.fs_cse.core;

public interface ObserverALU {
    void onRead(int regId, long value);
    void onWrite(int regId, long value);
    void reset();
}
