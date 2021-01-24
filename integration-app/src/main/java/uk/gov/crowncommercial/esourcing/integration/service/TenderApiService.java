package uk.gov.crowncommercial.esourcing.integration.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uk.gov.crowncommercial.esourcing.integration.server.api.TendersApiDelegate;
import uk.gov.crowncommercial.esourcing.integration.server.model.InlineResponse201;
import uk.gov.crowncommercial.esourcing.integration.server.model.Procurement;
import uk.gov.crowncommercial.esourcing.integration.server.model.Tender;
import uk.gov.crowncommercial.esourcing.jaggaer.mocksvr.model.ProjectResponse;
import uk.gov.crowncommercial.esourcing.jaggaer.mocksvr.model.Projects;
import uk.gov.crowncommercial.esourcing.jaggaer.mocksvr.model.RfxResponse;
import uk.gov.crowncommercial.esourcing.jaggaer.mocksvr.model.Rfxs;


@Service
public class TenderApiService implements TendersApiDelegate {

  private static final Logger logger = LoggerFactory.getLogger(TenderApiService.class);

  @Value("${ccs.esourcing.tenders.jaggaerclienturl}")
  private String JAGGAER_CLIENT_URL;

  public TenderApiService() {
    
  }

  @Override
  public ResponseEntity<InlineResponse201> createTender(Procurement procurement) {

    logger.info("Creating Tender with procurement : {}", procurement);


    RestTemplate restTemplate = new RestTemplate();

    HttpEntity<Projects> projectRequest = new HttpEntity<>(new Projects());
    ResponseEntity<ProjectResponse> projectResponse = restTemplate
        .exchange(JAGGAER_CLIENT_URL + "projects", HttpMethod.POST, projectRequest, ProjectResponse.class);
    ProjectResponse projectsResponseBody = projectResponse.getBody();

    HttpEntity<Rfxs> rfxRequest = new HttpEntity<>(new Rfxs());
    ResponseEntity<RfxResponse> rfxResponse = restTemplate
        .exchange(JAGGAER_CLIENT_URL + "rfxs", HttpMethod.POST, rfxRequest, RfxResponse.class);
    RfxResponse rfxsResponseBody = rfxResponse.getBody();


    InlineResponse201 inlineResponse201 = new InlineResponse201();

    inlineResponse201.setProjectCode(
        projectsResponseBody != null ? projectsResponseBody.getTenderReferenceCode() : null);
    inlineResponse201.setIttCode(rfxsResponseBody != null ? rfxsResponseBody.getRfxId() : null);

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
