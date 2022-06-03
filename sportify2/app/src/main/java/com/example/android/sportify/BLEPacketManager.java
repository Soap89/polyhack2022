package com.example.android.sportify;

import android.os.Build;
import androidx.annotation.RequiresApi;
import android.util.Log;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by hoyikhim on 17/06/16.
 */
public class BLEPacketManager {
    public int deviceID;
    public int deviceTypeValue;
    public float threshold;
    public float coordinateX;
    public float coordinateY;
    public final int STUDENT_ID_LENGTH = 9;
    public final int HASH_LENGTH = 20;
    public final int BUFFER_SIZE = HASH_LENGTH > 24 ? HASH_LENGTH : 24;
    public final int TRUNCATE_TO = HASH_LENGTH;

    public byte[] sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(input.getBytes());
        } catch (NoSuchAlgorithmException e) {
            System.out.println("sha256(): " + e);
            return null;
        }
    }

    public byte[] sha1(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            return md.digest(input.getBytes());
        } catch (NoSuchAlgorithmException e) {
            System.out.println("sha1(): " + e);
            return null;
        }
    }

    public byte[] stringToByte(String value) {
        byte[] b = value.getBytes(StandardCharsets.UTF_8);
        return b;
    }

    public byte intToByte(int value)
    {
        if (value < 128) {
            return (byte)value;
        }
        else
        {
            return 0;
        }
    }

    public byte[] floatToByte(float value)
    {
        if ((int)value > 127 || (int)value < -127) {
            value = -99;
        }
        int decimal = (int)(value * 100) - (int)value * 100;
        if (decimal > 99)
        {
            decimal = 0;
        }
        Byte firstByte =(byte)value;
        Byte secondByte = (byte)decimal;
        ByteBuffer b = ByteBuffer.allocate(2);
        b.put(firstByte);
        b.put(secondByte);
        return b.array();
    }

    public byte[][] byteListToByte (List<byte[]> byteList){
        int length = byteList.size();
        byte[] byteArray[] = new byte [16][3];
        for (int i = 0 ; i < 16; i ++)
        {
            if (i < length) {
                byteArray[i] = (byteList.get(i));
            }
            else
            {
                byteArray[i] = null;
            }
        }
        return byteArray;
    }

    public byte[] doubleListToByte(List<Double> doubleList){
        int length = doubleList.size();
        byte byteArray[] = new byte[3];
        for (int i = 0 ; i < 3; i ++)
        {
            byteArray[i] = (doubleList.get(i).byteValue());
        }
        return byteArray;
    }

    public byte[] integerListToByte(List<Integer> integerList){
        int length = integerList.size();
        byte byteArray[] = new byte[length+1];
        byteArray[0] = intToByte(length);
        for (int i = 0 ; i < length; i ++)
        {
            byteArray[i+1] = intToByte(integerList.get(i));
        }
        return byteArray;
    }

    public List<Integer> byteToIntegerList(byte[] byteArray){
        List<Integer> integerList = new ArrayList<>();
        int length = byteToInt(byteArray[0]);
        for (int i = 1; i < byteArray.length; i ++){
            integerList.add(byteToInt(byteArray[i]));
        }
        return integerList;
    }

    public byte[] listToByte(List<Integer> integerList)
    {
        int length = integerList.size();
        byte byteArray[] = new byte[16];
        for (int i = 0 ; i < 16; i ++)
        {
            if (i < length) {
                byteArray[i] = intToByte(integerList.get(i));
            }
            else
            {
                byteArray[i] = intToByte(-99);
            }
        }
        return byteArray;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public byte[] secretToByte(String text) {
        //int flags = Base64.NO_WRAP | Base64.URL_SAFE;
        //byte[] res = stringToByte(Base64.encodeToString(sha1(text), flags));
        byte[] res = sha1(text);
        Log.e("DIGEST INFO: ", "Digest length: " + res.length + "; digest msg: " + byteToString(res));
        /*if(res.length > TRUNCATE_TO) {
            res = Arrays.copyOfRange(res, 0, TRUNCATE_TO);
            Log.e("Base64 length: ", "" + res.length);
            return res;
        }*/
        //Log.e("Base64 length: ", "" + res.length);
        return res;
        //return stringToByte(Base64.encodeToString(sha256(text), flags));
        //return stringToByte(Base64.getEncoder().encodeToString(sha256(text)));
    }

    /* WITH STUDENT ID */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public byte[] advertisingPacketBuilder (String studentID) {
        byte[] studentID_byte = secretToByte(studentID);
        ByteBuffer b = ByteBuffer.allocate(BUFFER_SIZE); // used to be 24
        b.put(studentID_byte);
        return b.array();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public byte[] advertisingPacketBuilder (String studentID, int deviceID, int deviceType, float coordinateX, float coordinateY, HashMap<Integer,int[]> anchorDistanceReferenceHashMap) {
        // student ID is turned to hash
        byte[] studentID_byte = secretToByte(studentID);
        //if(studentID_byte.length > 24) {
            //studentID_byte = Arrays.copyOfRange(studentID_byte, 0, 23);
        //}

        byte deviceID_byte = intToByte(deviceID);

        byte deviceType_byte = intToByte(deviceType);

        byte[] coordinateX_byte = floatToByte(coordinateX);

        byte[] coordinateY_byte = floatToByte(coordinateY);

        byte[] map_byte = anchorReferenceMapToByteArray(anchorDistanceReferenceHashMap);

        ByteBuffer b = ByteBuffer.allocate(BUFFER_SIZE); // used to be 24
        b.put(studentID_byte);
        b.put(deviceID_byte);
        b.put(deviceType_byte);
        b.put(coordinateX_byte);
        b.put(coordinateY_byte);
        b.put(map_byte);

        return b.array();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public byte[] advertisingPacketBuilder (String studentID, int deviceID, int deviceType, float coordinateX, float coordinateY, Float threshold, List<Integer> unreliableList) {
        // always put student id at the beginning
        byte[] studentID_byte = secretToByte(studentID);

        byte deviceID_byte = intToByte(deviceID);

        byte deviceType_byte = intToByte(deviceType);

        byte[] coordinateX_byte = floatToByte(coordinateX);

        byte [] coordinateY_byte = floatToByte(coordinateY);

        byte [] threshold_byte = floatToByte(threshold);
        byte [] unreliableList_byte = null;
        if (unreliableList != null)
        {
            unreliableList_byte = listToByte(unreliableList);
        }

        ByteBuffer b = ByteBuffer.allocate(24);
        b.put(studentID_byte);
        b.put(deviceID_byte);
        b.put(deviceType_byte);
        b.put(coordinateX_byte);
        b.put(coordinateY_byte);
        b.put(threshold_byte);
        b.put(unreliableList_byte);

        return b.array();
    }
    /* END WITH STUDENT ID */

    public byte[] advertisingPacketBuilder (int deviceID, int deviceType, float coordinateX, float coordinateY, Float threshold, List<Integer> unreliableList)
    {
        byte deviceID_byte = intToByte(deviceID);

        byte deviceType_byte = intToByte(deviceType);

        byte[] coordinateX_byte = floatToByte(coordinateX);

        byte [] coordinateY_byte = floatToByte(coordinateY);

        byte [] threshold_byte = floatToByte(threshold);
        byte [] unreliableList_byte = null;
        if (unreliableList != null)
        {
            unreliableList_byte = listToByte(unreliableList);
        }

        ByteBuffer b = ByteBuffer.allocate(24);
        b.put(deviceID_byte);
        b.put(deviceType_byte);
        b.put(coordinateX_byte);
        b.put(coordinateY_byte);
        b.put(threshold_byte);
        b.put(unreliableList_byte);

        return b.array();
    }

    public byte[] advertisingPacketBuilder (int deviceID, int deviceType, float coordinateX, float coordinateY, HashMap<Integer,int[]> anchorDistanceReferenceHashMap) {
        byte deviceID_byte = intToByte(deviceID);

        byte deviceType_byte = intToByte(deviceType);

        byte[] coordinateX_byte = floatToByte(coordinateX);

        byte[] coordinateY_byte = floatToByte(coordinateY);

        byte[] map_byte = anchorReferenceMapToByteArray(anchorDistanceReferenceHashMap);


        ByteBuffer b = ByteBuffer.allocate(24);
        b.put(deviceID_byte);
        b.put(deviceType_byte);
        b.put(coordinateX_byte);
        b.put(coordinateY_byte);
        b.put(map_byte);

        return b.array();
    }

    public String byteToString(byte[] b) {
        Log.e("SCAN RESULT: ", String.format("%032x", new BigInteger(1, b)));
        return String.format("%032x", new BigInteger(1, b)); // a 40-char hex string
        //return new String(b, StandardCharsets.UTF_8);
    }

    public int byteToInt(byte byteValue)
    {
        //http://stackoverflow.com/questions/2529756/assigning-int-to-byte-in-java
        int intValue = byteValue;
        return intValue;
    }

    public float byteToFloat(byte[] byteValue)
    {
        float firstIntValue = byteValue[0];
        float secondIntValue = byteValue[1];
        return firstIntValue+secondIntValue/100;
    }


    public List<Integer> byteToList (byte[] value)
    {
        List<Integer> unreliableList = new ArrayList<>();
        int length = value.length;
            if (length > 0) {
                for (int i=0; i<length; i++)
                {
                    if (byteToInt(value[i]) != -99) {
                        unreliableList.add(byteToInt(value[i]));
                    }
                }
        }
        return unreliableList;
    }

    public String advertisingPacketDecoder (byte[] packet){
        if (packet.length < 16) {
            return null;
        }

        String data = byteToString(Arrays.copyOfRange(packet, 0, HASH_LENGTH));

        return data;
    }

    public byte[] anchorReferenceMapToByteArray (HashMap<Integer,int[]> anchorDistanceReferenceHashMap) {
        return integerListToByte(anchorReferenceMapToIntegerList(anchorDistanceReferenceHashMap));
    }

    public HashMap<Integer,int[]> byteArrayToAnchorReferenceMap (byte[] byteArray){
        return integerListToAnchorReferenceMap(byteToIntegerList(byteArray));
    }

    private List<Integer> anchorReferenceMapToIntegerList (HashMap<Integer,int[]> anchorDistanceReferenceHashMap) {
        ArrayList<Integer> integerList = new ArrayList<>();
        Iterator it = anchorDistanceReferenceHashMap.entrySet().iterator();
        int i = 0;
        while (it.hasNext()) {
            Map.Entry<Integer,int[]> pair = (Map.Entry)it.next();
            integerList.add(pair.getKey());
            integerList.add(pair.getValue()[0]);
            integerList.add(pair.getValue()[1]);
        }
        return integerList;
    }

    private HashMap<Integer,int[]> integerListToAnchorReferenceMap (List<Integer> integerList) {
        HashMap<Integer,int[]> anchorReferenceMap = new HashMap<>();
        int i = 0;
        while (i < integerList.size()){
            int[] intArray = new int[2];
            intArray[0] = integerList.get(i+1);
            intArray[1] = integerList.get(i+2);
            anchorReferenceMap.put(integerList.get(i),intArray);
            i = i + 3;
        }
        Iterator it = anchorReferenceMap.entrySet().iterator();
        while (it.hasNext()){
            Map.Entry<Integer,int[]> pair = (Map.Entry)it.next();
        }

        return anchorReferenceMap;
    }


}
