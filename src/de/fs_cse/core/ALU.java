package de.fs_cse.core;

import java.math.BigInteger;
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

    public void reset(){
        registers = new long[NUM_REGS];
        zf = cf = of = sf = false;
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

    // OPERATION SECTION
    // prefix r denotes operation with register value, prefixes u (s) denote operations with immediate unsigned (signed) value

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

    public void rMul(OperationField opfield){
        mul128(read(opfield.rX), read(opfield.rY), opfield.rZ);
    }

    public void sMul(OperationField opfield){
        mul128(opfield.sX, read(opfield.rY), opfield.rZ);
    }

    public void rDivUnsigned(OperationField opfield){
        div128(read(opfield.rX), read(opfield.rY), read(opfield.rY+1), opfield.rZ);
    }

    public void uDivUnsigned(OperationField opfield){
        div128(opfield.uX, read(opfield.rY), read(opfield.rY+1), opfield.rZ);
    }

    public void rDivSigned(OperationField opfield){
        div64(read(opfield.rX), read(opfield.rY), opfield.rZ);
    }

    public void sDivSigned(OperationField opfield){
        div64(opfield.sX, read(opfield.rY), opfield.rZ);
    }

    public void div64(long b, long a, int regId){
        long sign = (a > 0 && b > 0) || (a < 0 && b < 0)? 1 : -1;
        long result = sign * Math.abs(a)/Math.abs(b);
        long remainder = a - b * result;
        write(regId, result);
        write(regId+1, remainder);
    }

    private void mul128(long a, long b, int regId){
        BigInteger result = BigInteger.valueOf(a).multiply(BigInteger.valueOf(b));
        long resultLow = result.longValue();
        long resultHigh = result.shiftRight(64).longValue();
        write(regId, resultLow);
        if(resultHigh != 0) cf = of = true;
    }

    private void div128(long b, long aLow, long aHigh, int regId){
        BigInteger a = new BigInteger(uLongToBytes(aHigh));
        //System.out.println(divisor);
        a = a.shiftLeft(64);
        a = a.add(new BigInteger(uLongToBytes(aLow)));
        BigInteger[] divAndRem = a.divideAndRemainder(new BigInteger(uLongToBytes(b)));
        long resultLow = divAndRem[0].longValue();
        long resultHigh = divAndRem[0].shiftRight(64).longValue();
        long remainder = divAndRem[1].longValue();
        write(regId, resultLow);
        write(regId+1, resultHigh);
        write(regId+2, remainder);
    }

    private static byte[] uLongToBytes(long data) {
        return new byte[]{
                0x00,
                (byte) ((data >> 56) & 0xFF),
                (byte) ((data >> 48) & 0xFF),
                (byte) ((data >> 40) & 0xFF),
                (byte) ((data >> 32) & 0xFF),
                (byte) ((data >> 24) & 0xFF),
                (byte) ((data >> 16) & 0xFF),
                (byte) ((data >> 8) & 0xFF),
                (byte) ((data) & 0xFF),
        };
    }

}
