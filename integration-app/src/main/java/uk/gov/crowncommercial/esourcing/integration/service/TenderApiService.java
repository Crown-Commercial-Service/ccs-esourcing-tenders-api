package uk.gov.crowncommercial.esourcing.integration.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import uk.gov.crowncommercial.esourcing.integration.server.api.TendersApiDelegate;
import uk.gov.crowncommercial.esourcing.integration.server.model.InlineResponse201;
import uk.gov.crowncommercial.esourcing.integration.server.model.ProjectTender;
import uk.gov.crowncommercial.esourcing.jaggaer.client.ProjectsApi;
import uk.gov.crowncommercial.esourcing.jaggaer.client.RfxApi;
import uk.gov.crowncommercial.esourcing.jaggaer.client.model.AdditionalInfoItem;
import uk.gov.crowncommercial.esourcing.jaggaer.client.model.AdditionalInfoList;
import uk.gov.crowncommercial.esourcing.jaggaer.client.model.BuyerCompany;
import uk.gov.crowncommercial.esourcing.jaggaer.client.model.OwnerUser;
import uk.gov.crowncommercial.esourcing.jaggaer.client.model.Project;
import uk.gov.crowncommercial.esourcing.jaggaer.client.model.ProjectResponse;
import uk.gov.crowncommercial.esourcing.jaggaer.client.model.Projects;
import uk.gov.crowncommercial.esourcing.jaggaer.client.model.Rfx;
import uk.gov.crowncommercial.esourcing.jaggaer.client.model.RfxAdditionalInfoItem;
import uk.gov.crowncommercial.esourcing.jaggaer.client.model.RfxAdditionalInfoList;
import uk.gov.crowncommercial.esourcing.jaggaer.client.model.RfxResponse;
import uk.gov.crowncommercial.esourcing.jaggaer.client.model.RfxSetting;
import uk.gov.crowncommercial.esourcing.jaggaer.client.model.RfxValueItem;
import uk.gov.crowncommercial.esourcing.jaggaer.client.model.RfxValues;
import uk.gov.crowncommercial.esourcing.jaggaer.client.model.Rfxs;
import uk.gov.crowncommercial.esourcing.jaggaer.client.model.Tender;
import uk.gov.crowncommercial.esourcing.jaggaer.client.model.ValueItem;
import uk.gov.crowncommercial.esourcing.jaggaer.client.model.Values;

@Service
public class TenderApiService implements TendersApiDelegate {

  @Autowired private ProjectsApi projectsApi;
  @Autowired private RfxApi rfxApi;

  private static final Logger logger = LoggerFactory.getLogger(TenderApiService.class);

  @Value("${ccs.esourcing.default.api.timeout}000")
  private Long defaultApiTimeout;

  public TenderApiService() {}

  @Override
  public ResponseEntity<InlineResponse201> createProcurementCase(
      uk.gov.crowncommercial.esourcing.integration.server.model.ProjectTender projectTender) {

    InlineResponse201 inlineResponse201 = new InlineResponse201();

    logger.info("Creating Tender with procurement : {}", projectTender);

    Projects projectsRequestBody = getProject(projectTender);

    logger.info("Sending Project request to Jaggaer, request body : {}", projectsRequestBody);

    ProjectResponse projectsResponseBody =
        projectsApi.createProject(projectsRequestBody).block(Duration.ofSeconds(defaultApiTimeout));

    logger.info("Create Project response : {}", projectsResponseBody);

    final String tenderRef =
        projectsResponseBody != null ? projectsResponseBody.getTenderReferenceCode() : null;

    if (tenderRef != null) {

      Rfxs rfxRequestBody = getRfxs(tenderRef, projectTender.getRfx());

      logger.info("Sending RFX request to Jaggaer, request body : {}", rfxRequestBody);

      RfxResponse rfxsResponseBody =
          rfxApi.createRFX(rfxRequestBody).block(Duration.ofSeconds(defaultApiTimeout));

      logger.info("Create ITT Event response : {}", rfxsResponseBody);

      final String ittRef =
          rfxsResponseBody != null ? rfxsResponseBody.getRfxReferenceCode() : null;

      inlineResponse201.setTenderReferenceCode(tenderRef);
      inlineResponse201.setRfxReferenceCode(ittRef);

      return new ResponseEntity<>(inlineResponse201, HttpStatus.CREATED);
    }

    return new ResponseEntity<>(inlineResponse201, HttpStatus.BAD_REQUEST);
  }

