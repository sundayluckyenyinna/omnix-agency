package com.accionmfb.omnix.agency.module.agency3Line.converters;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.ws.rs.BadRequestException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JsonConverter {

    public static <T> void toJson(OutputStream os, T obj) {
        ObjectMapper mapper = new ObjectMapper();
        //mapper.setSerializationInclusion(Include.NON_NULL);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            mapper.writeValue(os, obj);
        } catch (IOException ex) {
            Logger.getLogger(JsonConverter.class.getName()).log(Level.SEVERE, null, ex);
            throw new BadRequestException("Failed to convert object to json");

        }
    }

    ///
    public static <T> T toObj(InputStream is, Class<T> objClass) {
        T obj = null;
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            obj = mapper.readValue(is, objClass);
//            System.out.println(mapper.writeValueAsString(obj));
        } catch (IOException ex) {
            Logger.getLogger(JsonConverter.class.getName()).log(Level.SEVERE, null, ex);
            throw new BadRequestException("Request message is not valid");
        }
        return obj;
    }
    
    public static <T> String toJson(T obj) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String res =mapper.writeValueAsString(obj);
            System.out.println(mapper.writeValueAsString(obj));
            return res;
        } catch (JsonProcessingException ex) {
            Logger.getLogger(JsonConverter.class.getName()).log(Level.SEVERE, null, ex);
        }
        return "";
    }

    public static <T> T toObj(String is, Class<T> objClass) {
        T obj = null;
        try {
            ObjectMapper mapper = new ObjectMapper();
            //mapper.setSerializationInclusion(Include.NON_NULL);
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            obj = mapper.readValue(is, objClass);
        } catch (IOException ex) {
            Logger.getLogger(JsonConverter.class.getName()).log(Level.SEVERE, null, ex);
            throw new BadRequestException("Request message is not valid");
        }
        return obj;
    }

    public static void main(String[] a) {
        String json = "{\n"
                + "\"productCode\":\"263\",\n"
                + "\"branchCode\":\"NG0010001\",\n"
                + "\"mnemonic\":\"AWT1B699\",\n"
                + "\"firstName\":\"Application\",\n"
                + "\"middleName\":\"Programming\",\n"
                + "\"lastName\":\"Interface\",\n"
                + "\"placeOfBirth\":\"Lagos\",\n"
                + "\"dob\":\"1990-12-12\",\n"
                + "\"bvn\":\"123456789\",\n"
                + "\"passport\":\"\",\n"
                + "\"residentialAddress\":\"VI Lagos\",\n"
                + "\"town\":\"Victoria Island\",\n"
                + "\"state\":\"15\",\n"
                + "\"countryOfResidence\":\"NG\",\n"
                + "\"mobileNumber\":\"08030000000\",\n"
                + "\"email\":\"aweodunayo@gmail.com\",\n"
                + "\"gender\":\"M\",\n"
                + "\"maritalStatus\":\"M\",\n"
                + "\"processingMode\":\"ONLINE\"\n"
                + "}";

    }

}
