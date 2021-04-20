package de.fs_cse.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import de.fs_cse.core.*;
import org.junit.jupiter.api.Test;

public class Tests {

    @Test
    void testMemoryPage(){
        MemoryPage page = new MemoryPage();
        long input1 = 0x89ABCDEFL;
        long input2 = 0x01234567L;
        page.set(0, 4, input1);
        long value = page.get(0, 4);
        assertEquals(input1, value);
        page.set(3, 3, input2);
        value = page.get(0, 8);
        assertEquals(0x89ABCD2345670000L, value);
    }

    @Test
    void testVirtualMemory(){
        VirtualMemory memory = new VirtualMemory();
        long input1 = 0x89ABCDEFL;
        long input2 = 0x01234567L;
        memory.write(0, 4, input1);
        memory.write(-4, 4, input2);
        long value1 = memory.read(0, 4);
        long value2 = memory.read(-4, 4);
        assertEquals(input1, value1);
        assertEquals(input2, value2);
    }

    @Test
    void testOperationField(){
        OperationField opfield = new OperationField(0x00AABBCC);
        assertEquals(0xAA, opfield.rX);
        assertEquals(0xBB, opfield.rY);
        assertEquals(0xCC, opfield.rZ);

        assertEquals(0xAAL, opfield.uX);
        assertEquals(0xAABBL, opfield.uXY);

        assertEquals(-86, opfield.sX);
        assertEquals(-69, opfield.sY);
        assertEquals(-21829, opfield.sXY);
        assertEquals(-5588020, opfield.sXYZ);
    }

    @Test
    void testALU(){
        ALU alu = new ALU();
        alu.write(0, 1);
        assertEquals(0, alu.read(0));

        alu.uLoad(new OperationField(0x00001002));

        OperationField opfield = new OperationField(0x00A00101);

        //unsigned loading
        alu.uLoad(opfield);
        assertEquals(0xA001L, alu.read(1));
        alu.uShiftLeftLoad(opfield);
        assertEquals(0xA001A001L, alu.read(1));

        //unsigned shifting
        alu.uLoad(opfield);
        OperationField rshift = new OperationField(0x00020101);
        OperationField ushift = new OperationField(0x00100101);
        alu.rShiftLeft(rshift);
        assertEquals(0xA0010000L, alu.read(1));
        alu.rLogicalShiftRight(rshift);
        assertEquals(0xA001L, alu.read(1));
        alu.uShiftLeft(ushift);
        assertEquals(0xA0010000L, alu.read(1));
        alu.uLogicalShiftRight(ushift);
        assertEquals(0xA001L, alu.read(1));

        //shifting too far
        alu.uShiftLeft(new OperationField(0x00310101));
        assertTrue(alu.cf);

        //signed loading
        alu.sLoad(opfield);
        assertEquals((long)0xFFFFA001, alu.read(1));
        alu.uShiftLeftLoad(opfield);
        assertEquals((long)0xA001A001, alu.read(1));

        //signed shifting
        alu.sLoad(opfield);
        alu.rShiftLeft(rshift);
        assertEquals((long)0xA0010000, alu.read(1));
        alu.rArithmeticShiftRight(rshift);
        assertEquals((long)0xFFFFA001, alu.read(1));
        alu.uShiftLeft(ushift);
        assertEquals((long)0xA0010000, alu.read(1));
        alu.uArithmeticShiftRight(ushift);
        assertEquals((long)0xFFFFA001, alu.read(1));


        //add and sub
        //unsigned
        alu.sLoad(new OperationField(0x00000101));
        alu.sLoad(new OperationField(0x00FFFF02));
        OperationField rOp = new OperationField(0x00010202);
        alu.rAdd(rOp);
        assertEquals(0, alu.read(2));
        assertTrue(alu.cf);
        alu.rSub(rOp);
        assertEquals(-1, alu.read(2));
        assertTrue(alu.cf);
        OperationField uOp = new OperationField(0x00100101);
        alu.uAdd(uOp);
        assertEquals(17, alu.read(1));
        assertFalse(alu.cf);
        alu.uSub(uOp);
        assertEquals(1, alu.read(1));
        assertFalse(alu.cf);
        //signed
        alu.registers[1] = -1;
        alu.registers[2] = Long.MIN_VALUE;
        alu.rAdd(rOp);
        assertEquals(Long.MAX_VALUE, alu.read(2));
        assertTrue(alu.of);
        alu.rSub(rOp);
        assertEquals(Long.MIN_VALUE, alu.read(2));
        assertTrue(alu.of);

        //bitwise operations
        alu.uLoad(new OperationField(0x00F0F101));
        alu.uLoad(new OperationField(0x000F0F02));
        OperationField buf = new OperationField(0x00010203);
        alu.and(buf);
        assertEquals(0x1L, alu.read(3));
        alu.or(buf);
        assertEquals(0xFFFFL, alu.read(3));
        alu.not(new OperationField(0x00020300));
        assertEquals((long)0xFFFFF0F0, alu.read(3));

        //mul div
        //unsigned
        alu.reset();
        alu.registers[1] = -1;
        alu.registers[2] = -1;
        alu.registers[3] = -1;
        alu.rDivUnsigned(new OperationField(0x00030104));
        assertEquals(1, alu.read(4));
        assertEquals(1, alu.read(5));
        assertEquals(0, alu.read(6));
        alu.registers[1] = -1;
        alu.registers[2] = 0;
        alu.registers[3] = 17;
        alu.rDivUnsigned(new OperationField(0x00030104));
        assertEquals(0x0F0F0F0F0F0F0F0FL, alu.read(4));
        assertEquals(0, alu.read(5));
        assertEquals(0, alu.read(6));
        alu.registers[1] = 25;
        alu.registers[2] = 0;
        alu.uDivUnsigned(new OperationField(0x00040103));
        assertEquals(6, alu.read(3));
        assertEquals(0, alu.read(4));
        assertEquals(1, alu.read(5));
        //signed
        alu.reset();
        alu.registers[1] = -25;
        alu.registers[2] = 113;
        alu.rMul(new OperationField(0x00010203));
        alu.rDivSigned(new OperationField(0x00020304));
        assertEquals(-25, alu.read(4));
        alu.registers[1] = -17;
        alu.sDivSigned(new OperationField(0x00040104));
        assertEquals(-4, alu.read(4));
        assertEquals(-1, alu.read(5));

    }

