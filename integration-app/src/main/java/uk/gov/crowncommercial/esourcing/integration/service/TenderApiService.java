package uk.gov.crowncommercial.esourcing.integration.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
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

  @Value("${ccs.esourcing.default.api.timeout}")
  private Long DEFAULT_API_TIMEOUT;

  public TenderApiService() {}

  @Override
  public ResponseEntity<InlineResponse201> createProcurementCase(
      uk.gov.crowncommercial.esourcing.integration.server.model.ProjectTender projectTender) {

    logger.info("Creating Tender with procurement : {}", projectTender);
    InlineResponse201 inlineResponse201 = new InlineResponse201();

    ProjectResponse projectsResponseBody =
        projectsApi.createProject(getProject(projectTender)).block(Duration.ofSeconds(DEFAULT_API_TIMEOUT));

    logger.info("Create Project response : {}", projectsResponseBody);

    final String tenderRef =
        projectsResponseBody != null ? projectsResponseBody.getTenderReferenceCode() : null;

    if (tenderRef != null) {
      RfxResponse rfxsResponseBody =
          rfxApi.createRFX(getRfxs(tenderRef, projectTender.getTender())).block(Duration.ofSeconds(DEFAULT_API_TIMEOUT));

      logger.info("Create ITT Event response : {}", rfxsResponseBody);

      final String ittRef = rfxsResponseBody != null ? rfxsResponseBody.getRfxReferenceCode() : null;


      inlineResponse201.setTenderReferenceCode(tenderRef);
      inlineResponse201.setRfxReferenceCode(ittRef);

      return new ResponseEntity<>(inlineResponse201, HttpStatus.CREATED);
    }

    return new ResponseEntity<>(inlineResponse201, HttpStatus.BAD_REQUEST);
  }

  private Projects getProject(ProjectTender projectTender) {
    BuyerCompany buyerCompany = new BuyerCompany();
    buyerCompany.setId("51435");

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
    tender.setBuyerCompany(buyerCompany);
    tender.setSourceTemplateReferenceCode("project_609");
    tender.setAdditionalInfoList(additionalInfoList);

    Project project = new Project();
    project.setTender(tender);

    Projects projects = new Projects();
    projects.setOperationCode("Create_From_Template");
    projects.setProject(project);

    return projects;
  }

  private Rfxs getRfxs(String tenderRef, uk.gov.crowncommercial.esourcing.integration.server.model.Tender tender){
    BuyerCompany buyerCompany = new BuyerCompany();
    buyerCompany.setId("51435");

    RfxAdditionalInfoList rfxAdditionalInfoList = new RfxAdditionalInfoList();
    rfxAdditionalInfoList.addAdditionalInfoItem(getRfxAddInfo("Procurement Route", "Call Off (Competition)"));
    rfxAdditionalInfoList.addAdditionalInfoItem(getRfxAddInfo("Framework Name", "TT3242"));
    rfxAdditionalInfoList.addAdditionalInfoItem(getRfxAddInfo("Lot Number", "Lot 1"));

    Rfx rfx = new Rfx();
    rfx.setRfxAdditionalInfoList(rfxAdditionalInfoList);

    OwnerUser ownerUser = new OwnerUser();
    ownerUser.setLogin("sro-ccs");

    RfxSetting rfxSetting = new RfxSetting();
    rfxSetting.tenderReferenceCode(tenderRef);
    rfxSetting.setShortDescription(String.valueOf(tender.getDescription()));
    rfxSetting.setBuyerCompany(buyerCompany);
    rfxSetting.setOwnerUser(ownerUser);
    rfxSetting.setTemplateReferenceCode("itt_543");
    rfxSetting.setValue(String.valueOf(tender.getValue()));
    rfxSetting.setPublishDate(String.valueOf(tender.getTenderPeriod().getStartDate()));
    rfxSetting.setCloseDate(String.valueOf(tender.getTenderPeriod().getEndDate()));
    rfx.setRfxSetting(rfxSetting);

    Rfxs rfxs = new Rfxs();
    rfxs.setOperationCode("Create_From_Template");
    rfxs.setRfx(rfx);

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
