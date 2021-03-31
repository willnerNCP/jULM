package de.fs_cse.core;

public class MemoryPage {
    public static int PAGE_SIZE = 1024;

    public byte[] bytes;

    public MemoryPage(){
        bytes = new byte[PAGE_SIZE];
    }

    public long get(int offset, int numBytes){
        if(offset < 0 || offset+numBytes > PAGE_SIZE) throw new IndexOutOfBoundsException("Offset out of bounds");
        if(offset%numBytes != 0) throw new IllegalArgumentException("Alignment error");

        long ret = ((long)bytes[offset] & 0xFFL);
        for(int i = 1; i < numBytes; i++){
            ret = (ret << 8) + ((long)bytes[i+offset] & 0xFFL);
        }
        return ret;
    }

    public void set(int offset, int numBytes, long value){
        if(offset < 0 || offset+numBytes > PAGE_SIZE) throw new IndexOutOfBoundsException("Offset out of bounds");
        if(offset%numBytes != 0) throw new IllegalArgumentException("Alignment error");
        for(int i = numBytes-1; i >= 0; i--){
            bytes[offset+i] = (byte)(value & 0xFFL);
            //>>>: unsigned shift
            value = value >>> 8;
        }
    }

    public void setLittleEndian(int offset, int numBytes, long value){
        if(offset < 0 || offset+numBytes > PAGE_SIZE) throw new IndexOutOfBoundsException("Offset out of bounds");
        if(offset%numBytes != 0) throw new IllegalArgumentException("Alignment error");

        for(int i = 0; i < numBytes; i++){
            bytes[offset+i] = (byte)(value & 0xFFL);
            //>>>: unsigned shift
            value = value >>> 8;
        }
    }

}
