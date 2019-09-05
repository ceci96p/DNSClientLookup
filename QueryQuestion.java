package ca.ubc.cs.cs317.dnslookup;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.Arrays;

import java.nio.ByteBuffer;

public class QueryQuestion {
    private ByteBuffer questionByte;
    private int questionByteSize;
    private String nameLookedUp;
    private String typeQ;

    public byte[] setBuffer(String name, RecordType type) {
        nameLookedUp = name;

        String[] nameString = name.split("\\.");
        int numBytes = 0;
        for(int l = 0; l < nameString.length; l++){
            numBytes += (nameString[l].length() + 1);
        }
        questionByte = ByteBuffer.wrap(new byte[numBytes + 5]);
        questionByteSize = numBytes + 5;


        for(int i = 0; i < nameString.length; i++){
            byte [] section = new byte[nameString[i].length() + 1];
            questionByte.put( (byte) nameString[i].length());
            byte[] temp = nameString[i].getBytes(StandardCharsets.US_ASCII);

            for (int p = 0 ; p < temp.length; p++){
                questionByte.put(temp[p]);
            }
        }
        byte[] qNameTerminator = new byte[1];
        qNameTerminator[0] = (byte) 0b00000000;
        questionByte.put(qNameTerminator[0]);

        byte[] qType = new byte[2];
        switch (type.getCode()){ //A, AAAA, CNAME, NS
            case 1: //A
                qType[0] = (byte) 0b00000000;
                qType[1] = (byte) 0b00000001;
                typeQ = "A";
                break;
            case 2: //NS
                qType[0] = (byte) 0b00000000;
                qType[1] = (byte) 0b00000010;
                typeQ = "NS";
                break;
            case 5: //CNAME
                qType[0] = (byte) 0b00000000;
                qType[1] = (byte) 0b00000101;
                typeQ = "CNAME";
                break;
            case 28: //AAAA
                qType[0] = (byte) 0b00000000;
                qType[1] = (byte) 0b00011100;
                typeQ = "AAAA";
                break;
                default:
                    break;
        }
        questionByte.put(qType[0]);
        questionByte.put(qType[1]);

        byte[] qClass = new byte[2];
        qClass[0] = (byte) 0b00000000;
        qClass[1] = (byte) 0b00000001;
        questionByte.put(qClass[0]);
        questionByte.put(qClass[1]);

        return questionByte.array();
    }

    public int getQuestionByteSize(){
        return questionByteSize;

    }

    public String getNameLookedUp(){
        return nameLookedUp;
    }

    public String getType(){
        return typeQ;
    }
}
