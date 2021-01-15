package uk.gov.crowncommercial.esourcing.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import uk.gov.crowncommercial.esourcing.jaggaer.api.RfxsApiDelegate;
import uk.gov.crowncommercial.esourcing.jaggaer.model.RfxResponse;
import uk.gov.crowncommercial.esourcing.jaggaer.model.Rfxs;

@Service
public class RfxApiService implements RfxsApiDelegate {

  private static final Logger logger = LoggerFactory.getLogger(RfxApiService.class);

  public RfxApiService() {

  }

  @Override
  public ResponseEntity<RfxResponse> createRFX(Rfxs rfxs) {

    logger.info("Creating Tender with project : {}", rfxs);

    RfxResponse rfxResponse = new RfxResponse();

    rfxResponse.setReturnCode(0);
    rfxResponse.setReturnMessage("OK");
    rfxResponse.setRfxId("rfq_47310");
    rfxResponse.setRfxReferenceCode("itt_534");

    return new ResponseEntity<>(rfxResponse, HttpStatus.CREATED);
  }

}
