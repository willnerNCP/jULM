package de.fs_cse.core;

public interface ObserverMemory {
    void onRead(long address, int numBytes, long value);
    void onWrite(long address, int numBytes, long value);
    void onLoadProgram(int [] program);
    void reset();
}
