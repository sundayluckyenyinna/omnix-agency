package com.accionmfb.omnix.agency.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class ErrandPayCashoutNotification {

    @NotNull(message = "Status code cannot be null")
    @NotEmpty(message = "Status code cannot be empty")
    @NotBlank(message = "Status code cannot be blank")
    @Pattern(regexp = "[0-9]{2}", message = "Status code must be 2 digit code like 00")
    private String StatusCode;

    @NotNull(message = "Status description cannot be null")
    @NotEmpty(message = "Status description cannot be empty")
    @NotBlank(message = "Status description cannot be blank")
    private String StatusDescription;

    @NotNull(message = "Serial number cannot be null")
    @NotEmpty(message = "Serial number cannot be empty")
    @NotBlank(message = "Serial number cannot be blank")
    private String SerialNumber;

    //    @Pattern(regexp = "^([0-9]{1,3},([0-9]{3},)*[0-9]{3}|[0-9]+)(\\.[0-9][0-9])?$", message = "Transaction Amount must contain only digits, comma or dot only")
    private double Amount;

    @NotNull(message = "Reference cannot be null")
    @NotEmpty(message = "Reference cannot be empty")
    @NotBlank(message = "Reference cannot be blank")
    private String TransactionReference;

    private String Currency;

    @NotNull(message = "Transaction date cannot be null")
    @NotEmpty(message = "Transaction date cannot be empty")
    @NotBlank(message = "Transaction date cannot be blank")
    private String TransactionDate;

    @NotNull(message = "Transaction time cannot be null")
    @NotEmpty(message = "Transaction time cannot be empty")
    @NotBlank(message = "Transaction time cannot be blank")
    private String TransactionTime;

    @NotNull(message = "Transaction type cannot be null")
    @NotEmpty(message = "Transaction type cannot be empty")
    @NotBlank(message = "Transaction type cannot be blank")
    private String TransactionType;

    @NotNull(message = "Service code cannot be null")
    @NotEmpty(message = "Service code cannot be empty")
    @NotBlank(message = "Service code cannot be blank")
    private String ServiceCode;

    private double Fee;

    @NotNull(message = "Posting type cannot be null")
    @NotEmpty(message = "Posting type cannot be empty")
    @NotBlank(message = "Posting type cannot be blank")
    private String PostingType;

    private OtherDetails AdditionalDetails;
}