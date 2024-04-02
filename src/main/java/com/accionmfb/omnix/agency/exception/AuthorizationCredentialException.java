package com.accionmfb.omnix.agency.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Olaoye on 31/10/2023
 */
@ResponseStatus(value = HttpStatus.UNAUTHORIZED, reason = "Invalid user credentials")
public class AuthorizationCredentialException extends Exception{

  public AuthorizationCredentialException() {
  }

}