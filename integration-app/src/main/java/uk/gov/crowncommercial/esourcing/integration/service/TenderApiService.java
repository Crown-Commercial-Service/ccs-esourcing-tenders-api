package uk.gov.crowncommercial.esourcing.integration.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;
import uk.gov.crowncommercial.esourcing.integration.exception.SalesforceUpdateException;
import uk.gov.crowncommercial.esourcing.integration.server.api.TendersApiDelegate;
import uk.gov.crowncommercial.esourcing.integration.server.model.ProjectTender;
import uk.gov.crowncommercial.esourcing.integration.server.model.ProjectTender200Response;
import uk.gov.crowncommercial.esourcing.integration.server.model.RfxStatus200Response;
import uk.gov.crowncommercial.esourcing.integration.server.model.RfxStatusItem;
import uk.gov.crowncommercial.esourcing.jaggaer.client.ProjectsApi;
import uk.gov.crowncommercial.esourcing.jaggaer.client.RfxApi;
import uk.gov.crowncommercial.esourcing.jaggaer.client.RfxWorkflowsApi;
import uk.gov.crowncommercial.esourcing.jaggaer.client.model.AdditionalInfoItem;
import uk.gov.crowncommercial.esourcing.jaggaer.client.model.AdditionalInfoList;
import uk.gov.crowncommercial.esourcing.jaggaer.client.model.BuyerCompany;
import uk.gov.crowncommercial.esourcing.jaggaer.client.model.OperatorUser;
import uk.gov.crowncommercial.esourcing.jaggaer.client.model.OwnerUser;
import uk.gov.crowncommercial.esourcing.jaggaer.client.model.Project;
import uk.gov.crowncommercial.esourcing.jaggaer.client.model.ProjectOwner;
import uk.gov.crowncommercial.esourcing.jaggaer.client.model.ProjectResponse;
import uk.gov.crowncommercial.esourcing.jaggaer.client.model.Projects;
import uk.gov.crowncommercial.esourcing.jaggaer.client.model.Rfx;
import uk.gov.crowncommercial.esourcing.jaggaer.client.model.RfxAdditionalInfoItem;
import uk.gov.crowncommercial.esourcing.jaggaer.client.model.RfxAdditionalInfoList;
import uk.gov.crowncommercial.esourcing.jaggaer.client.model.RfxGetResponse;
import uk.gov.crowncommercial.esourcing.jaggaer.client.model.RfxInvalidate;
import uk.gov.crowncommercial.esourcing.jaggaer.client.model.RfxInvalidationResponse;
import uk.gov.crowncommercial.esourcing.jaggaer.client.model.RfxResponse;
import uk.gov.crowncommercial.esourcing.jaggaer.client.model.RfxSetting;
import uk.gov.crowncommercial.esourcing.jaggaer.client.model.RfxValueItem;
import uk.gov.crowncommercial.esourcing.jaggaer.client.model.RfxValues;
import uk.gov.crowncommercial.esourcing.jaggaer.client.model.Rfxs;
import uk.gov.crowncommercial.esourcing.jaggaer.client.model.Tender;
import uk.gov.crowncommercial.esourcing.jaggaer.client.model.ValueItem;
import uk.gov.crowncommercial.esourcing.jaggaer.client.model.Values;
import uk.gov.crowncommercial.esourcing.salesforce.client.RfxStatusListApi;
import uk.gov.crowncommercial.esourcing.salesforce.client.model.RfxStatus200ResponseList;
import uk.gov.crowncommercial.esourcing.salesforce.client.model.RfxStatusList;

@Service
public class TenderApiService implements TendersApiDelegate {

  private final ProjectsApi projectsApi;
  private final RfxApi rfxApi;
  private final RfxStatusListApi rfxStatusListApi;
  private final RfxWorkflowsApi rfxWorkflowsApi;

  private static final Logger logger = LoggerFactory.getLogger(TenderApiService.class);

  @Value("${ccs.esourcing.default.api.timeout}000")
  private Long defaultApiTimeout;

