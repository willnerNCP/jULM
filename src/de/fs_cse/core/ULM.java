package de.fs_cse.core;

import de.fs_cse.CLI;

public class ULM {

    private Instruction[] instructionSet;

    CPU cpu;
    IODevice io;

    private boolean halted;
    private boolean blocked;
    private Instruction instruction;
    private int exitCode;
    private int opcode;
    private int opfield;
    private String errorMessage;

    public ULM(IODevice io){
        this.io = io;
        cpu = new CPU();
        initInstructionSet();
    }

    public void reset(){
        cpu.reset();
        io.reset();
        halted = blocked = false;
        instruction = null;
        errorMessage = null;
        exitCode = opcode = opfield = 0;
    }

    public void addObserverALU(ObserverALU observer){
        cpu.alu.addObserver(observer);
    }

    public void addObserverMemory(ObserverMemory observer){
        cpu.memory.addObserver(observer);
    }

    public void decodeInstruction(){
        opcode = cpu.ir >>> 24;
        instruction = instructionSet[opcode];
        if(instruction == null){
            halted = true;
            errorMessage = "illegal instruction: " + Integer.toHexString(opcode);
            return;
        }
        opfield = cpu.ir & 0xFFFFFF;
    }

    public void step(){
        cpu.loadInstruction();

        decodeInstruction();

        if(halted) return;

        instruction.execute(this, new OperationField(opfield));

        if(halted || blocked) return;

        cpu.incrementIP();
    }

    public int run(){
        while(!halted){
            step();
        }
        if(errorMessage != null)
            System.out.println(errorMessage);
        return exitCode;
    }

    public void loadProgram(){
        // Program: IO
        // int[] program = {0x30000000,
        //         0x60010000,
        //         0x39000100,
        //         0x42000003,
        //         0x61010000,
        //         0x41FFFFFC,
        //         0x09000000};

        // Program: Hello World
        int[] program = {0x56002001,
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
                0x210A0000};
        cpu.memory.loadProgram(program);
    }

    public void getc(OperationField opfield){
        cpu.alu.write(opfield.rX, (long)io.getc() & 0xFF);
    }

    public void rPutc(OperationField opfield){
        io.putc((char)cpu.alu.read(opfield.rX)); //cast by cutoff
    }

    public void uPutc(OperationField opfield){
        io.putc((char)opfield.uX); //cast by cutoff
    }

    public void rHalt(OperationField opfield){
        exitCode = (int)(cpu.alu.read(opfield.rX) & 0xFFL);
        halted = true;
    }

    public void uHalt(OperationField opfield){
        exitCode = (int)(opfield.uX & 0xFFL);
        halted = true;
    }

    public void initInstructionSet(){
        instructionSet = new Instruction[256];
        //HALT
        instructionSet[0x01] = (ulm, opfield) -> ulm.rHalt(opfield);
        instructionSet[0x09] = (ulm, opfield) -> ulm.uHalt(opfield);
        //ADD SUB
        instructionSet[0x30] = (ulm, opfield) -> ulm.cpu.alu.rAdd(opfield);
        instructionSet[0x31] = (ulm, opfield) -> ulm.cpu.alu.rSub(opfield);
        instructionSet[0x38] = (ulm, opfield) -> ulm.cpu.alu.uAdd(opfield);
        instructionSet[0x39] = (ulm, opfield) -> ulm.cpu.alu.uSub(opfield);
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
        instructionSet[0x18] = (ulm, opfield) -> ulm.cpu.sStore(opfield, 8);
        instructionSet[0x10] = (ulm, opfield) -> ulm.cpu.rStore(opfield, 8, 1);
        instructionSet[0x80] = (ulm, opfield) -> ulm.cpu.rStore(opfield, 8, 2);
        instructionSet[0xA0] = (ulm, opfield) -> ulm.cpu.rStore(opfield, 8, 4);
        instructionSet[0xC0] = (ulm, opfield) -> ulm.cpu.rStore(opfield, 8, 8);
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
        //FETCH QUAD
        instructionSet[0x28] = (ulm, opfield) -> ulm.cpu.sFetch(opfield, 8, false);
        instructionSet[0x20] = (ulm, opfield) -> ulm.cpu.rFetch(opfield, 8, 1, false);
        instructionSet[0x90] = (ulm, opfield) -> ulm.cpu.rFetch(opfield, 8, 2, false);
        instructionSet[0xB0] = (ulm, opfield) -> ulm.cpu.rFetch(opfield, 8, 4, false);
        instructionSet[0xD0] = (ulm, opfield) -> ulm.cpu.rFetch(opfield, 8, 8, false);
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


}
