package de.fs_cse.core;

public class OperationField {

    //register bytes are actually saved as int to prevent implicit signed casting from byte to int
    //also java internally stores bytes as ints so it doesnt really matter
    public int rX;    //register represented by byte x
    public int rY;    //register represented by byte y
    public int rZ;    //register represented by byte z

    public long uX;    //unsigned integer represented by byte x
    public long uXY;   //unsigned integer represented by word xy

    public long sX;    //signed integer represented by byte x
    public long sY;    //signed integer represented by byte y
    public long sXY;   //signed integer represented by word xy
    public long sXYZ;  //signed integer represented by the three bytes xyz

    public OperationField(int opfield){
        if(opfield >>> 24 != 0) throw new IllegalArgumentException("Trying to create operation field from more than 3 bytes!");

        rX = opfield >>> 16 & 0xFF;
        rY = opfield >>> 8 & 0xFF;
        rZ = opfield & 0xFF;

        //>>>: unsigned shift (fills in zeros)
        //implicit cast from int to long performs signed extension, but always fills in 0 in this case, because MSB is guaranteed to be 0.
        uX = opfield >>> 16;
        uXY = opfield >>> 8;

        //>>: signed shift
        //cast from int to long: signed extension (Source?)
        sX = (opfield << 8) >> 24;
        sY = (opfield << 16) >> 24; //Idee: 003B8E47 << 16 = 8E470000; 8E470000 >> 24 = FFFFFF8E
        sXY = (opfield << 8) >> 16;
        sXYZ = (opfield << 8) >> 8;
    }

    public String format_R(){
        return "%" + toHex(rX);
    }

    public String format_U8(){
        return Long.toUnsignedString(uX);
    }

    public String format_RR(){
        return "%" + toHex(rX) + ",\t%" + toHex(rY);
    }

    public String format_RRR(){
        return "%" + toHex(rX) + ",\t%" + toHex(rY) + ",\t%" + toHex(rZ);
    }

    public String format_URR(){
        return Long.toUnsignedString(uX) + ",\t%" + toHex(rY) + ",\t%" + toHex(rZ);
    }

    public String format_SRR(){
        return sX + ",\t%" + toHex(rY) + ",\t%" + toHex(rZ);
    }

    public String format_U16R(){
        return Long.toUnsignedString(uXY) + ",\t%" + toHex(rZ);
    }

    public String format_S16R(){
        return sXY + ",\t%" + toHex(rZ);
    }

    public String format_S24(){
        return sXYZ + "";
    }

    public String format_MRR_R(){
        return "M(%" + toHex(rX) + " + %" + toHex(rY) + ") ->\t%" + toHex(rZ);
    }

    public String format_MSR_R(){
        return "M(" + sX + " + %" + toHex(rY) + ") ->\t%" + toHex(rZ);
    }

    public String format_R_MRR(){
        return "%" + toHex(rX) + " ->\tM(%" + toHex(rY) + " + %" + toHex(rZ) + ")";
    }

    public String format_R_MSR(){
        return "%" + toHex(rX) + " ->\tM(" + sY + " + %" + toHex(rZ) + ")";
    }

    private static String toHex(Integer i){
        return "0x" + Integer.toHexString(i).toUpperCase();
    }


}
