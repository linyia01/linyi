 public  void test(){
//        readlbs();
//        readdata();
//        WaveHeader wh= new WaveHeader( );
//        wh.main();
        System.out.println("linyi");
        System.out.println("linyi");
        File file = new File("D://SZ002918.dat");
        try{
            FileInputStream fileStream= new FileInputStream(file);
            ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
            byte[] buf = new byte[256];
            int sise = (int)file.length();
            System.out.println(sise);
            int filesize=sise;
            if(sise > buf.length) {
                sise = buf.length;
            }
            int read = 0;
            int v4 = 0;
            byte[] b1a =new byte[1];
            byte[] b2a =new byte[2];
            byte[] b6a=new byte[6];
            byte[] b4a =new byte[4];
            fileStream.read(b2a);
            System.out.println("version "+byte2ToShort(b2a,0));
            fileStream.read(b6a);
            long datetime =(((((long)b6a[5])) & 255) << 40) + (((((long)b6a[4])) & 255) << 32) +
                    (((((long)b6a[3])) & 255) << 24) + (((((long)b6a[2])) & 255) << 16) +
                    (((((long)b6a[1])) & 255) << 8) + ((((long)b6a[0])) & 255);
            System.out.println("time is "+datetime);
            fileStream.read(b1a);
            System.out.println("isClosed "+(int)b1a[0]);
            fileStream.read(b4a);
            System.out.println("dataLength "+byte4ToInt(b4a,0));
            fileStream.read(b4a);
            System.out.println("dataLengthZip "+byte4ToInt(b4a,0));
            fileStream.read(b1a);
            System.out.println("decNum "+(int)b1a[0]);
            fileStream.read(b1a);
            System.out.println("displayDecNum "+(int)b1a[0]);
            fileStream.read(b6a);


            while(read <  filesize) {
                v4 = fileStream.read(buf, 0, sise);
                if(v4 == -1) {
                    break;
                }
                read += v4;
                sise = filesize - read>buf.length ? buf.length : filesize - read;
                byteArray.write(buf, 0, v4);
            }
            byte[] debuff= decompress(byteArray.toByteArray());
            resolve(debuff);
//           for (byte b : debuff) {
//                System.out.print ((short)b);
//                System.out.print (',');
//            }
            System.out.println("");
        }catch(Exception e){

        }

    }
    private static void  resolve(byte[] srcbyte){
        ByteArrayInputStream inS = new ByteArrayInputStream(srcbyte);
        byte []  by4c=new byte[4];
        byte []  by2c=new byte[2];
        try{
            inS.read(by4c);
            int count=byte4ToInt(by4c,0);
            for(int i=0;i<count;++i){
                inS.read(by4c);
                int time_index=byte4ToInt(by4c,0);
                short v1_1 = ((short)(((int)(time_index & 255))));
                System.out.println("time : "+(time_index>>>8)+" : "+ (time_index&255));
                for(int j=0;j<v1_1;++j){
                    inS.read(by4c);
                    int price=byte4ToInt(by4c,0);
                    inS.read(by4c);
                    int TotalOrders=byte4ToInt(by4c,0);
                    System.out.println("price totalOrders "+price+" : "+TotalOrders);
                    int flag=inS.read();
                    int [][] v9=new int[50][2];
                    for(int n = 0; n < flag >>> 2; ++n) {
                        int v5=inS.read();
                        inS.read(by2c);
                        int v11_1 = byte2ToShort(by2c,0);
                        int v12 = v5 & 63;
                        v9[v12][0] = v5 >>> 6;
                        v9[v12][1] = v11_1;
                        if((flag >>> 1 & 1) == 0) {
                            System.out.println ("  buy "+"index:"+v12+" var1:"+v9[v12][0]+" var2:"+v11_1);
                        }else{
                            System.out.println("  sell "+"index:"+v12+" var1:"+v9[v12][0]+" var2:"+v11_1);
                        }
                    }
                    System.out.println("");
                }
            }
            System.out.println("all end"+inS.available());
            while ( inS.available()>0) {
                System.out.print ((short)inS.read());
                System.out.print (',');
            }
        }catch(Exception e){

        }


    }
    public static byte[] decompress(byte[] data) {
        byte[] output = new byte[0];
        Inflater decompresser = new Inflater();
        decompresser.reset();
        decompresser.setInput(data);

        ByteArrayOutputStream o = new ByteArrayOutputStream(data.length);
        try {
            byte[] buf = new byte[1024];
            while (!decompresser.finished()) {
                int i = decompresser.inflate(buf);
                o.write(buf, 0, i);
            }
            output = o.toByteArray();
        } catch (Exception e) {
            output = data;
            e.printStackTrace();
        } finally {
            try {
                o.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        decompresser.end();
        return output;
    }
    public static int byte4ToInt(byte[] bytes, int off) {
        int b3 = bytes[off] & 0xFF;
        int b2 = bytes[off + 1] & 0xFF;
        int b1 = bytes[off + 2] & 0xFF;
        int b0 = bytes[off + 3] & 0xFF;
        return (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
    }
    public static void putInt(byte[] bb, int x, int index) {
        bb[index + 3] = (byte) (x >> 24);
        bb[index + 2] = (byte) (x >> 16);
        bb[index + 1] = (byte) (x >> 8);
        bb[index + 0] = (byte) (x >> 0);
    }
    public static short byte2ToShort(byte[] b, int index) {
        return (short) (((b[index + 1] << 8) | b[index + 0] & 0xff));
    }
