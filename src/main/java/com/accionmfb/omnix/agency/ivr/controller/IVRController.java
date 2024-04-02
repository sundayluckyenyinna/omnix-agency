package com.accionmfb.omnix.agency.ivr.controller;

import com.accionmfb.omnix.agency.exception.AuthorizationCredentialException;
import com.accionmfb.omnix.agency.exception.IPBannedException;
import com.accionmfb.omnix.agency.exception.IncompleteParameterException;
import com.accionmfb.omnix.agency.ivr.service.IVRGenericService;
import com.accionmfb.omnix.agency.ivr.service.IVRService;
import com.accionmfb.omnix.agency.model.ivr.ThirdPartyVendors;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Olaoye on 30/10/2023
 */
@RestController
@RequestMapping("/api")
public class IVRController {

    @Autowired
    IVRGenericService ivrGenericService;
    @Autowired
    IVRService apiService;
    @Autowired
    Environment env;

    Gson gson;

    private static final Logger LOGGER = Logger.getLogger(IVRController.class.getName());
    private static final String APPLICATION_NAME = "IVR";

    IVRController() {
        gson = new Gson();
    }

    @PostMapping(value = "/ivr", consumes = "application/json", produces = "application/json")
    @ResponseBody
    public String validateRequest(@RequestBody String requestPayload, HttpServletRequest request) throws IPBannedException, AuthorizationCredentialException, IncompleteParameterException, Exception {
        String session = request.getSession().getId();
        //Check if the IP address is from You Verify
        System.out.println(request.getRemoteAddr());
        String remoteIP = request.getRemoteAddr();
        String requestSource = "";
        List<String> connectingServices = null;
        String token = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJURVNUIiwicm9sZXMiOiJbQUNDT1VOVF9CQUxBTkNFUywgQUNDT1VOVF9TVEFURU1FTlQsIEFDQ09VTlRfREVUQUlMUywgQUNDT1VOVF9PUEVOSU5HLCBBQ0NPVU5UX0dMT0JBTCwgV0FMTEVUX0FDQ09VTlRfT1BFTklORywgQUlSVElNRV9DQUxMQkFDSywgQUlSVElNRV9TRUxGLCBBSVJUSU1FX09USEVSUywgREFUQV9TRUxGLCBEQVRBX09USEVSUywgQUlSVElNRV9EQVRBX0RFVEFJTFMsIEJWTl9WQUxJREFUSU9OLCBDQUJMRV9UVl9TVUJTQ1JJUFRJT04sIENBQkxFX1RWX0RFVEFJTFMsIENBQkxFX1RWX0JJTExFUiwgQ1JFRElUX0JVUkVBVV9WQUxJREFUSU9OLCBJTkRJVklEVUFMX1dJVEhfQlZOLCBJTkRJVklEVUFMX1dJVEhPVVRfQlZOLCBDT1JQT1JBVEVfQ1VTVE9NRVIsIENVU1RPTUVSX0JPQVJESU5HLCBDVVNUT01FUl9ERVRBSUxTLCBVUERBVEVfQ1VTVE9NRVJfVElFUiwgVVBEQVRFX0NVU1RPTUVSX1NUQVRVUywgVVBEQVRFX0NVU1RPTUVSX1NUQVRFX1JFU0lERU5DRSwgVVBEQVRFX0NVU1RPTUVSX0NJVFlfUkVTSURFTkNFLCBVUERBVEVfQ1VTVE9NRVJfUkVTSURFTlRJQUxfQUREUkVTUywgVVBEQVRFX0NVU1RPTUVSX1BBU1NXT1JELCBVUERBVEVfQ1VTVE9NRVJfU0VDVVJJVFlfUVVFU1RJT04sIFVQREFURV9DVVNUT01FUl9QSU4sIFVQREFURV9DVVNUT01FUl9FTUFJTCwgVVBEQVRFX0NVU1RPTUVSX01BUklUQUxfU1RBVFVTLCBVUERBVEVfQ1VTVE9NRVJfTU9CSUxFX05VTUJFUiwgQVVUSF9DVVNUT01FUl9VU0lOR19QSU4sIEFVVEhfQ1VTVE9NRVJfVVNJTkdfUEFTU1dPUkQsIEVMRUNUUklDSVRZX0JJTExfUEFZTUVOVCwgRUxFQ1RSSUNJVFlfQklMTF9ERVRBSUxTLCBFTEVDVFJJQ0lUWV9CSUxMRVJTLCBMT0NBTF9GVU5EU19UUkFOU0ZFUiwgTE9DQUxfRlVORFNfVFJBTlNGRVJfV0lUSF9DSEFSR0UsIFJFVkVSU0VfTE9DQUxfRlVORFNfVFJBTlNGRVIsIE9QQVksIFBBWV9BVFRJVFVERSwgTklQX05BTUVfRU5RVUlSWSwgSU5URVJfQkFOS19GVU5EU19UUkFOU0ZFUiwgQUNDT1VOVF9UT19XQUxMRVRfRlVORFNfVFJBTlNGRVIsIElERU5USVRZX1ZBTElEQVRJT04sIEJPT0tfQVJUSVNBTl9MT0FOLCBQRU5ESU5HX0FSVElTQU5fTE9BTiwgRElTQlVSU0VfQVJUSVNBTl9MT0FOLCBBVVRIX0FSVElTQU5fTE9BTiwgUkVORVdfQVJUSVNBTl9MT0FOLCBCT09LX0RJR0lUQUxfTE9BTiwgRElTQlVSU0VfRElHSVRBTF9MT0FOLCBBQ0NFUFRfRElHSVRBTF9MT0FOLCBSRU5FV19ESUdJVEFMX0xPQU4sIExPQU5fU0VUVVAsIExPQU5fVFlQRV9MSVNUSU5HLCBTTVNfTk9USUZJQ0FUSU9OLCBUUkFOU0FDVElPTl9FTUFJTF9BTEVSVCwgTE9BTl9PRkZFUl9FTUFJTF9BTEVSVCwgTE9BTl9HVUFSQU5UT1JfRU1BSUxfQUxFUlQsIFdBTExFVF9CQUxBTkNFLCBXQUxMRVRfREVUQUlMUywgV0FMTEVUX0NVU1RPTUVSLCBDUkVBVEVfV0FMTEVULCBDTE9TRV9XQUxMRVQsIFdBTExFVF9BSVJUSU1FX1NFTEYsIFdBTExFVF9BSVJUSU1FX09USEVSUywgV0FMTEVUX0RBVEFfU0VMRiwgV0FMTEVUX0RBVEFfT1RIRVJTLCBXQUxMRVRfQ0FCTEVfVFZfU1VCU0NSSVBUSU9OLCBXQUxMRVRfRUxFQ1RSSUNJVFlfQklMTCwgV0FMTEVUX1RPX1dBTExFVF9GVU5EU19UUkFOU0ZFUiwgV0FMTEVUX1RPX0FDQ09VTlRfRlVORFNfVFJBTlNGRVIsIFdBTExFVF9UT19JTlRFUl9CQU5LX1RSQU5TRkVSLCBJTlRFUl9CQU5LX1RPX1dBTExFVF9GVU5EU19UUkFOU0ZFUiwgV0FMTEVUX0lOVEVSX0JBTktfTkFNRV9FTlFVSVJZLCBDT05WRVJUX1dBTExFVF9UT19BQ0NPVU5ULCBEQVRBX1BMQU4sIEFERF9QT1NUSU5HX1JFU1RSSUNUSU9OLCBDQVJEX1JFUVVFU1QsIEZVTkRTX1RSQU5TRkVSX1NUQVRVUywgTklCU1NfUVJfUEFZTUVOVCwgQVVUSF9TRUNVUklUWV9RVUVTVElPTiwgVEVMTEVSLCBVUERBVEVfQ1VTVE9NRVJfQlZOLCBGVU5EU19UUkFOU0ZFUl9ERUxFVEUsIEFVVEhfQ1VTVE9NRVJfVVNJTkdfRklOR0VSUFJJTlQsIFVQREFURV9DVVNUT01FUl9GSU5HRVJfUFJJTlQsIEFDQ09VTlRfQkFMQU5DRSwgR1JVUFAsIEdPQUxTX0FORF9JTlZFU1RNRU5ULCBBR0VOQ1lfQkFOS0lORywgUlVCWVhdIiwiYXV0aCI6Imt4dmQzRFNUeWV2U1I1N3M1SXM0T1E9PSIsIkNoYW5uZWwiOiJURVNUIiwiSVAiOiIxMC4xMC4wLjUyIiwiaXNzIjoiQWNjaW9uIE1pY3JvZmluYW5jZSBCYW5rIiwiaWF0IjoxNjk3NDY3ODkyLCJleHAiOjYyNTE0Mjg0NDAwfQ.UVLTl71ReQQVwwPHmOC_RBGls4REKVMBgwEo-s17PiM";
        String authorization = token.trim();

//        check if the IP is whitelisted
        ThirdPartyVendors vendor = ivrGenericService.getIPDetails(remoteIP.trim());
        if (vendor != null) {
            requestSource = vendor.getVendorName().concat(" - ").concat(vendor.getApplication());
            LOGGER.log(Level.INFO, "Request Source - {0}.", requestSource);
            connectingServices = Arrays.asList(vendor.getConnectingServices().split("\\,"));
            if (connectingServices != null) {
                if (!connectingServices.contains(APPLICATION_NAME)) {
                    LOGGER.log(Level.INFO, "Connecting services for IP - ".concat(remoteIP).concat(" does not contain ").concat(APPLICATION_NAME));
                    throw new IPBannedException();
                }
            } else {
                LOGGER.log(Level.INFO, "IP Address does not have a connecting service - ".concat(remoteIP));
                throw new IPBannedException();
            }
        } else {
            LOGGER.log(Level.INFO, "UNKNOWN IP - ".concat(remoteIP).concat(". IP has not been profiled"));
            throw new IPBannedException();
        }

//        Check the request header
        Boolean requestHeaderValid = apiService.checkRequestHeaderValidity(authorization);
        if (!requestHeaderValid) {
            throw new AuthorizationCredentialException();
        }

//        validate request parameter
        boolean requestTypeValid = apiService.typeValidation(requestPayload);
        if (!requestTypeValid) {
            throw new IncompleteParameterException();
        }

        String result = apiService.ivrRequest(authorization, requestPayload, requestSource, remoteIP, session);


        return result;
    }

}
