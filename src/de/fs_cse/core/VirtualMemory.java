package de.fs_cse.core;

import java.util.HashMap;
import java.util.Map;

public class VirtualMemory {
    private Map<Long, MemoryPage> pages;

    public VirtualMemory(){
        pages = new HashMap<>();
    }

    public long get(long address, int numBytes){
        long key = Long.divideUnsigned(address, MemoryPage.PAGE_SIZE);
        int offset = (int) Long.remainderUnsigned(address, MemoryPage.PAGE_SIZE);

        MemoryPage page = pages.get(key);
        if(page == null) return 0;
        else return page.get(offset, numBytes);
    }

    public void set(long address, int numBytes, long value){
        long key = Long.divideUnsigned(address, MemoryPage.PAGE_SIZE);
        int offset = (int) Long.remainderUnsigned(address, MemoryPage.PAGE_SIZE);

        MemoryPage page = pages.get(key);
        if(page == null){
            page = new MemoryPage();
            pages.put(key, page);
        }
        page.set(offset, numBytes, value);
    }
}
