package de.fs_cse;

import de.fs_cse.core.MemoryPage;
import de.fs_cse.core.VirtualMemory;

public class Main {

    public static void main(String[] args) {
        testVirtualMemory();
    }

    public static void testVirtualMemory(){
        VirtualMemory memory = new VirtualMemory();
        long input1 = 0x89ABCDEF;
        long input2 = 0x01234567;
        memory.set(0, 4, input1);
        memory.set(-4, 4, input2);
        printAsHex(memory.get(0, 4));
        printAsHex(memory.get(-4, 4));
    }

    public static void testMemoryPage(){
        MemoryPage page = new MemoryPage();
        long input1 = 0x89ABCDEF;
        long input2 = 0x01234567;
        page.set(0, 4, input1);
        long value = page.get(0, 4);
        printAsHex(value);
        page.set(3, 3, input2);
        printAsHex(page.get(0, 7));
    }

    public static void printAsHex(long l){
        System.out.println(Long.toHexString(l));
    }
}
