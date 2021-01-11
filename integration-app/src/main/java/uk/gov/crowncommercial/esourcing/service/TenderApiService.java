package uk.gov.crowncommercial.esourcing.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import uk.gov.crowncommercial.esourcing.api.TendersApiDelegate;
import uk.gov.crowncommercial.esourcing.model.InlineResponse201;
import uk.gov.crowncommercial.esourcing.model.Procurement;
import uk.gov.crowncommercial.esourcing.model.Tender;

@Service
public class TenderApiService implements TendersApiDelegate {

  private static final Logger logger = LoggerFactory.getLogger(TenderApiService.class);
  
  public TenderApiService() {
    
  }

  @Override
  public ResponseEntity<InlineResponse201> createTender(Procurement procurement) {

    logger.info("Creating Tender with procurement : {}", procurement);

    InlineResponse201 inlineResponse201 = new InlineResponse201();

    inlineResponse201.setProjectCode("TESTPROJECT99");
    inlineResponse201.setIttCode("TESTITT100");

    return new ResponseEntity<>(inlineResponse201, HttpStatus.CREATED);
  }

  @Override
  public ResponseEntity<Tender> getTenderById(Long id) {

    logger.info("Getting Tenders with ID : {}", id);

    Tender tender = new Tender();
    tender.setId(id);
    tender.setDescription("Tender - Laptops for Schools");
    tender.setStatus(0);

    return ResponseEntity.ok(tender);
  }
}
