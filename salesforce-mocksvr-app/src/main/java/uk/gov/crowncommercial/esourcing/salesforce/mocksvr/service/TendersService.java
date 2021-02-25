package uk.gov.crowncommercial.esourcing.salesforce.mocksvr.service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import uk.gov.crowncommercial.esourcing.salesforce.mocksvr.api.TendersApiDelegate;
import uk.gov.crowncommercial.esourcing.salesforce.mocksvr.model.RfxStatus200Response;
import uk.gov.crowncommercial.esourcing.salesforce.mocksvr.model.RfxStatusItem;

@Service
public class TendersService implements TendersApiDelegate {

  @Override
  public ResponseEntity<List<RfxStatus200Response>> updateCaseRFxStatus(
      List<RfxStatusItem> rfxStatusItem) {

    List<RfxStatus200Response> rfxStatus200Responses = new ArrayList<>();

    RfxStatus200Response rfxStatus200Response = new RfxStatus200Response();
    rfxStatus200Response.setProcurementReference("CO0001");
    rfxStatus200Response.setIsSuccess("true");
    rfxStatus200Response.setErrorMessage("");
    rfxStatus200Responses.add(rfxStatus200Response);

    return new ResponseEntity<>(rfxStatus200Responses, HttpStatus.OK);
  }
}
