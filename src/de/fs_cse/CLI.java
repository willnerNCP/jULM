package de.fs_cse;

import de.fs_cse.core.IODevice;
import de.fs_cse.core.ObserverALU;
import de.fs_cse.core.ObserverMemory;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

public class CLI implements ObserverALU, ObserverMemory, IODevice {

    private String out = "";
    private char[] in;
    private int index;

    private Scanner scanner;

    public CLI(){
        scanner = new Scanner(System.in);
    }

    @Override
    public void putc(char c) {
        out += c;
        System.out.println("output: \n" + out);
    }

    @Override
    public char getc() {
        if(in == null || index == in.length){
            String next = "";
            while(next.length() == 0) {
                next = scanner.nextLine();
            }
            in = next.toCharArray();
            index = 0;
        }
        return in[index++];
    }

    @Override
    public void onRead(int regId, long value) {
        System.out.println("ALU: " + toHex(value) + " was read from register\t" + toHex(regId));
    }

    @Override
    public void onWrite(int regId, long value) {
        System.out.println("ALU: " + toHex(value) + " was written to register\t" + toHex(regId));
    }

    @Override
    public void onRead(long address, int numBytes, long value) {
        System.out.println("Bus: " + toHex(value) + " was read from address\t" + toHex(address));
    }

    @Override
    public void onWrite(long address, int numBytes, long value) {
        System.out.println("Bus: " + toHex(value) + " was written to address\t" + toHex(address));
    }

    private static String toHex(long i){
        return "0x" + String.format("%1$" + 16 + "s", Long.toHexString(i).toUpperCase()).replace(' ', '0');
    }

    private static String toHex(int regId){
        return "0x" + String.format("%1$" + 2 + "s", Integer.toHexString(regId).toUpperCase()).replace(' ', '0');
    }
}
