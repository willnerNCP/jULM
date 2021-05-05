package de.fs_cse.core;

public interface IODevice {
    void putc(char c);
    char getc();
    boolean hasNextChar();
    void reset();
}
