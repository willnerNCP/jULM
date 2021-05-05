package de.fs_cse.core;

import java.util.ArrayList;

public class ULM {

    private Instruction[] instructionSet;
    private Disassembly[] disassemblies;

    private CPU cpu;
    private IODevice io;

    private boolean halted;
    private boolean blocked;
    private Instruction instruction;
    private int exitCode;
    private int opcode;
    private int opfield;
    private String errorMessage;

    private ArrayList<ObserverULM> observers;

    public ULM(IODevice io) {
        this.io = io;
        cpu = new CPU();
        observers = new ArrayList<>();
        initInstructionSet();
        initDisassemblies();
    }

    public void reset() {
        cpu.reset();
        io.reset();
        halted = blocked = false;
        instruction = null;
        errorMessage = null;
        exitCode = opcode = opfield = 0;
        for (ObserverULM observer : observers) observer.reset();
    }

    public void addObserver(ObserverULM observer) {
        observers.add(observer);
    }

    public void addObserverALU(ObserverALU observer) {
        cpu.alu.addObserver(observer);
    }

    public void addObserverMemory(ObserverMemory observer) {
        cpu.memory.addObserver(observer);
    }

    private void decodeInstruction() {
        opcode = cpu.ir >>> 24;
        instruction = instructionSet[opcode];
        if (instruction == null) {
            halted = true;
            errorMessage = "illegal instruction: " + Integer.toHexString(opcode);
            return;
        }
        opfield = cpu.ir & 0xFFFFFF;
    }

    public boolean step() {
        cpu.loadInstruction();

        decodeInstruction();

        if (halted) {
            for (ObserverULM observer : observers) observer.onHalt(exitCode, errorMessage);
            return false;
        }

        instruction.execute(this, new OperationField(opfield));

        if (halted) {
            for (ObserverULM observer : observers) observer.onHalt(exitCode, errorMessage);
            return false;
        } else if (blocked) {
            for (ObserverULM observer : observers) observer.onBlock();
            return false;
        }

        cpu.incrementIP();

        analyseNextInstruction();

        return true;
    }

    public int run() {
        while (!halted) {
            step();
        }
        if (errorMessage != null)
            System.out.println(errorMessage);
        return exitCode;
    }

    public void loadProgram(int[] program) {
        cpu.memory.loadProgram(program);
        analyseNextInstruction();
    }

    private void analyseNextInstruction() {
        int nextOpfield = (int) cpu.memory.peek(cpu.ip, 4);
        int nextOpcode = nextOpfield >>> 24;
        String disassembly;
        if (instructionSet[nextOpcode] != null) {
            if (disassemblies[nextOpcode] != null) {
                disassembly = disassemblies[nextOpcode].disassembly(new OperationField(nextOpfield & 0xFFFFFF));
            } else {
                disassembly = "not implemented";
            }
        } else {
            disassembly = "illegal instruction";
        }
        for (ObserverULM observer : observers) observer.nextInstruction(nextOpfield, disassembly);
    }

    private void getc(OperationField opfield) {
        if (io.hasNextChar()) {
            blocked = false;
            cpu.alu.write(opfield.rX, (long) io.getc() & 0xFF);
        } else
            blocked = true;
    }

    private void rPutc(OperationField opfield) {
        io.putc((char) cpu.alu.read(opfield.rX)); //cast by cutoff
    }

    private void uPutc(OperationField opfield) {
        io.putc((char) opfield.uX); //cast by cutoff
    }

    private void rHalt(OperationField opfield) {
        exitCode = (int) (cpu.alu.read(opfield.rX) & 0xFFL);
        halted = true;
    }

    private void uHalt(OperationField opfield) {
        exitCode = (int) (opfield.uX & 0xFFL);
        halted = true;
    }

