package de.fs_cse.core;

public interface ObserverULM {
    void nextInstruction(int opfield, String disassembly);
    void onHalt(int exitCode, String errorMessage);
    void onBlock();
    void reset();
}
