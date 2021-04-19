package de.fs_cse.core;

import java.util.ArrayList;

public class ALU {

    public static int NUM_REGS = 256;

    public long[] registers;

    public boolean zf;
    public boolean cf;
    public boolean of;
    public boolean sf;

    public ArrayList<ObserverALU> observers;

    public ALU(){
        registers = new long[NUM_REGS];
        observers = new ArrayList<>();
    }

    public void addObserver(ObserverALU observer){
        observers.add(observer);
    }

    public long read(int regId){
        long value = registers[regId];
        for(ObserverALU observer : observers) observer.onRead(regId, value);
        return value;
    }

    public void write(int regId, long value){
        if(regId == 0x00) value = 0;
        for(ObserverALU observer : observers) observer.onWrite(regId, value);
        registers[regId] = value;
    }

    private void load(int regId, long value){
        write(regId, value);
        zf = value == 0;
    }

    public void uLoad(OperationField opfield){
        load(opfield.rZ, opfield.uXY);
    }

    public void sLoad(OperationField opfield){
        load(opfield.rZ, opfield.sXY);
    }

    public void uShiftLeftLoad(OperationField opfield){
        long value = (read(opfield.rZ) << 16) + opfield.uXY; //operator precedence
        load(opfield.rZ, value);
    }

    private void arithmeticShiftLeft(long shiftBy, long value, int regId){
        long z = value << shiftBy;
        write(regId, z);

        if(Long.compareUnsigned(shiftBy, 64) < 0){
            cf = (value >>> (64-shiftBy) & 0x01) == 1; //unsigned shift
        }
    }

    public void rShiftLeft(OperationField opfield){
        arithmeticShiftLeft(read(opfield.rX), read(opfield.rY), opfield.rZ);
    }

    public void uShiftLeft(OperationField opfield){
        arithmeticShiftLeft(opfield.uX, read(opfield.rY), opfield.rZ);
    }

    private void arithmeticShiftRight(long shiftBy, long value, int regId){
        long z = value >> shiftBy; //arithmetic shift = signed shift
        write(regId, z);
    }

    public void rArithmeticShiftRight(OperationField opfield){
        arithmeticShiftRight(read(opfield.rX), read(opfield.rY), opfield.rZ);
    }

    public void uArithmeticShiftRight(OperationField opfield){
        arithmeticShiftRight(opfield.uX, read(opfield.rY), opfield.rZ);
    }

    private void logicalShiftRight(long shiftBy, long value, int regId){
        long z = value >>> shiftBy; //logical shift = unsigned shift
        write(regId, z);
    }

    public void rLogicalShiftRight(OperationField opfield){
        logicalShiftRight(read(opfield.rX), read(opfield.rY), opfield.rZ);
    }

    public void uLogicalShiftRight(OperationField opfield){
        logicalShiftRight(opfield.uX, read(opfield.rY), opfield.rZ);
    }

    private void add(long x, long y, int regId){
        long z = x + y;
        write(regId, z);

        zf = z == 0;
        cf = Long.compareUnsigned(z, x) < 0 || Long.compareUnsigned(z, y) < 0;
        of = (x < 0 && y < 0 && z > 0) || (x > 0 && y > 0 && z < 0);
        sf = z < 0;
    }

    public void rAdd(OperationField opfield){
        add(read(opfield.rX), read(opfield.rY), opfield.rZ);
    }

    public void uAdd(OperationField opfield){
        add(opfield.uX, read(opfield.rY), opfield.rZ);
    }

    public void sub(long x, long y, int regId){
        long z = y - x;
        write(regId, z);

        zf = z == 0;
        cf = Long.compareUnsigned(y, z) < 0;
        of = (x >= 0 && y < 0 && z >= 0) || (x < 0 && y >= 0 && z < 0);
        sf = z < 0;
    }

    public void rSub(OperationField opfield){
        sub(read(opfield.rX), read(opfield.rY), opfield.rZ);
    }

    public void uSub(OperationField opfield){
        sub(opfield.uX, read(opfield.rY), opfield.rZ);
    }

    public void and(OperationField opfield){
        long z = read(opfield.rX) & read(opfield.rY);
        write(opfield.rZ, z);

        zf = z == 0;
    }

    public void or(OperationField opfield){
        long z = read(opfield.rX) | read(opfield.rY);
        write(opfield.rZ, z);

        zf = z == 0;
    }

    public void not(OperationField opfield){
        long y = ~read(opfield.rX);
        write(opfield.rY, y);

        zf = y == 0;
    }

}
