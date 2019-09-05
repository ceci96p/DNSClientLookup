package ca.ubc.cs.cs317.dnslookup;

public class RRecord {
        private String nameLookedUp;
        private String name;
        private String type;
        private int ttl;
        private int dataLength;
        private String nameServer;
        private boolean isInetAddress = true;

        public void setNameLookedUp(String nameLookedUp){
            this.nameLookedUp = nameLookedUp;
        }

        public void setType(int type){
            switch (type){
                case 1:
                    this.type = "A";
                    break;
                case 2:
                    this.type = "NS";
                    break;
                case 5:
                    this.type = "CNAME";
                    break;
                case 6:
                    this.type = "SOA";
                    break;
                case 15:
                    this.type = "MX";
                    break;
                case 28:
                    this.type = "AAAA";
                    break;
                    default:
                    this.type = "OTHER";
            }
        }

        public void setTtl(int ttl){
            this.ttl = ttl;
        }

        public void setDataLength(int dataLength){
            this.dataLength = dataLength;
        }

        public void setisInetAddressFalse(){
            this.isInetAddress = false;
        }

        public void setNameServer(String nameServer){
            this.nameServer = nameServer;
        }

        public void setName(String name){this.name = name;}

        public void printInfo(){
            System.out.format("       %-30s %-10d %-4s %s\n", getName(), getTtl(), getType(), getNameServer());
        }

        public String getNameLookedUp(){
            return nameLookedUp;
        }

        public String getType(){
            return type;
        }

        public String getName(){return name;}

        public boolean getisInetAddress() {
        return isInetAddress;
    }

        public int getTtl(){
            return ttl;
        }

        public String getNameServer(){
            return nameServer;
        }
    }