    private void initInstructionSet() {
        instructionSet = new Instruction[256];
        //HALT
        instructionSet[0x01] = (ulm, opfield) -> ulm.rHalt(opfield);
        instructionSet[0x09] = (ulm, opfield) -> ulm.uHalt(opfield);
        //ADD SUB MUL DIV
        instructionSet[0x30] = (ulm, opfield) -> ulm.cpu.alu.rAdd(opfield);
        instructionSet[0x31] = (ulm, opfield) -> ulm.cpu.alu.rSub(opfield);
        instructionSet[0x33] = (ulm, opfield) -> ulm.cpu.alu.rDivUnsigned(opfield);
        instructionSet[0x34] = (ulm, opfield) -> ulm.cpu.alu.rMul(opfield);
        instructionSet[0x35] = (ulm, opfield) -> ulm.cpu.alu.rDivSigned(opfield);
        instructionSet[0x38] = (ulm, opfield) -> ulm.cpu.alu.uAdd(opfield);
        instructionSet[0x39] = (ulm, opfield) -> ulm.cpu.alu.uSub(opfield);
        instructionSet[0x3B] = (ulm, opfield) -> ulm.cpu.alu.uDivUnsigned(opfield);
        instructionSet[0x3C] = (ulm, opfield) -> ulm.cpu.alu.sMul(opfield);
        instructionSet[0x3D] = (ulm, opfield) -> ulm.cpu.alu.sDivSigned(opfield);
        //JMP
        instructionSet[0x40] = (ulm, opfield) -> ulm.cpu.absJmp(opfield);
        instructionSet[0x41] = (ulm, opfield) -> ulm.cpu.relJmp(opfield);
        instructionSet[0x42] = (ulm, opfield) -> ulm.cpu.jz(opfield);
        instructionSet[0x43] = (ulm, opfield) -> ulm.cpu.jnz(opfield);
        //JMP - signed
        instructionSet[0x44] = (ulm, opfield) -> ulm.cpu.jl(opfield);
        instructionSet[0x45] = (ulm, opfield) -> ulm.cpu.jge(opfield);
        instructionSet[0x46] = (ulm, opfield) -> ulm.cpu.jle(opfield);
        instructionSet[0x47] = (ulm, opfield) -> ulm.cpu.jg(opfield);
        //JMP - unsigned
        instructionSet[0x48] = (ulm, opfield) -> ulm.cpu.jb(opfield);
        instructionSet[0x49] = (ulm, opfield) -> ulm.cpu.jae(opfield);
        instructionSet[0x4A] = (ulm, opfield) -> ulm.cpu.jbe(opfield);
        instructionSet[0x4B] = (ulm, opfield) -> ulm.cpu.ja(opfield);
        //BITWISE OPERATORS
        instructionSet[0x50] = (ulm, opfield) -> ulm.cpu.alu.or(opfield);
        instructionSet[0x51] = (ulm, opfield) -> ulm.cpu.alu.and(opfield);
        instructionSet[0x5E] = (ulm, opfield) -> ulm.cpu.alu.not(opfield);
        //SHIFT and LOAD
        instructionSet[0x52] = (ulm, opfield) -> ulm.cpu.alu.rShiftLeft(opfield);
        instructionSet[0x53] = (ulm, opfield) -> ulm.cpu.alu.rLogicalShiftRight(opfield);
        instructionSet[0x54] = (ulm, opfield) -> ulm.cpu.alu.rArithmeticShiftRight(opfield);
        instructionSet[0x56] = (ulm, opfield) -> ulm.cpu.alu.uLoad(opfield);
        instructionSet[0x57] = (ulm, opfield) -> ulm.cpu.alu.sLoad(opfield);
        instructionSet[0x5A] = (ulm, opfield) -> ulm.cpu.alu.uShiftLeft(opfield);
        instructionSet[0x5B] = (ulm, opfield) -> ulm.cpu.alu.uLogicalShiftRight(opfield);
        instructionSet[0x5C] = (ulm, opfield) -> ulm.cpu.alu.uArithmeticShiftRight(opfield);
        instructionSet[0x5D] = (ulm, opfield) -> ulm.cpu.alu.uShiftLeftLoad(opfield);
        //IO
        instructionSet[0x60] = (ulm, opfield) -> ulm.getc(opfield);
        instructionSet[0x61] = (ulm, opfield) -> ulm.rPutc(opfield);
        instructionSet[0x69] = (ulm, opfield) -> ulm.uPutc(opfield);
        //NOP
        instructionSet[0xFF] = (ulm, opfield) -> {};
        //STORE BYTE
        instructionSet[0x2B] = (ulm, opfield) -> ulm.cpu.sStore(opfield, 1);
        instructionSet[0x23] = (ulm, opfield) -> ulm.cpu.rStore(opfield, 1, 1);
        instructionSet[0x93] = (ulm, opfield) -> ulm.cpu.rStore(opfield, 1, 2);
        instructionSet[0xB3] = (ulm, opfield) -> ulm.cpu.rStore(opfield, 1, 4);
        instructionSet[0xD3] = (ulm, opfield) -> ulm.cpu.rStore(opfield, 1, 8);
        //STORE WORD
        instructionSet[0x2A] = (ulm, opfield) -> ulm.cpu.sStore(opfield, 2);
        instructionSet[0x22] = (ulm, opfield) -> ulm.cpu.rStore(opfield, 2, 1);
        instructionSet[0x92] = (ulm, opfield) -> ulm.cpu.rStore(opfield, 2, 2);
        instructionSet[0xB2] = (ulm, opfield) -> ulm.cpu.rStore(opfield, 2, 4);
        instructionSet[0xD2] = (ulm, opfield) -> ulm.cpu.rStore(opfield, 2, 8);
        //STORE LONG
        instructionSet[0x29] = (ulm, opfield) -> ulm.cpu.sStore(opfield, 4);
        instructionSet[0x21] = (ulm, opfield) -> ulm.cpu.rStore(opfield, 4, 1);
        instructionSet[0x91] = (ulm, opfield) -> ulm.cpu.rStore(opfield, 4, 2);
        instructionSet[0xB1] = (ulm, opfield) -> ulm.cpu.rStore(opfield, 4, 4);
        instructionSet[0xD1] = (ulm, opfield) -> ulm.cpu.rStore(opfield, 4, 8);
        //STORE QUAD
        instructionSet[0x28] = (ulm, opfield) -> ulm.cpu.sStore(opfield, 8);
        instructionSet[0x20] = (ulm, opfield) -> ulm.cpu.rStore(opfield, 8, 1);
        instructionSet[0x90] = (ulm, opfield) -> ulm.cpu.rStore(opfield, 8, 2);
        instructionSet[0xB0] = (ulm, opfield) -> ulm.cpu.rStore(opfield, 8, 4);
        instructionSet[0xD0] = (ulm, opfield) -> ulm.cpu.rStore(opfield, 8, 8);
        //FETCH UNSIGNED BYTE
        instructionSet[0x1B] = (ulm, opfield) -> ulm.cpu.sFetch(opfield, 1, false);
        instructionSet[0x13] = (ulm, opfield) -> ulm.cpu.rFetch(opfield, 1, 1, false);
        instructionSet[0x83] = (ulm, opfield) -> ulm.cpu.rFetch(opfield, 1, 2, false);
        instructionSet[0xA3] = (ulm, opfield) -> ulm.cpu.rFetch(opfield, 1, 4, false);
        instructionSet[0xC3] = (ulm, opfield) -> ulm.cpu.rFetch(opfield, 1, 8, false);
        //FETCH UNSIGNED WORD
        instructionSet[0x1A] = (ulm, opfield) -> ulm.cpu.sFetch(opfield, 2, false);
        instructionSet[0x12] = (ulm, opfield) -> ulm.cpu.rFetch(opfield, 2, 1, false);
        instructionSet[0x82] = (ulm, opfield) -> ulm.cpu.rFetch(opfield, 2, 2, false);
        instructionSet[0xA2] = (ulm, opfield) -> ulm.cpu.rFetch(opfield, 2, 4, false);
        instructionSet[0xC2] = (ulm, opfield) -> ulm.cpu.rFetch(opfield, 2, 8, false);
        //FETCH UNSIGNED LONG
        instructionSet[0x19] = (ulm, opfield) -> ulm.cpu.sFetch(opfield, 4, false);
        instructionSet[0x11] = (ulm, opfield) -> ulm.cpu.rFetch(opfield, 4, 1, false);
        instructionSet[0x81] = (ulm, opfield) -> ulm.cpu.rFetch(opfield, 4, 2, false);
        instructionSet[0xA1] = (ulm, opfield) -> ulm.cpu.rFetch(opfield, 4, 4, false);
        instructionSet[0xC1] = (ulm, opfield) -> ulm.cpu.rFetch(opfield, 4, 8, false);
        //FETCH (UNSIGNED) QUAD
        instructionSet[0x18] = (ulm, opfield) -> ulm.cpu.sFetch(opfield, 8, false);
        instructionSet[0x10] = (ulm, opfield) -> ulm.cpu.rFetch(opfield, 8, 1, false);
        instructionSet[0x80] = (ulm, opfield) -> ulm.cpu.rFetch(opfield, 8, 2, false);
        instructionSet[0xA0] = (ulm, opfield) -> ulm.cpu.rFetch(opfield, 8, 4, false);
        instructionSet[0xC0] = (ulm, opfield) -> ulm.cpu.rFetch(opfield, 8, 8, false);
        //FETCH SIGNED BYTE
        instructionSet[0x1F] = (ulm, opfield) -> ulm.cpu.sFetch(opfield, 1, true);
        instructionSet[0x17] = (ulm, opfield) -> ulm.cpu.rFetch(opfield, 1, 1, true);
        instructionSet[0x87] = (ulm, opfield) -> ulm.cpu.rFetch(opfield, 1, 2, true);
        instructionSet[0xA7] = (ulm, opfield) -> ulm.cpu.rFetch(opfield, 1, 4, true);
        instructionSet[0xC7] = (ulm, opfield) -> ulm.cpu.rFetch(opfield, 1, 8, true);
        //FETCH SIGNED WORD
        instructionSet[0x1E] = (ulm, opfield) -> ulm.cpu.sFetch(opfield, 2, true);
        instructionSet[0x16] = (ulm, opfield) -> ulm.cpu.rFetch(opfield, 2, 1, true);
        instructionSet[0x86] = (ulm, opfield) -> ulm.cpu.rFetch(opfield, 2, 2, true);
        instructionSet[0xA6] = (ulm, opfield) -> ulm.cpu.rFetch(opfield, 2, 4, true);
        instructionSet[0xC6] = (ulm, opfield) -> ulm.cpu.rFetch(opfield, 2, 8, true);
        //FETCH SIGNED LONG
        instructionSet[0x1D] = (ulm, opfield) -> ulm.cpu.sFetch(opfield, 4, true);
        instructionSet[0x15] = (ulm, opfield) -> ulm.cpu.rFetch(opfield, 4, 1, true);
        instructionSet[0x85] = (ulm, opfield) -> ulm.cpu.rFetch(opfield, 4, 2, true);
        instructionSet[0xA5] = (ulm, opfield) -> ulm.cpu.rFetch(opfield, 4, 4, true);
        instructionSet[0xC5] = (ulm, opfield) -> ulm.cpu.rFetch(opfield, 4, 8, true);
    }