  @Value("${ccs.esourcing.jaggaer.default.buyer-company-id}")
  private Integer defaultBuyerCompanyId;

  @Value("${ccs.esourcing.jaggaer.default.project-type}")
  private String defaultProjectType;

  @Value("${ccs.esourcing.jaggaer.default.open-market-template-id}")
  private String openMarketTemplateId;

  @Value("${ccs.esourcing.jaggaer.default.sta-template-id}")
  private String staTemplateId;

  @Value("${ccs.esourcing.salesforce.retry.maxAttempts:3}")
  private Long salesforceMaxAttempts;

  @Value("${ccs.esourcing.salesforce.retry.waitDuration:10}")
  private Long salesforcWwaitDuration;

  public TenderApiService(
      ProjectsApi projectsApi,
      RfxApi rfxApi,
      RfxStatusListApi rfxStatusListApi,
      RfxWorkflowsApi rfxWorkflowsApi) {
    this.projectsApi = projectsApi;
    this.rfxApi = rfxApi;
    this.rfxStatusListApi = rfxStatusListApi;
    this.rfxWorkflowsApi = rfxWorkflowsApi;
  }

  @Override
  public ResponseEntity<ProjectTender200Response> createCase(ProjectTender projectTender) {
    String tenderReferenceCode = projectTender.getTenderReferenceCode();

    if (tenderReferenceCode == null) {
      logger.info("Creating Tender with procurement : {}", projectTender);
      Projects projectsRequestBody = getProjectBody(projectTender);
      logger.info("Sending Project request to Jaggaer, request body : {}", projectsRequestBody);
      ProjectResponse projectsResponseBody =
          projectsApi
              .createProject(projectsRequestBody)
              .block(Duration.ofSeconds(defaultApiTimeout));
      logger.info("Create Project response : {}", projectsResponseBody);
      tenderReferenceCode =
          projectsResponseBody != null ? projectsResponseBody.getTenderReferenceCode() : null;
    }

    Rfxs rfxRequestBody = getRfxsBody(tenderReferenceCode, projectTender.getProjectOwnerLogin(), projectTender.getRfx());
    logger.info("Sending RFX request to Jaggaer, request body : {}", rfxRequestBody);
    RfxResponse rfxsResponseBody =
        rfxApi.createRFX(rfxRequestBody).block(Duration.ofSeconds(defaultApiTimeout));
    logger.info("Create ITT Event response : {}", rfxsResponseBody);

    final String ittRef = rfxsResponseBody != null ? rfxsResponseBody.getRfxReferenceCode() : null;
    ProjectTender200Response response = new ProjectTender200Response();
    response.setTenderReferenceCode(tenderReferenceCode);
    response.setRfxReferenceCode(ittRef);

    return new ResponseEntity<>(response, HttpStatus.ACCEPTED);
  }

  @Override
  public ResponseEntity<String> createRFx (String procID, Object body){
    // TODO Implement API endpoint once we have a stable openapi spec that resolves
    return new ResponseEntity<>("", HttpStatus.OK);
  }

  @Override
  public ResponseEntity<List<RfxStatus200Response>> updateCaseRFxStatus(
      List<RfxStatusItem> rfxStatusItemList) {
    List<RfxStatus200Response> rfxStatus200Responses = new ArrayList<>();
    logger.info(
        "Sending Status Update request to Salesforce, request body : {}", rfxStatusItemList);
    RfxStatusList rfxStatusRequestBody = getRfxStatusBody(rfxStatusItemList);
    logger.info(
        "Sending Update RFX Status request to Salesforce, request body : {}", rfxStatusRequestBody);
    Map<String, String> errorMap =
        rfxStatusItemList.stream()
            .collect(
                Collectors.toMap(
                    RfxStatusItem::getProcurementReference, RfxStatusItem::getOwnerUserLogin));

    RfxStatus200ResponseList sfRfxStatus200ResponseList;

    try {
      sfRfxStatus200ResponseList =
          rfxStatusListApi
              .updateCaseRFxStatus(rfxStatusRequestBody)
              .retryWhen(
                  Retry.fixedDelay(
                          salesforceMaxAttempts, Duration.ofSeconds(salesforcWwaitDuration))
                      .filter(this::is5xxServerError)
                      .onRetryExhaustedThrow(
                          (retryBackoffSpec, retrySignal) ->
                              new SalesforceUpdateException(errorMap)))
              .block(Duration.ofSeconds(defaultApiTimeout));
    } catch (WebClientResponseException we) {
      throw new SalesforceUpdateException(errorMap);
    }

    logger.info("Update RFX Status response : {}", sfRfxStatus200ResponseList);

    if (sfRfxStatus200ResponseList != null) {
      rfxStatus200Responses = convertRfxStatusResponse(sfRfxStatus200ResponseList);
      return new ResponseEntity<>(rfxStatus200Responses, HttpStatus.OK);
    }

    return new ResponseEntity<>(rfxStatus200Responses, HttpStatus.OK);
  }

