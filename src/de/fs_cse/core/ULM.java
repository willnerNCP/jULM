package de.fs_cse.core;

public class ULM {

    Instruction[] instructionSet;

    CPU cpu;

    boolean halted;
    boolean blocked;
    Instruction instruction;
    int exitCode;
    int opcode;
    int opfield;
    String errorMessage;

    public ULM(){
        reset();
        initInstructionSet();
    }

    public void reset(){
        cpu = new CPU();
        halted = blocked = false;
        instruction = null;
        errorMessage = null;
        exitCode = opcode = opfield = 0;
        loadProgram();
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

    public void run(){
        while(!halted){
            step();
        }
    }

    public void loadProgram(){
        return;
    }

    public void initInstructionSet(){
        instructionSet = new Instruction[256];
        instructionSet[0x30] = (ulm, opfield) -> ulm.cpu.alu.rAdd(opfield);
        instructionSet[0x38] = (ulm, opfield) -> ulm.cpu.alu.uAdd(opfield);
    }


}