    private void initDisassemblies() {
        disassemblies = new Disassembly[256];
        //HALT
        disassemblies[0x01] = (opfield) -> format_R("halt", opfield.rX);
        disassemblies[0x09] = (opfield) -> format_US("halt", opfield.uX);
        //ADD SUB MUL DIV
        disassemblies[0x30] = (opfield) -> format_RRR("addq", opfield.rX, opfield.rY, opfield.rZ);
        disassemblies[0x31] = (opfield) -> format_RRR("subq", opfield.rX, opfield.rY, opfield.rZ);
        disassemblies[0x33] = (opfield) -> format_RRR( "divq", opfield.rX, opfield.rY, opfield.rZ);
        disassemblies[0x34] = (opfield) -> format_RRR("imulq", opfield.rX, opfield.rY, opfield.rZ);
        disassemblies[0x35] = (opfield) -> format_RRR("idivq", opfield.rX, opfield.rY, opfield.rZ);
        disassemblies[0x38] = (opfield) -> format_US_RR("addq", opfield.uX, opfield.rY, opfield.rZ);
        disassemblies[0x39] = (opfield) -> format_US_RR("subq", opfield.uX, opfield.rY, opfield.rZ);
        disassemblies[0x3B] = (opfield) -> format_US_RR("divq", opfield.uX, opfield.rY, opfield.rZ);
        disassemblies[0x3C] = (opfield) -> format_US_RR("imulq", opfield.sX, opfield.rY, opfield.rZ);
        disassemblies[0x3D] = (opfield) -> format_US_RR("idivq", opfield.sX, opfield.rY, opfield.rZ);
        //JMP
        disassemblies[0x40] = (opfield) -> format_RR("jmp", opfield.rX, opfield.rY);
        disassemblies[0x41] = (opfield) -> format_US("jmp", opfield.sXYZ);
        disassemblies[0x42] = (opfield) -> format_US("jz", opfield.sXYZ);
        disassemblies[0x43] = (opfield) -> format_US("jnz", opfield.sXYZ);
        //JMP - signed                                            format_US(
        disassemblies[0x44] = (opfield) -> format_US("jl", opfield.sXYZ);
        disassemblies[0x45] = (opfield) -> format_US("jge", opfield.sXYZ);
        disassemblies[0x46] = (opfield) -> format_US("jle", opfield.sXYZ);
        disassemblies[0x47] = (opfield) -> format_US("jg", opfield.sXYZ);
        //JMP - unsigned                                          format_US(
        disassemblies[0x48] = (opfield) -> format_US("jb", opfield.sXYZ);
        disassemblies[0x49] = (opfield) -> format_US("jae", opfield.sXYZ);
        disassemblies[0x4A] = (opfield) -> format_US("jbe", opfield.sXYZ);
        disassemblies[0x4B] = (opfield) -> format_US("ja", opfield.sXYZ);
        //BITWISE OPERATORS
        disassemblies[0x50] = (opfield) -> format_RRR("orq", opfield.rX, opfield.rY, opfield.rZ);
        disassemblies[0x51] = (opfield) -> format_RRR("andq", opfield.rX, opfield.rY, opfield.rZ);
        disassemblies[0x5E] = (opfield) -> format_RR("notq", opfield.rX, opfield.rY);
        //SHIFT and LOAD
        disassemblies[0x52] = (opfield) -> format_RRR("shlq", opfield.rX, opfield.rY, opfield.rZ);
        disassemblies[0x53] = (opfield) -> format_RRR("shrq", opfield.rX, opfield.rY, opfield.rZ);
        disassemblies[0x54] = (opfield) -> format_RRR("sarq", opfield.rX, opfield.rY, opfield.rZ);
        disassemblies[0x56] = (opfield) -> format_US_16R("ldzwq", opfield.uXY, opfield.rZ);
        disassemblies[0x57] = (opfield) -> format_US_16R("ldswq", opfield.sXY, opfield.rZ);
        disassemblies[0x5A] = (opfield) -> format_US_RR("shlq", opfield.uX, opfield.rY, opfield.rZ);
        disassemblies[0x5B] = (opfield) -> format_US_RR("shrq", opfield.uX, opfield.rY, opfield.rZ);
        disassemblies[0x5C] = (opfield) -> format_US_RR("sarq", opfield.uX, opfield.rY, opfield.rZ);
        disassemblies[0x5D] = (opfield) -> format_US_16R( "shldwq", opfield.uXY, opfield.rZ);
        //IO
        disassemblies[0x60] = (opfield) -> format_US("getc", opfield.rX);
        disassemblies[0x61] = (opfield) -> format_US("putc", opfield.rX);
        disassemblies[0x69] = (opfield) -> String.format("%s '%c'", "putc", (byte) opfield.uX);
        //NOP
        disassemblies[0xFF] = (opfield) -> "nop";
        //STORE BYTE
        disassemblies[0x2B] = (opfield) -> format_R_MSR("movb", opfield.rX, opfield.sY, opfield.rZ);
        disassemblies[0x23] = (opfield) -> format_R_MRR("movb", opfield.rX, opfield.rY, opfield.rZ, 1);
        disassemblies[0x93] = (opfield) -> format_R_MRR("movb", opfield.rX, opfield.rY, opfield.rZ, 2);
        disassemblies[0xB3] = (opfield) -> format_R_MRR("movb", opfield.rX, opfield.rY, opfield.rZ, 4);
        disassemblies[0xD3] = (opfield) -> format_R_MRR("movb", opfield.rX, opfield.rY, opfield.rZ, 8);
        //STORE WORD
        disassemblies[0x2A] = (opfield) -> format_R_MSR("movw", opfield.rX, opfield.sY, opfield.rZ);
        disassemblies[0x22] = (opfield) -> format_R_MRR("movw", opfield.rX, opfield.rY, opfield.rZ, 1);
        disassemblies[0x92] = (opfield) -> format_R_MRR("movw", opfield.rX, opfield.rY, opfield.rZ, 2);
        disassemblies[0xB2] = (opfield) -> format_R_MRR("movw", opfield.rX, opfield.rY, opfield.rZ, 4);
        disassemblies[0xD2] = (opfield) -> format_R_MRR("movw", opfield.rX, opfield.rY, opfield.rZ, 8);
        //STORE LONG
        disassemblies[0x29] = (opfield) -> format_R_MSR("movl", opfield.rX, opfield.sY, opfield.rZ);
        disassemblies[0x21] = (opfield) -> format_R_MRR("movl", opfield.rX, opfield.rY, opfield.rZ, 1);
        disassemblies[0x91] = (opfield) -> format_R_MRR("movl", opfield.rX, opfield.rY, opfield.rZ, 2);
        disassemblies[0xB1] = (opfield) -> format_R_MRR("movl", opfield.rX, opfield.rY, opfield.rZ, 4);
        disassemblies[0xD1] = (opfield) -> format_R_MRR("movl", opfield.rX, opfield.rY, opfield.rZ, 8);
        //STORE QUAD
        disassemblies[0x28] = (opfield) -> format_R_MSR("movq", opfield.rX, opfield.sY, opfield.rZ);
        disassemblies[0x20] = (opfield) -> format_R_MRR("movq", opfield.rX, opfield.rY, opfield.rZ, 1);
        disassemblies[0x90] = (opfield) -> format_R_MRR("movq", opfield.rX, opfield.rY, opfield.rZ, 2);
        disassemblies[0xB0] = (opfield) -> format_R_MRR("movq", opfield.rX, opfield.rY, opfield.rZ, 4);
        disassemblies[0xD0] = (opfield) -> format_R_MRR("movq", opfield.rX, opfield.rY, opfield.rZ, 8);
        //FETCH UNSIGNED BYTE
        disassemblies[0x1B] = (opfield) -> format_MSR_R("movzbq", opfield.sX, opfield.rY, opfield.rZ);
        disassemblies[0x13] = (opfield) -> format_MRR_R("movzbq", opfield.rX, opfield.rY, opfield.rZ, 1);
        disassemblies[0x83] = (opfield) -> format_MRR_R("movzbq", opfield.rX, opfield.rY, opfield.rZ, 2);
        disassemblies[0xA3] = (opfield) -> format_MRR_R("movzbq", opfield.rX, opfield.rY, opfield.rZ, 4);
        disassemblies[0xC3] = (opfield) -> format_MRR_R("movzbq", opfield.rX, opfield.rY, opfield.rZ, 8);
        //FETCH UNSIGNED WORD
        disassemblies[0x1A] = (opfield) -> format_MSR_R("movzwq", opfield.sX, opfield.rY, opfield.rZ);
        disassemblies[0x12] = (opfield) -> format_MRR_R("movzwq", opfield.rX, opfield.rY, opfield.rZ, 1);
        disassemblies[0x82] = (opfield) -> format_MRR_R("movzwq", opfield.rX, opfield.rY, opfield.rZ, 2);
        disassemblies[0xA2] = (opfield) -> format_MRR_R("movzwq", opfield.rX, opfield.rY, opfield.rZ, 4);
        disassemblies[0xC2] = (opfield) -> format_MRR_R("movzwq", opfield.rX, opfield.rY, opfield.rZ, 8);
        //FETCH UNSIGNED LONG
        disassemblies[0x19] = (opfield) -> format_MSR_R("movzlq", opfield.sX, opfield.rY, opfield.rZ);
        disassemblies[0x11] = (opfield) -> format_MRR_R("movzlq", opfield.rX, opfield.rY, opfield.rZ, 1);
        disassemblies[0x81] = (opfield) -> format_MRR_R("movzlq", opfield.rX, opfield.rY, opfield.rZ, 2);
        disassemblies[0xA1] = (opfield) -> format_MRR_R("movzlq", opfield.rX, opfield.rY, opfield.rZ, 4);
        disassemblies[0xC1] = (opfield) -> format_MRR_R("movzlq", opfield.rX, opfield.rY, opfield.rZ, 8);
        //FETCH (UNSIGNED) QUAD
        disassemblies[0x18] = (opfield) -> format_MSR_R("movq", opfield.sX, opfield.rY, opfield.rZ);
        disassemblies[0x10] = (opfield) -> format_MRR_R("movq", opfield.rX, opfield.rY, opfield.rZ, 1);
        disassemblies[0x80] = (opfield) -> format_MRR_R("movq", opfield.rX, opfield.rY, opfield.rZ, 2);
        disassemblies[0xA0] = (opfield) -> format_MRR_R("movq", opfield.rX, opfield.rY, opfield.rZ, 4);
        disassemblies[0xC0] = (opfield) -> format_MRR_R("movq", opfield.rX, opfield.rY, opfield.rZ, 8);
        //FETCH SIGNED BYTE
        disassemblies[0x1F] = (opfield) -> format_MSR_R("movsbq", opfield.sX, opfield.rY, opfield.rZ);
        disassemblies[0x17] = (opfield) -> format_MRR_R("movsbq", opfield.rX, opfield.rY, opfield.rZ, 1);
        disassemblies[0x87] = (opfield) -> format_MRR_R("movsbq", opfield.rX, opfield.rY, opfield.rZ, 2);
        disassemblies[0xA7] = (opfield) -> format_MRR_R("movsbq", opfield.rX, opfield.rY, opfield.rZ, 4);
        disassemblies[0xC7] = (opfield) -> format_MRR_R("movsbq", opfield.rX, opfield.rY, opfield.rZ, 8);
        //FETCH SIGNED WORD
        disassemblies[0x1E] = (opfield) -> format_MSR_R("movswq", opfield.sX, opfield.rY, opfield.rZ);
        disassemblies[0x16] = (opfield) -> format_MRR_R("movswq", opfield.rX, opfield.rY, opfield.rZ, 1);
        disassemblies[0x86] = (opfield) -> format_MRR_R("movswq", opfield.rX, opfield.rY, opfield.rZ, 2);
        disassemblies[0xA6] = (opfield) -> format_MRR_R("movswq", opfield.rX, opfield.rY, opfield.rZ, 4);
        disassemblies[0xC6] = (opfield) -> format_MRR_R("movswq", opfield.rX, opfield.rY, opfield.rZ, 8);
        //FETCH SIGNED LONG
        disassemblies[0x1D] = (opfield) -> format_MSR_R("movslq", opfield.sX, opfield.rY, opfield.rZ);
        disassemblies[0x15] = (opfield) -> format_MRR_R("movslq", opfield.rX, opfield.rY, opfield.rZ, 1);
        disassemblies[0x85] = (opfield) -> format_MRR_R("movslq", opfield.rX, opfield.rY, opfield.rZ, 2);
        disassemblies[0xA5] = (opfield) -> format_MRR_R("movslq", opfield.rX, opfield.rY, opfield.rZ, 4);
        disassemblies[0xC5] = (opfield) -> format_MRR_R("movslq", opfield.rX, opfield.rY, opfield.rZ, 8);
    }