  @Override
  public ResponseEntity<String> terminateRFx(String procID, String eventID) {

    logger.info("Invalidating ITT  - ProcID : {} / EventID : {}", procID, eventID);

    RfxGetResponse rfx =
        rfxApi
            .getRFX(String.format("rfxReferenceCode==%s", eventID))
            .block(Duration.ofSeconds(defaultApiTimeout));

    final RfxSetting rfxSetting;
    if (rfx != null) {
      if (rfx.getDataList() != null) {
        if (rfx.getDataList().getRfx() != null) {
          rfxSetting =
              rfx.getDataList() != null ? rfx.getDataList().getRfx().get(0).getRfxSetting() : null;
        } else {
          throw new IllegalStateException(
              String.format("No RFx details found for refReferenceCode - %s", eventID));
        }
      } else {
        throw new IllegalStateException(
            String.format("No RFx dataList found for refReferenceCode - %s", eventID));
      }
    } else {
      throw new IllegalStateException(
          String.format("No RFx found for refReferenceCode - %s", eventID));
    }

    Integer ownerUserId;
    if (rfxSetting != null) {
      ownerUserId = rfxSetting.getOwnerUser().getId();
    } else {
      throw new IllegalStateException(
          String.format("No Owner found for refReferenceCode - %s", eventID));
    }

    String status = rfxSetting.getStatus();
    OperatorUser operatorUser = new OperatorUser();
    operatorUser.setId(ownerUserId);

    RfxInvalidate rfxInvalidate = new RfxInvalidate();
    rfxInvalidate.setOperatorUser(operatorUser);
    rfxInvalidate.setRfxReferenceCode(eventID);

    RfxInvalidationResponse jaggaerResponse;

    if (status != null && status.equals("To be Published")) {
      jaggaerResponse =
          rfxWorkflowsApi.deleteRFX(rfxInvalidate).block(Duration.ofSeconds(defaultApiTimeout));
    } else {
      jaggaerResponse =
          rfxWorkflowsApi.invalidateRFX(rfxInvalidate).block(Duration.ofSeconds(defaultApiTimeout));
    }
    final String finalStatus = (jaggaerResponse == null) ? "" : jaggaerResponse.getFinalStatus();

    String response;

    // Convert Jaggaer termination status to OCDS
    if (finalStatus != null && finalStatus.equalsIgnoreCase("Invalidated")) {
      response = "withdrawn";
    } else if (finalStatus != null && finalStatus.equalsIgnoreCase("Deleted")) {
      response = "cancelled";
    } else {
      response = finalStatus;
    }

    if (jaggaerResponse != null) {
      if (jaggaerResponse.getReturnMessage() != null
          && jaggaerResponse.getReturnMessage().contains("rfx can't be invalidate")) {
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
      } else {
        return new ResponseEntity<>(response, HttpStatus.OK);
      }
    }
    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
  }

