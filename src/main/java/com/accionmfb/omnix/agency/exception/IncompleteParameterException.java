package com.accionmfb.omnix.agency.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Olaoye on 31/10/2023
 */
@ResponseStatus(value = HttpStatus.UNPROCESSABLE_ENTITY, reason = "Required parameter not supplied")
public class IncompleteParameterException extends Exception{

  public IncompleteParameterException() {
  }

}
