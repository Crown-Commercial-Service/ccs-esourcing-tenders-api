package uk.gov.crowncommercial.esourcing.jaggaer.mocksvr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import uk.gov.crowncommercial.esourcing.jaggaer.mocksvr.api.ProjectsApiDelegate;
import uk.gov.crowncommercial.esourcing.jaggaer.mocksvr.model.ProjectResponse;
import uk.gov.crowncommercial.esourcing.jaggaer.mocksvr.model.Projects;

@Service
public class ProjectApiService implements ProjectsApiDelegate {

  private static final Logger logger = LoggerFactory.getLogger(ProjectApiService.class);

  public ProjectApiService() {

  }

  @Override
  public ResponseEntity<ProjectResponse> createProject(Projects projects) {

    logger.info("Creating Tender with project : {}", projects);

    ProjectResponse projectResponse = new ProjectResponse();

    projectResponse.setReturnCode(0);
    projectResponse.setReturnMessage("OK");
    projectResponse.setTenderCode("tender_40881");
    projectResponse.setTenderReferenceCode("project_1063");

    return new ResponseEntity<>(projectResponse, HttpStatus.CREATED);
  }

}