  private Projects getProjectBody(ProjectTender projectTender) {

    Projects projects = new Projects();

    projects.setOperationCode("CREATE");

    AdditionalInfoList additionalInfoList = new AdditionalInfoList();
    List<AdditionalInfoItem> additionalInfoItemList = new ArrayList<>();
    AdditionalInfoItem additionalInfoItem = new AdditionalInfoItem();
    Values values = new Values();
    List<ValueItem> valueItemList = new ArrayList<>();
    ValueItem valueItem = new ValueItem();
    valueItem.setValue(projectTender.getProcurementReference());
    valueItemList.add(valueItem);
    values.setValue(valueItemList);
    additionalInfoItem.setName("Procurement Reference");
    additionalInfoItem.setValues(values);
    additionalInfoItemList.add(additionalInfoItem);
    additionalInfoList.setAdditionalInfo(additionalInfoItemList);

    Tender tender = new Tender();
    tender.setTitle(projectTender.getSubject());
    BuyerCompany buyerCompany = new BuyerCompany();
    buyerCompany.setId(defaultBuyerCompanyId);
    tender.setBuyerCompany(buyerCompany);
    ProjectOwner projectOwner = new ProjectOwner();
    projectOwner.setLogin(projectTender.getProjectOwnerLogin());
    tender.setProjectOwner(projectOwner);
    tender.setProjectType(defaultProjectType);
    tender.setAdditionalInfoList(additionalInfoList);

    Project project = new Project();
    project.setTender(tender);

    projects.setProject(project);

    return projects;
  }

  private Rfxs getRfxsBody(
      String tenderReferenceCode,
      String ownerLogin,
      uk.gov.crowncommercial.esourcing.integration.server.model.Rfx rfx) {

    Rfxs rfxs = new Rfxs();
    rfxs.setOperationCode("CREATE");

    String rfxTemplateReferenceCode = "";
    String rfxProcurementRoute = rfx.getProcurementRoute();

    if (rfxProcurementRoute.equalsIgnoreCase("Open Market")) {
      rfxTemplateReferenceCode = openMarketTemplateId;
    } else if (rfxProcurementRoute.equalsIgnoreCase("Single Tender Action")) {
      rfxTemplateReferenceCode = staTemplateId;
    } else {
      if (rfx.getTemplateReferenceCode() != null) {
        rfxTemplateReferenceCode = rfx.getTemplateReferenceCode();
      }
    }

    RfxAdditionalInfoList rfxAdditionalInfoList = new RfxAdditionalInfoList();
    rfxAdditionalInfoList.addAdditionalInfoItem(
        getRfxAddInfo("Procurement Route", rfxProcurementRoute));

    if (rfx.getFrameworkName() != null) {
      rfxAdditionalInfoList.addAdditionalInfoItem(
          getRfxAddInfo("Framework Name", rfx.getFrameworkName()));
    }
    if (rfx.getFrameworkLotNumber() != null) {
      rfxAdditionalInfoList.addAdditionalInfoItem(
          getRfxAddInfo("Lot Number", rfx.getFrameworkLotNumber()));
    }

    // TODO Awaiting confirmation from Jaggaer of parameter name
    //    if(rfx.getFrameworkRMNumber() != null){
    //      rfxAdditionalInfoList.addAdditionalInfoItem(
    //          getRfxAddInfo("RM Number", rfx.getFrameworkRMNumber()));
    //    }

    Rfx rfxJag = new Rfx();
    rfxJag.setRfxAdditionalInfoList(rfxAdditionalInfoList);

    RfxSetting rfxSetting = new RfxSetting();

    rfxSetting.setTenderReferenceCode(tenderReferenceCode);
    rfxSetting.setShortDescription(tenderReferenceCode + " " + rfx.getShortDescription());
    rfxSetting.setLongDescription(rfx.getLongDescription());
    BuyerCompany buyerCompany = new BuyerCompany();
    buyerCompany.setId(defaultBuyerCompanyId);
    rfxSetting.setBuyerCompany(buyerCompany);

    OwnerUser ownerUser = new OwnerUser();

      ownerUser.setLogin(ownerLogin);

    rfxSetting.setOwnerUser(ownerUser);
    rfxSetting.setValue(rfx.getValue());

    if (rfx.getRfiFlag() != null && rfx.getRfiFlag().equals("1")) {
      rfxSetting.setRfxType("STANDARD_PQQ");
      rfxSetting.setRfiFlag(1);
    } else {
      rfxSetting.setRfxType("STANDARD_ITT");
    }

    if (rfx.getQualEnvStatus() != null) {
      rfxSetting.setQualEnvStatus(rfx.getQualEnvStatus());
    }
    if (rfx.getTechEnvStatus() != null) {
      rfxSetting.setTechEnvStatus(rfx.getTechEnvStatus());
    }
    if (rfx.getCommEnvStatus() != null) {
      rfxSetting.setCommEnvStatus(rfx.getCommEnvStatus());
    }
    if (rfx.getVisibilityEGComments() != null) {
      rfxSetting.setVisibilityEGComments(rfx.getVisibilityEGComments());
    }
    if (rfx.getRankingStrategy() != null) {
      rfxSetting.setRankingStrategy(rfx.getRankingStrategy());
    }
    if (!rfxTemplateReferenceCode.isEmpty()) {
      rfxSetting.setTemplateReferenceCode(rfxTemplateReferenceCode);
    }

    rfxSetting.setPublishDate(String.valueOf(rfx.getPublishDate()));
    rfxSetting.setCloseDate(String.valueOf(rfx.getCloseDate()));
    rfxJag.setRfxSetting(rfxSetting);

    rfxs.setRfx(rfxJag);

    return rfxs;
  }

