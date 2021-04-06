package de.fs_cse.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import de.fs_cse.core.ALU;
import org.junit.jupiter.api.Test;

import de.fs_cse.core.MemoryPage;
import de.fs_cse.core.OperationField;
import de.fs_cse.core.VirtualMemory;

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
        memory.set(0, 4, input1);
        memory.set(-4, 4, input2);
        long value1 = memory.get(0, 4);
        long value2 = memory.get(-4, 4);
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
    }

}
