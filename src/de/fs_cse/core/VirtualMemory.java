package de.fs_cse.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class VirtualMemory {
    private Map<Long, MemoryPage> pages;

    public ArrayList<ObserverMemory> observers;

    public VirtualMemory(){
        pages = new HashMap<>();
    }

    public long read(long address, int numBytes){
        long key = Long.divideUnsigned(address, MemoryPage.PAGE_SIZE);
        int offset = (int) Long.remainderUnsigned(address, MemoryPage.PAGE_SIZE);

        long value = 0;
        MemoryPage page = pages.get(key);
        if(page != null) value = page.get(offset, numBytes);

        for(ObserverMemory observer : observers) observer.onRead(address, numBytes, value);

        return value;
    }

    public void write(long address, int numBytes, long value){
        long key = Long.divideUnsigned(address, MemoryPage.PAGE_SIZE);
        int offset = (int) Long.remainderUnsigned(address, MemoryPage.PAGE_SIZE);

        MemoryPage page = pages.get(key);
        if(page == null){
            page = new MemoryPage();
            pages.put(key, page);
        }
        page.set(offset, numBytes, value);
        for(ObserverMemory observer : observers) observer.onWrite(address, numBytes, value);
    }
}
