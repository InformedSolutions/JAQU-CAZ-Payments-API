# JAQU-CAZ Payments API

## Prerequisites

* Java 8
* aws-cli (for deployment).
See official [AWS Guide](https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-install.html)
for instructions.
* aws-sam-cli (for testing locally). See official [AWS Guide](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/serverless-sam-cli-install.html)
for instructions.
* Docker (for testing using mocks from Localstack and for aws-sam)


### Configuring code style formatter
There are style guides for _Eclipse_ and _Intellij IDEA_ located in `developer-resources`.
It is mandatory to import them and format code to match this configuration. Check Eclipse or IDEA
documentation for details how to set this up and format code that you work on.

### Adding and configuring Lombok
What is [Lombok](https://projectlombok.org/)?

*Project Lombok is a java library that automatically plugs into your editor and build tools, spicing up your java. Never write another getter or equals method again, with one annotation your class has a fully featured builder, Automate your logging variables, and much more.*

Lombok needs to be installed into Maven build process and into _Eclipse_ and _Intellij IDEA_.
1. Lombok and Maven - this is already configured in _pom.xml_ - nothing more to do.
2. Eclipse - follow up this [official tutorial](https://projectlombok.org/setup/eclipse) to install into Eclipse.
2. IDEA - follow up this [official tutorial](https://projectlombok.org/setup/intellij) to install into IDEA.

For more details about what Lombok can do see this [feature list](https://projectlombok.org/features/all).


### Configuring Nexus access
What is [Nexus](https://www.sonatype.com/nexus-repository-sonatype)?

*Nexus manages components, build artifacts, and release candidates in one central location.* We 
use it as repository for our internal artifacts but also as a proxy for Maven central repo - so as a cache
speeding up our builds.

You need to configure access to JAQU Nexus instance because without it you won't be able to build
and deploy artifacts and projects.

Firstly you need to obtain 3 values:
1. Nexus URL
2. Nexus username
3. Nexus password

You can ask a fellow developer or dedicated DevOps team for these values. Now you need to copy 
`settings.ci.xml.template` from `ci-cd-resources` directory to your local Maven repo dir: `~/.m2/`.
Then backup any existing `~/.m2/settings.xml` file and either copy contents of `settings.ci.xml.template` into
`settings.xml` or rename `settings.ci.xml.template` to `settings.xml`.

Now you need to set Nexus data.
You can either set 3 environment variables:
1. `export JAQU_NEXUS_URL=<nexus url>`
1. `export JAQU_NEXUS_USER=<nexus user>`
1. `export JAQU_NEXUS_PASSWORD=<nexus password>`

or:

Replace `${env.JAQU_NEXUS_URL}`, `${env.JAQU_NEXUS_USER}` and `${env.JAQU_NEXUS_PASSWORD}` strings in
`settings.xml` to the values you got from colleague or DevOps team.

### AWS setup
As this service integrates with AWS Secrets manager, you will need to configure your ~/.aws settings appropriately. In the ~/.aws/config file, a default region must be set as per the below:

```
[default]
region = eu-west-2
```

Credentials must also be setup under a profile named 'dev' which is applied as the active profile on Spring starting up (see application.yml). In the ~/.aws/credentials file, this requires a profile to be present as per the below:

```
[dev]
aws_access_key_id = ************
aws_secret_access_key = *************
```



### Vagrant
Optionally you can use Virtual Machine to compile and test project.
A Vagrant development machine definition inclusive of the following software assets can be found at the root of this repository:

1. Ubuntu 18.04 LTS
1. Eclipse for Java Enterprise
1. OpenJDK 8
1. Maven
1. Git
1. Docker CE (for backing tools used for example to emulate AWS lambda functions and DB instances)

As a complimentary note, this Vagrant image targets VirtualBox as its provider. As such, the necessary technical dependencies installed on the host are simply VirtualBox and Vagrant.

## (dev) Deployment

The following command will build, pack and deploy the service as a artifact used by AWS Lambda
and API Gateway.

```
$ make build deploy-to-aws S3_BUCKET_NAME=name_of_your_bucket STACK_NAME=name_of_your_stack
```

To only deploy:

```
$ make deploy-to-aws S3_BUCKET_NAME=name_of_your_bucket STACK_NAME=name_of_your_stack
```

## Local Development: building, running and testing

[Detailed descripton of how to build, run and test service](RUNNING_AND_TESTING.md)

## API specification

API specification is available at `{server.host}:{server.port}/v2/api-docs` (locally usually at http://localhost:8080/v2/api-docs)
