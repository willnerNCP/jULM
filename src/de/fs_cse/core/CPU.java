package de.fs_cse.core;

public class CPU {

    public VirtualMemory memory;
    public ALU alu;

    public int ir;
    public long ip;
    public boolean jumped;

    public CPU(){
        memory = new VirtualMemory();
        alu = new ALU();
    }

    public void reset(){
        ir = 0;
        ip = 0;
        jumped = false;
        alu.reset();
        memory.reset();
    }

    public static long signExtend(long bitPattern, int size){
        long mask = (-1) << (size*8 - 1);
        if((mask & bitPattern) != 0){
            return mask | bitPattern;
        }
        return bitPattern;
    }

    public void loadInstruction(){
        ir = (int) memory.read(ip, 4);
    }

    public void incrementIP(){
        if(jumped) jumped = false;
        else ip += 4;
    }

    //BUS SECTION

    public void rFetch(OperationField opfield, int size, int scale, boolean signed){
        long address = alu.read(opfield.rX) + (alu.read(opfield.rY) << (8*scale));
        long z = memory.read(address, size);
        if(signed) z = signExtend(z, size);
        alu.write(opfield.rZ, z);
    }

    public void sFetch(OperationField opfield, int size, boolean signed){
        long address = opfield.sX + alu.read(opfield.rY);
        long z = memory.read(address, size);
        if(signed) z = signExtend(z, size);
        alu.write(opfield.rZ, z);
    }

    public void rStore(OperationField opfield, int size, int scale){
        long address = alu.read(opfield.rY) + (alu.read(opfield.rZ) << (8*scale));
        memory.write(address, size, alu.read(opfield.rX));
    }

    public void sStore(OperationField opfield, int size){
        long address = opfield.sY + alu.read(opfield.rZ);
        memory.write(address, size, alu.read(opfield.rX));
    }

    //JUMP SECTION

    public void absJmp(OperationField opfield){
        alu.write(opfield.rY, ip + 4);
        ip = alu.read(opfield.rX);
        jumped = true;
    }

    public void relJmp(OperationField opfield){
        ip += opfield.sXYZ << 2;
        jumped = true;
    }

    public void ja(OperationField opfield){
        if(!alu.cf && !alu.zf) relJmp(opfield);
    }

    public void jbe(OperationField opfield){
        if(alu.cf || alu.zf) relJmp(opfield);
    }

    public void jae(OperationField opfield){
        if(!alu.cf) relJmp(opfield);
    }

    public void jb(OperationField opfield){
        if(alu.cf) relJmp(opfield);
    }

    public void jg(OperationField opfield){
        if(alu.zf && (alu.sf == alu.of)) relJmp(opfield);
    }

    public void jle(OperationField opfield){
        if(alu.zf || (alu.sf != alu.of)) relJmp(opfield);
    }

    public void jge(OperationField opfield){
        if(alu.sf == alu.of) relJmp(opfield);
    }

    public void jl(OperationField opfield){
        if(alu.sf != alu.of) relJmp(opfield);
    }

    public void jz(OperationField opfield){
        if(alu.zf) relJmp(opfield);
    }

    public void jnz(OperationField opfield){
        if(!alu.zf) relJmp(opfield);
    }
}