    private static String format_R(String op, int x){
        return String.format("%s %%%x", op, x);
    }

    private static String format_US(String op, long x){
        return String.format("%s %d", op, x);
    }

    private static String format_RR(String op, int x, int y){
        return String.format("%s %%%x %%%x", op, x, y);
    }

    private static String format_RRR(String op, int x, int y, int z){
        return String.format("%s %%%x %%%x %%%x", op, x, y, z);
    }

    private static String format_US_RR(String op, long x, int y, int z){
        return String.format("%s %d %%%x %%%x", op, x, y, z);
    }

    private static String format_US_16R(String op, long xy, int z){
        return String.format("%s %d %%%x", op, xy, z);
    }

    private static String format_MRR_R(String op, int x, int y, int z, int mul){
        return String.format("%s (%%%x %%%x %d) %%%x", op, x, y, z, mul);
    }

    private static String format_R_MRR(String op, int x, int y, int z, int mul){
        return String.format("%s %%%x (%%%x %%%x %d)", op, x, y, z, mul);
    }

    private static String format_MSR_R(String op, long x, int y, int z){
        return String.format("%s %d(%%%x) %%%x", op, x, y, z);
    }

    private static String format_R_MSR(String op, int x, long y, int z){
        return String.format("%s %%%x %d(%%%x)", op, x, y, z);
    }


}