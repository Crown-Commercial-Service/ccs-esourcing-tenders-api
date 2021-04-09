package uk.gov.crowncommercial.esourcing.integration.exception;

public class SalesforceError {

  private String procurementRef;
  private String ownerUserLogin;

  public String getProcurementRef() {
    return procurementRef;
  }

  public void setProcurementRef(String procurementRef) {
    this.procurementRef = procurementRef;
  }

  public String getOwnerUserLogin() {
    return ownerUserLogin;
  }

  public void setOwnerUserLogin(String ownerUserLogin) {
    this.ownerUserLogin = ownerUserLogin;
  }
}
