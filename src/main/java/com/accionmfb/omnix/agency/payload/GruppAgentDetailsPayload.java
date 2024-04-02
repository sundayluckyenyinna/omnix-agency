/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.agency.payload;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import lombok.*;

/**
 *
 * @author bokon
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GruppAgentDetailsPayload {

    @NotNull(message = "First name cannot be null")
    @NotEmpty(message = "First name cannot be empty")
    @NotBlank(message = "First name cannot be blank")
    private String firstName;
    @NotNull(message = "Last name cannot be null")
    @NotEmpty(message = "Last name cannot be empty")
    @NotBlank(message = "Last name cannot be blank")
    private String lastName;
    @NotNull(message = "Date of birth cannot be null")
    @NotEmpty(message = "Date of birth cannot be empty")
    @NotBlank(message = "Date of birth cannot be blank")
    private String dob;
    @NotNull(message = "Identifier cannot be null")
    @NotEmpty(message = "Identifier cannot be empty")
    @NotBlank(message = "Identifier cannot be blank")
    private String identifier;
    @NotNull(message = "Phone number cannot be null")
    @NotEmpty(message = "Phone number cannot be empty")
    @NotBlank(message = "Phone number cannot be blank")
    @Pattern(regexp = "[0-9]{11}", message = "Phone number must be 11 digits")
    private String phoneNumber;
    @NotNull(message = "Business name cannot be null")
    @NotEmpty(message = "Business name cannot be empty")
    @NotBlank(message = "Business name cannot be blank")
    private String businessName;
    @NotNull(message = "Business type cannot be null")
    @NotEmpty(message = "Business type cannot be empty")
    @NotBlank(message = "Business type cannot be blank")
    private String businessType;
    @NotNull(message = "L.G.A cannot be null")
    @NotEmpty(message = "L.G.A cannot be empty")
    @NotBlank(message = "L.G.A cannot be blank")
    private String lga;
    @NotNull(message = "State cannot be null")
    @NotEmpty(message = "State cannot be empty")
    @NotBlank(message = "State cannot be blank")
    private String state;
    @NotNull(message = "Address cannot be null")
    @NotEmpty(message = "Address cannot be empty")
    @NotBlank(message = "Address cannot be blank")
    private String address;
    @NotNull(message = "BVN cannot be null")
    @NotEmpty(message = "BVN cannot be empty")
    @NotBlank(message = "BVN cannot be blank")
    @Pattern(regexp = "[0-9]{11}", message = "BVN must be 11 digits only")
    private String bvn;
    @NotNull(message = "Gender cannot be null")
    @NotEmpty(message = "Gender cannot be empty")
    @NotBlank(message = "Gender cannot be blank")
    @Pattern(regexp = "^(Female|Male|MALE|FEMALE)$", message = "Value must be either Female or Male")
    private String gender;
    @Email
    private String email;
    @NotNull(message = "Hash cannot be null")
    @NotEmpty(message = "Hash cannot be empty")
    @NotBlank(message = "Hash cannot be blank")
    private String hash;
}
