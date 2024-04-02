package com.accionmfb.omnix.agency.module.agency3Line.converters;

import javax.ws.rs.BadRequestException;
import javax.xml.bind.JAXB;
import java.io.InputStream;
import java.io.OutputStream;


public class XmlConverter {

    public static <T> void toXml(OutputStream os, T obj) {
        try {
            JAXB.marshal(obj, os);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            throw new BadRequestException("Request message is not valid");
        }

    }

    public static <T> T toObj(InputStream is, Class<T> objClass) {
        try {
            return JAXB.unmarshal(is, objClass);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            throw new BadRequestException("Request message is not valid");
        }
    }
    

   
    
    public static void main(String[] a)
    {
//        drawDown d= new drawDown();
//        List<transfer> trf= new ArrayList();
//        
//        transfers ab= new transfers();
//        
//        transfer t= new transfer();
//        t.setAccountNumber("123");
//        t.setAmount(BigDecimal.ONE);
//        t.setName("Main");
//        trf.add(t);
//        
//         t= new transfer();
//        t.setAccountNumber("123");
//        t.setAmount(BigDecimal.ONE);
//        t.setName("Main");
//        trf.add(t);
//        
//        ab.setTrf(trf.toArray(new transfer[trf.size()]));
//        
//        d.setTransfers(ab);
//        
//        System.out.println(toXmlMtnUssd(d));
        
//        String xml="<?xml version=\"1.0\" ?><DATA><ROW><PostDate>OPENING BALANCE</PostDate><ValueDate></ValueDate><Reference></Reference><Description></Description><Debit></Debit><Credit></Credit><Balance>244139.45</Balance><AccountNumber></AccountNumber><ChequeNo></ChequeNo></ROW><ROW><PostDate>24 OCT 2017</PostDate><ValueDate>24 OCT 2017</ValueDate><Reference>MM1729710003\\B40</Reference><Description>New Deposit: AWE ODUNAYO F/D 60D</Description><Debit>-200000.00</Debit><Credit></Credit><Balance>44139.45</Balance><AccountNumber>1006612312</AccountNumber><ChequeNo></ChequeNo></ROW><ROW><PostDate>24 OCT 2017</PostDate><ValueDate>24 OCT 2017</ValueDate><Reference>BK17297786830519\\BNK</Reference><Description>: BP-150885555850722-1 OCT 2017 DEPENDANT TAX BP-150885555850722-1</Description><Debit>-4900.00</Debit><Credit></Credit><Balance>39239.45</Balance><AccountNumber>1006612312</AccountNumber><ChequeNo></ChequeNo></ROW><ROW><PostDate>CLOSING BALANCE</PostDate><ValueDate></ValueDate><Reference></Reference><Description></Description><Debit></Debit><Credit></Credit><Balance>39239.45</Balance><AccountNumber></AccountNumber><ChequeNo></ChequeNo></ROW></DATA>";
        
//        StatementResponseDto1 resObj= toObjMtnUssd(xml, StatementResponseDto1.class);
       // System.out.println(resObj.getData().getRow()[0].getAccountNumber());
//        System.out.println(resObj.getRow().get(0).getAccountNumber());
        
    }
}
