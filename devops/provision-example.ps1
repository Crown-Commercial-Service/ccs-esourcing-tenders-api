# Example provisioning script (Windows PowerShell)
#
# This is a very simple deployment of a single application with no route or
# backing services and as such just requires a few environment variables to
# be set for the application in the required Space.
#
# Most of the variables required to be set are secrets that are passed to the
# application using environment variables. As such it is fine for this example
# be checked into version control but the actual Space specific provisioning
# scripts must not be.
#
# As these environment variables need to be set before the application is run
# and they can't be set until the application is deployed the application must
# first be deployed but not run. 
#
# Before running this scripts ensure the command shell is logged into the
# cloudfoundry organisation/space with permissions to manage the space.
#

# define the environment/space/spring profile we are using
$ENVIRONMENT = "dev"

# Application name.
# Also defines the URL endpoint for the deployed service. As such this must
# contain URL "friendly" characters and not contain a '.' as the URL
# endpoint then looks like a subdomain and the GOV.UK PaaS wildcard HTTPS
# certificates no longer work.
$APP_NAME = "ccs-esourcing-tenders-api"

# define app but do not push or start
cf apply-manifest -f .\manifest-$ENVIRONMENT.yml

# optionally set IP addresses if IP restricted
$SALESFORCE_IP_ADDRESSES = ""
$JAGGAER_IP_ADDRESSES = ""
$ROWEIT_IP_ADDRESSES = ""
$SWAGGER_HUB_IP_ADDRESSES = "3.223.162.99, 18.213.102.186, 23.23.82.88, 34.231.31.110, 54.158.217.73"
$SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_JAGGAER_CLIENT-SECRET = ""
$SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_JAGGAER_TOKEN-URI = ""
$SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_JAGGAER_CLIENT-ID = ""
$SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_JAGGAER_CLIENT-SECRET = ""
$SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_JAGGAER_AUTHORIZATION-GRANT-TYPE = ""
$CCS_ESOURCING_JAGGAER_CLIENT_URL = ""
$CCS_ESOURCING_SALESFORCE_CLIENT_URL = ""
cf set-env $APP_NAME CCS_ESOURCING_IP_ALLOW_LIST "$ROWEIT_IP_ADDRESSES $SWAGGER_HUB_IP_ADDRESSES"

# set the API keys needed to allow access to the application
cf set-env $APP_NAME CCS_ESOURCING_API_KEYS "pzoduvoasekghcbapsfypaicfisahh, nas4daig7audgoaijdvjklavbgyua8"

# set Jaggaer API endpoint URL
cf set-env $APP_NAME CCS_ESOURCING_JAGGAER_CLIENT_URL "https://crowncommercialservice-ws02-prep.bravosolution.co.uk/esop/jint/api/public/ja/v1/"

# set Salesforce API endpoint URL
cf set-env $APP_NAME CCS_ESOURCING_SALESFORCE_CLIENT_URL ""

# Rollbar configuration
# cf set-env $APP_NAME ROLLBAR_ENABLED true
# cf set-env $APP_NAME ROLLBAR_ACCESSTOKEN a098sd7gpaisdhkghalkjsdhy

# Application is now deployed but not running. It is anticipated that the actual
# deployment will be via TravisCI and no other manual configuration is required.

# Push (or start, or restage) the application (if required)
cf push -f .\manifest-$ENVIRONMENT.yml 
#cf start $APP_NAME
#cf restage $APP_NAME
