package de.fs_cse;

import de.fs_cse.core.ULM;

public class Main {

    public static void main(String[] args) {
        CLI cli = new CLI();
        ULM ulm = new ULM(cli);
        ulm.addObserverALU(cli);
        ulm.addObserverMemory(cli);
        ulm.loadProgram(getProgramHelloWorld());
        ulm.run();
        ulm.reset();
        ulm.loadProgram(getProgramHelloWorld());
        ulm.run();
    }

    public static int[] getProgramIO(){
        return new int[]{
                0x30000000,
                0x60010000,
                0x39000100,
                0x42000003,
                0x61010000,
                0x41FFFFFC,
                0x09000000};
    }

    public static int[] getProgramHelloWorld(){
        return new int[]{
                0x56002001,
                0x1B000102,
                0x39000200,
                0x42000004,
                0x61020000,
                0x38010101,
                0x41FFFFFB,
                0x09000000,
                0x68656C6C,
                0x6F2C2077,
                0x6F726C64,
                0x210A0000,
        };
    }
}
