package de.fs_cse;

import de.fs_cse.core.ULM;

public class Main {

    public static void main(String[] args) {
        CLI cli = new CLI();
        ULM ulm = new ULM(cli);
        ulm.addObserverALU(cli);
        ulm.addObserverMemory(cli);
        ulm.loadProgram();
        ulm.run();
        ulm.reset();
        ulm.loadProgram();
        ulm.run();
    }
}
