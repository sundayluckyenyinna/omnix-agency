package com.accionmfb.omnix.agency.service.impl;

import com.accionmfb.omnix.agency.payload.ExcelTransaction;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import com.opencsv.exceptions.CsvValidationException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CsvReaderService {

    public List<ExcelTransaction> readCsvFile2222(String filePath) throws IOException, CsvValidationException {
        List<ExcelTransaction> transactions = new ArrayList<>();

        try ( FileReader fileReader = new FileReader(filePath);  CSVReader csvReader = new CSVReaderBuilder(fileReader)
                .withSkipLines(1) // Skip the header row
                .build()) {

            String[] nextRecord;
            while ((nextRecord = csvReader.readNext()) != null) {
                ExcelTransaction transaction = new ExcelTransaction();

                transaction.setFirstName(nextRecord[0]);
                transaction.setLastName(nextRecord[1]);
                transaction.setBusinessName(nextRecord[2]);
                transaction.setPhoneNumber(nextRecord[3]);
                transaction.setUserType(nextRecord[4]);
                transaction.setReference(nextRecord[5]);
                transaction.setUniqueId(nextRecord[6]);
                transaction.setAmount(nextRecord[7]);
                transaction.setServiceFee(nextRecord[8]);
                transaction.setBankServiceFee(nextRecord[9]);
                transaction.setCustomerServiceFee(nextRecord[10]);
                transaction.setBillerId(nextRecord[11]);
                transaction.setTransactionType(nextRecord[12]);
                transaction.setStatus(nextRecord[13]);
                transaction.setRrn(nextRecord[14]);
                transaction.setStan(nextRecord[15]);
                transaction.setMaskedPan(nextRecord[16]);
                transaction.setCardDescription(nextRecord[17]);
                transaction.setCardType(nextRecord[18]);
                transaction.setTerminalId(nextRecord[19]);
                transaction.setDate(nextRecord[20]);

                transactions.add(transaction);
            }
        }
        return transactions;
    }

    public List<ExcelTransaction> readCsvFile(File csvFile) throws IOException, CsvException {
        try ( CSVReader csvReader = new CSVReader(new FileReader(csvFile))) {
            List<String[]> csvData = csvReader.readAll();
            return mapCsvDataToPojoList(csvData);
        }
    }

    private List<ExcelTransaction> mapCsvDataToPojoList(List<String[]> csvData) {
        List<ExcelTransaction> pojoList = new ArrayList<>();

        for (int i = 1; i < csvData.size(); i++) {
            String[] rowData = csvData.get(i);

            ExcelTransaction excelTransaction = new ExcelTransaction();

            // Assuming the order of columns in the CSV matches the order of fields in the POJO
            excelTransaction.setFirstName(rowData[0]);
            excelTransaction.setLastName(rowData[1]);
            excelTransaction.setBusinessName(rowData[2]);
            excelTransaction.setPhoneNumber(rowData[3]);
            excelTransaction.setUserType(rowData[4]);
            excelTransaction.setReference(rowData[5]);
            excelTransaction.setUniqueId(rowData[6]);
            excelTransaction.setAmount(rowData[7]);
            excelTransaction.setServiceFee(rowData[8]);
            excelTransaction.setBankServiceFee(rowData[9]);
            excelTransaction.setCustomerServiceFee(rowData[10]);
            excelTransaction.setBillerId(rowData[11]);
            excelTransaction.setTransactionType(rowData[12]);
            excelTransaction.setStatus(rowData[13]);
            excelTransaction.setRrn(rowData[14]);
            excelTransaction.setStan(rowData[15]);
            excelTransaction.setMaskedPan(rowData[16]);
            excelTransaction.setCardDescription(rowData[17]);
            excelTransaction.setCardType(rowData[18]);
            excelTransaction.setTerminalId(rowData[19]);
            excelTransaction.setDate(rowData[20]);

            pojoList.add(excelTransaction);
        }

        return pojoList;
    }

    public void moveFileToDestination(File sourceFile) throws IOException, InterruptedException {
        // Base directory where processed settlements will be stored
//        final String BASE_DESTINATION_DIRECTORY = "C:\\Users\\cedozie\\Documents\\accion\\settlements";
        final String BASE_DESTINATION_DIRECTORY = "C:\\Users\\Public\\agencyReconciliation";

        // Name for the subdirectory (you can customize this)
        String subdirectoryName = "ProcessedSettlements";

        File baseDestinationDirectory = new File(BASE_DESTINATION_DIRECTORY);

        if (!baseDestinationDirectory.exists()) {
            // Create the base destination directory if it doesn't exist
            baseDestinationDirectory.mkdirs();
        }

        // Create a subdirectory based on the provided name
        File subdirectory = new File(baseDestinationDirectory, subdirectoryName);

        if (!subdirectory.exists()) {
            // Create the subdirectory if it doesn't exist
            subdirectory.mkdirs();
        }

        // Build the destination file path
        Path destinationFilePath = Paths.get(subdirectory.getAbsolutePath(), sourceFile.getName());

        // Move the file to the destination directory
        Thread.sleep(3000);
        Files.move(sourceFile.toPath(), destinationFilePath, StandardCopyOption.REPLACE_EXISTING);

        System.out.println("Moved file to destination: " + destinationFilePath);
    }

    public boolean isCsvFile(File file) {
        return file.isFile() && file.getName().toLowerCase().endsWith(".csv");
    }

    public boolean isTransactionDateNotLessThanYesterday(ExcelTransaction transaction) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM, yyyy hh:mm a", Locale.ENGLISH);

            // Get yesterday's date
            Calendar yesterdayCal = Calendar.getInstance();
            yesterdayCal.add(Calendar.DAY_OF_MONTH, -1);
            Date yesterday = yesterdayCal.getTime();
            log.info(transaction.getDate());

            // Parse the transaction date from the provided string
            Date transactionDate = dateFormat.parse(transaction.getDate());

            // Compare the transaction date with yesterday's date
            return !transactionDate.before(yesterday);
        } catch (ParseException e) {
// Handle parsing exception based on your requirements
            return false;
        }
    }
}
