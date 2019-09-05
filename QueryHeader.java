package ca.ubc.cs.cs317.dnslookup;


import java.nio.ByteBuffer;
import java.util.Random;

public class QueryHeader {

    private ByteBuffer headerByte = ByteBuffer.wrap(new byte[12]);
    private String ID;
    private int HeaderByteSize = 12;

    public byte[] setBuffer(){
        Random randomGenerator = new Random();
        byte s1 = (byte) randomGenerator.nextInt(Byte.MAX_VALUE + 0);
        byte s2 = (byte) randomGenerator.nextInt(Byte.MAX_VALUE + 0);

        headerByte.put((byte)s1);
        headerByte.put((byte)s2);

        ID =  Integer.toString( ( s1 &0xff) <<8 | s2 & 0xff);

        byte[] bitFields = new byte[2];
        bitFields[0] = (byte) 0b00000000;
        bitFields[1] = (byte) 0b00000000;
        headerByte.put(bitFields[0]);
        headerByte.put(bitFields[1]);

        byte[] questionCount = new byte[2];
        questionCount[0] = (byte) 0b00000000;
        questionCount[1] = (byte) 0b00000001;
        headerByte.put(questionCount[0]);
        headerByte.put(questionCount[1]);

        byte[] answerCount = new byte[2];
        answerCount[0] = (byte) 0b00000000;
        answerCount[1] = (byte) 0b00000000;
        headerByte.put(answerCount[0]);
        headerByte.put(answerCount[1]);

        byte[] authorityRecordsCount = new byte[2];
        authorityRecordsCount[0] = (byte) 0b00000000;
        authorityRecordsCount[1] = (byte) 0b00000000;
        headerByte.put(authorityRecordsCount[0]);
        headerByte.put(authorityRecordsCount[1]);

        byte[] resourceRecordsCount = new byte[2];
        resourceRecordsCount[0] = (byte) 0b00000000;
        resourceRecordsCount[1] = (byte) 0b00000000;
        headerByte.put(resourceRecordsCount[0]);
        headerByte.put(resourceRecordsCount[1]);

        return  headerByte.array();
    }

    public String getHeaderID(){
        return ID;

    }

    public int getHeaderByteSize(){
        return HeaderByteSize;
    }


}