    @Test
    void testCPU(){
        //sign extend
        long bitPattern = 0xFFL;
        bitPattern = CPU.signExtend(bitPattern, 1);
        assertEquals(-1, bitPattern);

        //bus
        CPU cpu = new CPU();
        ALU alu = cpu.alu;
        alu.write(1, 4);
        alu.write(2, 12);
        alu.write(3, 0x80000001L);
        OperationField rStoreOp = new OperationField(0x00030102);
        OperationField sStoreOp = new OperationField(0x0003FC02);
        cpu.rStore(rStoreOp, 4, 0);
        cpu.sStore(sStoreOp, 4);
        OperationField rFetchOp = new OperationField(0x00010204);
        OperationField sFetchOp = new OperationField(0x00FC0205);
        cpu.rFetch(rFetchOp, 4, 0, false);
        cpu.sFetch(sFetchOp, 4, false);
        assertEquals(0x80000001L, alu.read(4));
        assertEquals(0x80000001L, alu.read(5));
        cpu.rFetch(rFetchOp, 4, 0, true);
        cpu.sFetch(sFetchOp, 4, true);
        assertEquals((long)0x80000001, alu.read(4));
        assertEquals((long)0x80000001, alu.read(5));

        //jumps
        cpu.absJmp(new OperationField(0x00010200));
        assertEquals(4, cpu.ip);
        OperationField jmpOp = new OperationField(0x00000001);
        cpu.relJmp(jmpOp);
        assertEquals(8, cpu.ip);
        //jz jnz
        cpu.incrementIP();
        alu.sub(10, 10, 0);
        cpu.jnz(jmpOp);
        assertFalse(cpu.jumped);
        cpu.jz(jmpOp);
        assertTrue(cpu.jumped);
        cpu.incrementIP();
        alu.sub(10, 11, 0);
        cpu.jz(jmpOp);
        assertFalse(cpu.jumped);
        cpu.jnz(jmpOp);
        assertTrue(cpu.jumped);
    }

}