  private Projects getProject(ProjectTender projectTender) {

    Projects projects = new Projects();
    if (StringUtils.isNotEmpty(projectTender.getTenderReferenceCode())) {
      projects.setOperationCode("CREATEUPDATE");
    } else {
      projects.setOperationCode("CREATE_FROM_TEMPLATE");
    }

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
    buyerCompany.setId("51435");
    tender.setBuyerCompany(buyerCompany);
    if (StringUtils.isNotEmpty(projectTender.getTenderReferenceCode())) {
      tender.setTenderReferenceCode(projectTender.getTenderReferenceCode());
      tender.setProjectType("CCS_PROJ");
    } else {
      tender.setSourceTemplateReferenceCode("project_609");
    }

    tender.setAdditionalInfoList(additionalInfoList);

    Project project = new Project();
    project.setTender(tender);

    projects.setProject(project);

    return projects;
  }

  private Rfxs getRfxs(
      String tenderReferenceCode,
      uk.gov.crowncommercial.esourcing.integration.server.model.Rfx rfx) {

    Rfxs rfxs = new Rfxs();
    rfxs.setOperationCode("CREATE");

    RfxAdditionalInfoList rfxAdditionalInfoList = new RfxAdditionalInfoList();
    rfxAdditionalInfoList.addAdditionalInfoItem(
        getRfxAddInfo("Procurement Route", "Call Off (Competition)"));

    Rfx rfxJag = new Rfx();
    rfxJag.setRfxAdditionalInfoList(rfxAdditionalInfoList);

    RfxSetting rfxSetting = new RfxSetting();

    rfxSetting.setTenderReferenceCode(tenderReferenceCode);
    rfxSetting.setShortDescription(String.valueOf(rfx.getRfxSetting().getShortDescription()));

    BuyerCompany buyerCompany = new BuyerCompany();
    buyerCompany.setId("51435");
    rfxSetting.setBuyerCompany(buyerCompany);

    OwnerUser ownerUser = new OwnerUser();
    ownerUser.setLogin("sro-ccs");
    rfxSetting.setOwnerUser(ownerUser);
    rfxSetting.setValue(String.valueOf(rfx.getRfxSetting().getValue()));
    rfxSetting.setRfxType("STANDARD_ITT");

    if (StringUtils.isNotEmpty(rfx.getRfxSetting().getValueCurrency())) {
      rfxSetting.setValueCurrency(rfx.getRfxSetting().getValueCurrency());
    }
    if (rfx.getRfxSetting().getBidsCurrency() != null) {
      rfxSetting.setBidsCurrency(rfx.getRfxSetting().getBidsCurrency());
    }
    if (rfx.getRfxSetting().getValueCurrency() != null) {
      rfxSetting.setValueCurrency(rfx.getRfxSetting().getValueCurrency());
    }
    if (rfx.getRfxSetting().getQualEnvStatus() != null) {
      rfxSetting.setQualEnvStatus(rfx.getRfxSetting().getQualEnvStatus());
    }
    if (rfx.getRfxSetting().getTechEnvStatus() != null) {
      rfxSetting.setTechEnvStatus(rfx.getRfxSetting().getTechEnvStatus());
    }
    if (rfx.getRfxSetting().getCommEnvStatus() != null) {
      rfxSetting.setCommEnvStatus(rfx.getRfxSetting().getCommEnvStatus());
    }
    if (rfx.getRfxSetting().getVisibilityEGComments() != null) {
      rfxSetting.setVisibilityEGComments(rfx.getRfxSetting().getVisibilityEGComments());
    }
    if (rfx.getRfxSetting().getRankingStrategy() != null) {
      rfxSetting.setRankingStrategy(rfx.getRfxSetting().getRankingStrategy());
    }

    rfxSetting.setPublishDate(String.valueOf(rfx.getRfxSetting().getPublishDate()));
    rfxSetting.setCloseDate(String.valueOf(rfx.getRfxSetting().getCloseDate()));
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
}