  private RfxAdditionalInfoItem getRfxAddInfo(String name, String value) {
    RfxAdditionalInfoItem rfxAdditionalInfoItem = new RfxAdditionalInfoItem();
    RfxValues rfxValues = new RfxValues();
    List<RfxValueItem> rfxValueItemList = new ArrayList<>();
    RfxValueItem rfxValueItem = new RfxValueItem();
    rfxValueItem.setValue(value);
    rfxValueItemList.add(rfxValueItem);
    rfxValues.setValue(rfxValueItemList);
    rfxAdditionalInfoItem.setName(name);
    rfxAdditionalInfoItem.setValues(rfxValues);

    return rfxAdditionalInfoItem;
  }

  private RfxStatusList getRfxStatusBody(List<RfxStatusItem> rfxStatusItemList) {

    RfxStatusList rfxStatusList = new RfxStatusList();

    final List<uk.gov.crowncommercial.esourcing.salesforce.client.model.RfxStatusItem> collect =
        rfxStatusItemList.stream()
            .map(
                r -> {
                  uk.gov.crowncommercial.esourcing.salesforce.client.model.RfxStatusItem sfr =
                      new uk.gov.crowncommercial.esourcing.salesforce.client.model.RfxStatusItem();
                  sfr.setProcurementReference(r.getProcurementReference());
                  sfr.setStage(r.getStage());
                  sfr.setActualStartDate(r.getActualStartDate());
                  sfr.setTenderEndDate(r.getTenderEndDate());
                  sfr.setClosingTime(r.getClosingTime());
                  return sfr;
                })
            .collect(Collectors.toList());

    rfxStatusList.addAll(collect);
    return rfxStatusList;
  }

  private List<RfxStatus200Response> convertRfxStatusResponse(
      RfxStatus200ResponseList sfRfxStatus200ResponseList) {

    return sfRfxStatus200ResponseList.stream()
        .map(
            r -> {
              RfxStatus200Response rfxStatus200Response = new RfxStatus200Response();
              rfxStatus200Response.setProcurementReference(r.getProcurementReference());
              rfxStatus200Response.setIsSuccess(r.getIsSuccess());
              rfxStatus200Response.setErrorMessage(r.getErrorMessage());
              return rfxStatus200Response;
            })
        .collect(Collectors.toList());
  }

  private boolean is5xxServerError(Throwable throwable) {
    return throwable instanceof WebClientResponseException
        && ((WebClientResponseException) throwable).getStatusCode().is5xxServerError();
  }
}
