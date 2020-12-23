package uk.gov.crowncommercial.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import uk.gov.crowncommercial.api.TendersApiDelegate;
import uk.gov.crowncommercial.model.InlineResponse201;
import uk.gov.crowncommercial.model.Procurement;

@Service
public class TenderApiService implements TendersApiDelegate {

  private static Logger logger = LoggerFactory.getLogger(TenderApiService.class);

  @Override
  public ResponseEntity<InlineResponse201> createTender(Procurement procurement) {

    logger.info("Creating Tender with procurement : {}", procurement.toString());

    InlineResponse201 inlineResponse201 = new InlineResponse201();

    inlineResponse201.setProjectCode("TESTPROJECT99");
    inlineResponse201.setIttCode("TESTITT100");

    return new ResponseEntity<>(inlineResponse201, HttpStatus.CREATED);
  }

}
