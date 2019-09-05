package ca.ubc.cs.cs317.dnslookup;

import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class DecodePacket {
    public int packetID;
    public int Rcode;
    public int questionCount;
    public int answerCount;
    public int authorityCount;
    public int additionalCount;
    private byte[] data;
    private RRecord[] Answers;
    private RRecord[] AuthoritativeNameServers;
    private RRecord[] AdditionalRecords;
    private int location;
    //private DNSNode[] answerNode;

        public DecodePacket(DatagramPacket responsePacket,QueryQuestion q){ //decode header of packet
                packetID = (((responsePacket.getData()[0] << 8) & 0x0000ff00) | (responsePacket.getData()[1] & 0x000000ff));
                Rcode = (responsePacket.getData()[3] & 0x000000ff) & 0x0F;
                questionCount = (((responsePacket.getData()[4] << 8) & 0x0000ff00) | (responsePacket.getData()[5] & 0x000000ff));
                answerCount = (((responsePacket.getData()[6] << 8) & 0x0000ff00) | (responsePacket.getData()[7] & 0x000000ff));
                authorityCount = (((responsePacket.getData()[8] << 8) & 0x0000ff00) | (responsePacket.getData()[9] & 0x000000ff));
                additionalCount = (((responsePacket.getData()[10] << 8) & 0x0000ff00) | (responsePacket.getData()[11] & 0x000000ff));
                data = responsePacket.getData();
                Answers = new RRecord[answerCount];
                AuthoritativeNameServers = new RRecord[authorityCount];
                AdditionalRecords = new RRecord[additionalCount];
                location = 12 + (questionCount * q.getQuestionByteSize());

                if (answerCount > 0 ){
                    setAnswers(q);
                }
                if (authorityCount > 0){
                    setAuthoritativeNameServers(q);
                }
               if (additionalCount > 0){
                    setAdditionalRecords(q);
                }
        }

    private void setAnswers(QueryQuestion q) { //decodes answer section and places each record on object RRecord in an array in DecodePacket
        for(int j= 0; j < answerCount; j++){
            Answers[j] = new RRecord();

            int answersNamePointer = data[location] & 0xFF;
            location++;
            String finalName = "";
            if (answersNamePointer == 192) { //if record name is in a pointer
                int answersNameOffset = data[location] & 0xFF;
                location++;
                int sizeofEnding = data[answersNameOffset] & 0xFF;
                String word = "";
                int nextValue;
                while (true) {
                    if(sizeofEnding == 192){
                        answersNameOffset = data[answersNameOffset + 1];
                        sizeofEnding = data[answersNameOffset];
                    }
                    for (int l = 1; l <= sizeofEnding; l++) {  // start at 1 to get size to READ
                        byte aByte = data[answersNameOffset + l];//
                        try {
                            String letter = new String(new byte[]{(byte) aByte}, "US-ASCII");
                            word += letter;
                        } catch (UnsupportedEncodingException e) {
                            System.out.println("error decoding");
                        }
                    }
                    nextValue = data[answersNameOffset + sizeofEnding + 1] & 0xFF;
                    if (nextValue == 0) { //if we find the end of the string
                        finalName += word;
                        Answers[j].setName(finalName);//setter
                        break;
                    } else if (nextValue == 192) { // if there is a pointer within a pointer
                        finalName += word + ".";
                        word = "";
                        answersNameOffset = data[answersNameOffset + sizeofEnding + 2] & 0xFF;
                        sizeofEnding = data[answersNameOffset] & 0xFF;
                    } else {//if there is more to append at a pointed value
                        finalName += word + ".";
                        word = "";
                        answersNameOffset += sizeofEnding + 1;
                        sizeofEnding = nextValue;
                    }
                }
            }else { // if name is hardcoded inside section check for length of each word and loop and check for next
                String word = "";
                while (answersNamePointer != 0) {
                    for (int x = 0; x < answersNamePointer ; x++) {
                        byte aByte = data[location];//
                        try {
                            String letter = new String(new byte[]{(byte) aByte}, "US-ASCII");
                            word += letter;
                        } catch (UnsupportedEncodingException e) {
                            System.out.println("error decoding");
                        }
                        location++;
                    }
                    answersNamePointer = data[location] & 0xFF;
                    location++;
                    if (answersNamePointer == 0){
                        Answers[j].setName(word);//setter
                        break;
                    }else{
                        word +=  ".";
                    }
                }
            }

            int type = (((data[location] << 8) & 0xFF) | data[location + 1]  & 0xFF);//get type
            location += 2;
            Answers[j].setType(type); //setter

            location += 2;// + 2 accounts for CLASS size

            int ttl1 = data[location ] & 0xFF; //get ttl
            int ttl2 = data[location + 1] & 0xFF;
            int ttl3 = data[location + 2] & 0xFF;
            int ttl4 = data[location + 3] & 0xFF;
            location+= 4;

            long TTL = ((ttl1 << 24) | (ttl2 << 16) | (ttl3 << 8) | (ttl4)) & 0xffffffffL;
            Answers[j].setTtl((int)TTL);//setter

            int lengthRdata = (((data[location] << 8) & 0xFF) | (data[location + 1] & 0xFF)); //get Rdata length

            Answers[j].setNameLookedUp(q.getNameLookedUp()); //setter
            location+=2;

            switch (type){ //check what to set up on Data section based on type
                case 1: //A
                    setAddressIPV4(lengthRdata,Answers[j]);
                    break;
                case 2: //NS
                    setNameServerName(lengthRdata,Answers[j]);
                    break;
                case 5: //CNAME
                    setNameServerName(lengthRdata,Answers[j]);
                    break;
                case 6: //SOA
                    Answers[j].setNameServer("----");
                    Answers[j].setisInetAddressFalse();
                    break;
                case 15: //MX
                    Answers[j].setNameServer("----");
                    Answers[j].setisInetAddressFalse();
                    break;
                case 28: //AAAA
                    setAddressIPV6(lengthRdata,Answers[j]);
                    break;
                default: //0 = OTHER
                    Answers[j].setNameServer("----");
                    Answers[j].setisInetAddressFalse();

            }
        }
    }

    public void setAuthoritativeNameServers(QueryQuestion q) { //decodes authoritative section and places each record on object RRecord in an array in DecodePacket
            for(int j= 0; j < authorityCount; j++){
                AuthoritativeNameServers[j] = new RRecord();

                int authorityNamePointer = data[location] & 0xFF;
                location++;

                String finalName = "";
                if (authorityNamePointer == 192) { //if record name is in a pointer
                    int authorityNameOffset = data[location] & 0xFF;
                    location++;
                    int sizeofEnding = data[authorityNameOffset] & 0xFF;
                    String word = "";
                    int nextValue;
                    while (true) {
                        if(sizeofEnding == 192){
                            authorityNameOffset = data[authorityNameOffset + 1];
                            sizeofEnding = data[authorityNameOffset];
                        }
                        for (int l = 1; l <= sizeofEnding; l++) {  // start at 1 to get size to READ
                            byte aByte = data[authorityNameOffset + l];//
                            try {
                                String letter = new String(new byte[]{(byte) aByte}, "US-ASCII");
                                word += letter;
                            } catch (UnsupportedEncodingException e) {
                                System.out.println("error decoding");
                            }
                        }
                        nextValue = data[authorityNameOffset + sizeofEnding + 1] & 0xFF;
                        if (nextValue == 0) { //if we find the end of the string
                            finalName += word;
                            AuthoritativeNameServers[j].setName(finalName);//setter
                            break;
                        } else if (nextValue == 192) { // if there is a pointer within a pointer
                            finalName += word + ".";
                            word = "";
                            authorityNameOffset = data[authorityNameOffset + sizeofEnding + 2] & 0xFF;
                            sizeofEnding = data[authorityNameOffset] & 0xFF;
                        } else {//if there is more to append at a pointed value
                            finalName += word + ".";
                            word = "";
                            authorityNameOffset += sizeofEnding + 1;
                            sizeofEnding = nextValue;
                        }
                    }
                }else { // if name is hardcoded inside section check for length of each word and loop and check for next
                    String word = "";
                    while (authorityNamePointer != 0) {
                        for (int x = 0; x < authorityNamePointer ; x++) {
                            byte aByte = data[location];
                            try {
                                String letter = new String(new byte[]{(byte) aByte}, "US-ASCII");
                                word += letter;
                            } catch (UnsupportedEncodingException e) {
                                System.out.println("error decoding");
                            }
                            location++;
                        }
                        authorityNamePointer = data[location] & 0xFF;
                            location++;
                        if (authorityNamePointer == 0){
                            AuthoritativeNameServers[j].setName(word);//setter
                            break;
                        }else{
                            word +=  ".";
                        }
                    }
                }

                int type = (((data[location] << 8) & 0xFF) | data[location + 1]  & 0xFF);// get type
                location += 2;
                AuthoritativeNameServers[j].setType(type); //setter

                location += 2;// + 2 accounts for CLASS size

                int ttl1 = data[location ] & 0xFF; //get ttl
                int ttl2 = data[location + 1] & 0xFF;
                int ttl3 = data[location + 2] & 0xFF;
                int ttl4 = data[location + 3] & 0xFF;
                location+= 4;

                long TTL = ((ttl1 << 24) | (ttl2 << 16) | (ttl3 << 8) | (ttl4)) & 0xffffffffL;
                AuthoritativeNameServers[j].setTtl((int)TTL); //setter

                int lengthRdata = (((data[location] << 8) & 0xFF) | (data[location + 1] & 0xFF)); //get length Rdata
                AuthoritativeNameServers[j].setNameLookedUp(q.getNameLookedUp());//setter
                location+=2;

                switch (type){ ////check what to set up on Data section based on type
                        case 1: //A
                            setAddressIPV4(lengthRdata,AuthoritativeNameServers[j]);
                            break;
                        case 2: //NS
                            setNameServerName(lengthRdata,AuthoritativeNameServers[j]);
                            break;
                        case 5: //CNAME
                            setNameServerName(lengthRdata,AuthoritativeNameServers[j]);
                            break;
                        case 6: //SOA
                            AuthoritativeNameServers[j].setNameServer("----");
                            AuthoritativeNameServers[j].setisInetAddressFalse();
                            break;
                        case 15: //MX
                            AuthoritativeNameServers[j].setNameServer("----");
                            AuthoritativeNameServers[j].setisInetAddressFalse();
                            break;
                        case 28: //AAAA
                            setAddressIPV6(lengthRdata,AuthoritativeNameServers[j]);
                            break;
                        default: //0 = OTHER
                            AuthoritativeNameServers[j].setNameServer("----");
                            AuthoritativeNameServers[j].setisInetAddressFalse();
                    }
                }
    }
    private int getOffsetValue (int offsetValue, int nextByte){ //function : takes CX  and c0-value --> get the offset to goto to
        String hex =Integer.toHexString(offsetValue); // convert to HEX
        int temp = offsetValue<< 2 &0xFF;   // remove 11 bits in front of byte
        int value = (temp <<6  | nextByte);
        return value;
    }

    // function: gets the words from the ptr location
    public String resolvePtr(int c, int nextByte){
        String word="";
        String finalName="";
        int nextValue=0;
        int additionalNameOffset = getOffsetValue(c,nextByte); // get NEW LOCATION
        int sizeofEnding=  data[additionalNameOffset];

        while (true) {
            // function: Reads the amount of bytes to read
            for (int l = 1; l <= sizeofEnding; l++) {  // start at 1 to get size to READ
                byte aByte = data[additionalNameOffset + l]; // at GIVEN LOCATION OFFSET
                try {
                    String letter = new String(new byte[]{(byte) aByte}, "US-ASCII");
                    word += letter;
                } catch (UnsupportedEncodingException e) {
                    System.out.println("error decoding");
                }
            }

            nextValue = data[additionalNameOffset + sizeofEnding + 1] & 0xFF; //the value after reading X-bytes
            if (nextValue == 0) { //if we find the end of the string
                finalName += word;
                return finalName;
                //break;

            } else if (nextValue >=192& nextValue<=207) { // if there is a pointer within a pointer
                finalName += word + ".";
                word = "";
                additionalNameOffset = data[additionalNameOffset + sizeofEnding + 2] & 0xFF;
                sizeofEnding = data[additionalNameOffset] & 0xFF;
            } else {//if there is more to append at a pointed value
                finalName += word + ".";
                word = "";
                additionalNameOffset += sizeofEnding + 1;
                sizeofEnding = nextValue;
            }
        }
    }

    private void setAdditionalRecords(QueryQuestion q) { //decodes additional section and places each record on object RRecord in an array in DecodePacket
        for(int j= 0; j < additionalCount; j++){
            AdditionalRecords[j] = new RRecord();

            int additionalNamePointer = data[location] & 0xFF;
            location++; // at c0-value OR firstLetter
            String finalName = "";
            if (additionalNamePointer >= 192 & additionalNamePointer<=207 ) {
                int additionalNameOffset =getOffsetValue(additionalNamePointer, data[location]&0xFF);
                location++;


                int myOffset= getOffsetValue(additionalNamePointer, additionalNameOffset);
                int sizeofEnding = data[myOffset] & 0xFF;

                String word = "";
                int nextValue;
                while (true) {
                    if(sizeofEnding >= 192 & sizeofEnding<=207){//Added
                        additionalNameOffset = data[additionalNameOffset + 1];
                        additionalNameOffset= getOffsetValue(additionalNamePointer, additionalNameOffset); // SET: new offset to goto
                        sizeofEnding = data[myOffset];
                    }
                    // function: Reads the amount of bytes to read
                    for (int l = 1; l <= sizeofEnding; l++) {  // start at 1 to get size to READ
                        byte aByte = data[additionalNameOffset + l]; // at GIVEN LOCATION OFFSET
                        try {
                            String letter = new String(new byte[]{(byte) aByte}, "US-ASCII");
                            word += letter;
                        } catch (UnsupportedEncodingException e) {
                            System.out.println("error decoding");
                        }
                    }
                    nextValue = data[additionalNameOffset + sizeofEnding + 1] & 0xFF; //the value after reading X-bytes
                    if (nextValue == 0) { //if we find the end of the string
                        finalName += word;
                        AdditionalRecords[j].setName(finalName);//setter
                        break;

                    } else if (nextValue >=192& nextValue<=207) { // if there is a pointer within a pointer
                        finalName += word + ".";
                        word = "";
                        additionalNameOffset = data[additionalNameOffset + sizeofEnding + 2] & 0xFF;
                        sizeofEnding = data[additionalNameOffset] & 0xFF;
                    }

                    else {//if there is more to append at a pointed value
                        finalName += word + ".";
                        word = "";
                        additionalNameOffset += sizeofEnding + 1;
                        sizeofEnding = nextValue;
                    }
                }
            }
            else { // if name is hardcoded inside section check for length of each word and loop and check for next
                String word = "";
                while (additionalNamePointer != 0) {
                    for (int x = 0; x < additionalNamePointer ; x++) {
                        byte aByte = data[location];
                        try {
                            String letter = new String(new byte[]{(byte) aByte}, "US-ASCII");
                            word += letter;
                        } catch (UnsupportedEncodingException e) {
                            System.out.println("error decoding");
                        }
                        location++;
                    }

                    additionalNamePointer = data[location] & 0xFF;
                    location++;
                    if (additionalNamePointer == 0){
                        AdditionalRecords[j].setName(word);//setter
                        break;
                    }
                    if (additionalNamePointer >192 || additionalNamePointer<=207){
                        word+= resolvePtr(additionalNamePointer, data[location]&0xFF);
                        AdditionalRecords[j].setName(word);
                        location++;
                        break;
                    }
                    else{
                        word +=  ".";
                    }
                }
            }

            int type = (((data[location] << 8 & 0xFF) | data[location + 1]  & 0xFF)); //get type
            location += 2;
            AdditionalRecords[j].setType(type);//setter

            location += 2;// + 2 accounts for CLASS size

            int ttl1 = data[location ] & 0xFF;//get ttl
            int ttl2 = data[location + 1] & 0xFF;
            int ttl3 = data[location + 2] & 0xFF;
            int ttl4 = data[location + 3] & 0xFF;

            location+= 4;

            long TTL = ((ttl1 << 24) | (ttl2 << 16) | (ttl3 << 8 | (ttl4)) & 0xffffffffL);
            AdditionalRecords[j].setTtl((int)TTL); //setter

            int lengthRdata = (((data[location] << 8 & 0xFF) | (data[location + 1] & 0xFF)));//get length of Rdata
            AdditionalRecords[j].setNameLookedUp(q.getNameLookedUp()); //setter
            location+=2; // this makes us at rData

            switch (type){ //check what to set up on Data section based on type
                case 1: //A
                    setAddressIPV4(lengthRdata,AdditionalRecords[j]);
                    break;
                case 2: //NS
                    setNameServerName(lengthRdata,AdditionalRecords[j]);
                    break;
                case 5: //CNAME
                    setNameServerName(lengthRdata,AdditionalRecords[j]);
                    break;
                case 6: //SOA
                    AdditionalRecords[j].setNameServer("----");
                    AdditionalRecords[j].setisInetAddressFalse();

                    break;
                case 15: //MX
                    AdditionalRecords[j].setNameServer("----");
                    AdditionalRecords[j].setisInetAddressFalse();
                    break;
                case 28: //AAAA
                    setAddressIPV6(lengthRdata,AdditionalRecords[j]);;
                    break;
                default: //0 = OTHER
                    AdditionalRecords[j].setNameServer("----");
                    AdditionalRecords[j].setisInetAddressFalse();
            }
        }
    }
    public void setAddressIPV4(int lengthRdata, RRecord A){ //read hex to IPV4 format
            String address = "";
            for(int i = 0 ; i < lengthRdata; i++) {
                int number = data[location + i]  & 0xFF;
                if (i == lengthRdata-1){
                    address+= Integer.toString(number);
                }else {
                    address += Integer.toString(number) + ".";
                }
            }
            location+= lengthRdata;
            A.setNameServer(address);
    }
    private int twoBytesToInteger(byte byte1, byte byte2) { //turns 2 bytes into integer
        int first = ((byte1 & 0xFF) << 8);
        int second = (byte2 & 0xFF);
        return first + second;
    }

    public void setAddressIPV6(int lengthRdata, RRecord AAAA) { //read hex to IPV6 format
        String address = "";
        InetAddress addr;
        int length = lengthRdata/2;

        for (int i = 0; i < length; i++) { // do half bc: we take 2 values each time
            byte first =data[location++];
            byte second =data[location++];
            int firstByte = twoBytesToInteger(first, second); // get the INTEGER of 2 bytes
            String hexString = Integer.toHexString(firstByte);  // GET 2-BYTE representation of an INT.
            address += hexString + ":";
        }
        address= address.substring(0, address.length()-1); // take away the last :
        try {
            addr = InetAddress.getByName(address);
        }catch (UnknownHostException e) {
            return;
        }

        String stringAddr = addr.toString();// convert INETAddress -> string
        String addr2 = stringAddr.substring(1);
        AAAA.setNameServer(addr2);
    }

    public void setNameServerName(int lengthRdata, RRecord nameServer){// if record is of type NS of CNAME then it gets the address in string format
        String finalString = "";
        int currentPositionOffset = 0;
        int p = 0;
        int nextValue=192;
        nameServer.setisInetAddressFalse();
        while( p < lengthRdata){
            int sizeToRead = data[location] & 0xFF; //get the size to Read
            location++;
            if (sizeToRead == 192) {//c0* pointer first time only
                int c0Offset = data [location] & 0xFF;
                int sizeofEnding = data[c0Offset] & 0xFF;
                String word = "";
                while(true) {
                    for (int l = 1; l <= sizeofEnding; l++) {  // start at 1 to get size to READ
                        byte aByte = data[c0Offset + l];//
                        try {
                            String letter = new String(new byte[]{(byte) aByte}, "US-ASCII");
                            word += letter;
                        } catch (UnsupportedEncodingException e) {
                            System.out.println("error decoding");//TODO check error to throw
                        }
                    }
                    nextValue = data[c0Offset + sizeofEnding + 1]& 0xFF;
                    if (nextValue == 0) { //if we find the end of the string
                        location++; // bc currently we are at c0-value. need to goto next one
                        finalString += word;
                        nameServer.setNameServer(finalString);
                        return;
                    }else if(nextValue == 192){ // if there is a pointer within a pointer
                        finalString += word + ".";
                        word = "";
                        c0Offset = data[c0Offset + sizeofEnding + 2] & 0xFF;
                        sizeofEnding = data[c0Offset] & 0xFF;
                    }
                    else{//if there is more to append at a pointed value
                        finalString += word + ".";
                        word = "";
                        c0Offset += sizeofEnding + 1;
                        sizeofEnding = nextValue;
                    }
                }
            }
            String word = ""; //for string found in RData
            for (int b=0 ; b < sizeToRead; b++) {  // iterate through all the bytes in Rdata
                byte aByte = data[location + b];

                try {
                    String letter = null;
                    letter = new String(new byte[]{(byte) aByte}, "US-ASCII");
                    word += letter;
                } catch (UnsupportedEncodingException e) {
                    System.out.println("error decoding");
                }
            }
            location+= sizeToRead;
            currentPositionOffset += sizeToRead + 1;
            p = currentPositionOffset ;
            finalString += word + ".";
        }
        finalString = finalString.substring(0,finalString.length()-2);
        nameServer.setNameServer(finalString);
    }

    public int getResponseID(){
            return packetID;
        }

    public boolean isAuthoritative(){
            if (answerCount > 0){
                return true;
            }
            return false;
        }

    public int getQuestionCount(){
        return questionCount;
    }

    public int getAnswerCount(){
        return answerCount;
    }

    public int getAuthorityCount(){
        return authorityCount;
    }

    public int getAdditionalCount(){
        return additionalCount;
    }

    public RRecord[] getAnswers(){
         return Answers;
    }

    public RRecord[] getAuthoritativeNameServers(){
         return AuthoritativeNameServers;
    }

    public RRecord[] getAdditionalRecords(){
        return AdditionalRecords;
    }

    public int getRcode(){
        return Rcode;
    }

    public void convertDecodedPacketToResourceRecord(RRecord[] section, DNSCache cache) throws UnknownHostException { //parses RRecrd to DNSNode object
            for( int n = 0; n < section.length; n++ ) {
            String typeString = section[n].getType();
            RecordType typeNum = RecordType.A;

            if(typeString =="A"){
                typeNum = RecordType.A;
            }else if(typeString =="NS"){
                typeNum = RecordType.NS;

            }else if(typeString =="CNAME"){
                typeNum = RecordType.CNAME;

            }else if(typeString =="SOA"){
                typeNum = RecordType.SOA;

            }else if(typeString =="MX"){
                typeNum = RecordType.MX;

            }else if(typeString =="AAAA"){
                typeNum = RecordType.AAAA;

            }else if(typeString == "OTHER"){
                typeNum = RecordType.OTHER;
            }

            Long ttlLong = Long.valueOf(section[n].getTtl());

            if (section[n].getisInetAddress() == true) { //ResourceRecord(String hostName, RecordType type, long ttl, InetAddress result)
                InetAddress inetNumber = InetAddress.getByName(section[n].getNameServer());
                ResourceRecord record = new ResourceRecord(section[n].getName(),typeNum,ttlLong,inetNumber);
                cache.addResult(record);

            }else{ // ResourceRecord(String hostName, RecordType type, long ttl, String result)
                ResourceRecord record = new ResourceRecord(section[n].getName(),typeNum,ttlLong,section[n].getNameServer());
                cache.addResult(record);

                }
            }
            }


}
