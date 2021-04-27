package de.fs_cse.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class VirtualMemory {

    public static int PAGE_SIZE = 1024;

    public Map<Long, MemoryPage> pages;

    public ArrayList<ObserverMemory> observers;

    public VirtualMemory(){
        pages = new HashMap<>();
        observers = new ArrayList<>();
    }

    public void reset(){
        pages = new HashMap<>();
    }

    public void addObserver(ObserverMemory observer){
        observers.add(observer);
    }

    public long read(long address, int numBytes){
        long key = Long.divideUnsigned(address, PAGE_SIZE);
        int offset = (int) Long.remainderUnsigned(address, PAGE_SIZE);

        long value = 0;
        MemoryPage page = pages.get(key);
        if(page != null) value = page.get(offset, numBytes);

        for(ObserverMemory observer : observers) observer.onRead(address, numBytes, value);

        return value;
    }

    public void write(long address, int numBytes, long value){
        long key = Long.divideUnsigned(address, PAGE_SIZE);
        int offset = (int) Long.remainderUnsigned(address, PAGE_SIZE);

        MemoryPage page = pages.get(key);
        if(page == null){
            page = new MemoryPage();
            pages.put(key, page);
        }
        page.set(offset, numBytes, value);
        for(ObserverMemory observer : observers) observer.onWrite(address, numBytes, value);
    }

    public void loadProgram(int[] program){
        MemoryPage page = pages.get(0L);
        long key = 0L;
        int offset = 0;
        for(int i = 0; i < program.length; i++){
            if(i%PAGE_SIZE == 0){
                page = pages.get(key);
                if(page == null){
                    page = new MemoryPage();
                    pages.put(key, page);
                }
                key++;
                offset = 0;
            }
            page.set(offset, 4, program[i]);
            offset += 4;

        }
    }

    public class MemoryPage {
        public byte[] bytes;

        public MemoryPage(){
            bytes = new byte[PAGE_SIZE];
        }

        public long get(int offset, int numBytes){
            if(offset < 0 || offset+numBytes > PAGE_SIZE) throw new IndexOutOfBoundsException("Offset out of bounds");
            if(offset%numBytes != 0) throw new IllegalArgumentException("Alignment error");

            long ret = ((long)bytes[offset] & 0xFFL); //cast from byte to long: byte is sign extended, that's why we need &0xFF
            for(int i = 1; i < numBytes; i++){
                ret = (ret << 8) + ((long)bytes[i+offset] & 0xFFL);
            }
            return ret;
        }

        public void set(int offset, int numBytes, long value){
            if(offset < 0 || offset+numBytes > PAGE_SIZE) throw new IndexOutOfBoundsException("Offset out of bounds");
            if(offset%numBytes != 0) throw new IllegalArgumentException("Alignment error");
            for(int i = numBytes-1; i >= 0; i--){
                bytes[offset+i] = (byte)value; //cast to byte truncates everything but the lowest byte
                //>>>: unsigned shift
                value = value >>> 8;
            }
        }

        public void setLittleEndian(int offset, int numBytes, long value){
            if(offset < 0 || offset+numBytes > PAGE_SIZE) throw new IndexOutOfBoundsException("Offset out of bounds");
            if(offset%numBytes != 0) throw new IllegalArgumentException("Alignment error");

            for(int i = 0; i < numBytes; i++){
                bytes[offset+i] = (byte)value;
                //>>>: unsigned shift
                value = value >>> 8;
            }
        }

    }
}
