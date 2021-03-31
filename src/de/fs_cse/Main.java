package de.fs_cse;

import de.fs_cse.core.MemoryPage;

public class Main {

    public static void main(String[] args) {
        testMemoryPage();
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
