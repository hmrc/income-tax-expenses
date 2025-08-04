
# income-tax-expenses
This is where we make API calls from users viewing and making changes to the employment expenses section of their income tax return.

## Running the service locally

You will need to have the following:
- Installed/configured [service manager](https://github.com/hmrc/service-manager).

This can be found in the [developer handbook](https://docs.tax.service.gov.uk/mdtp-handbook/documentation/developer-set-up/)

The service manager profile for this service is:

    sm2 --start INCOME_TAX_EXPENSES
Run the following command to start the remaining services locally:

    sudo mongod (If not already running)
    sm2 --start INCOME_TAX_SUBMISSION_ALL

This service runs on port: `localhost:9318`

To test the branch you're working on locally. You will need to run `sm2 --stop INCOME_TAX_EXPENSES` followed by
`./run.sh`

### Running Tests

- Run Unit Tests:  `sbt test`
- Run Integration Tests: `sbt it/test`
- Run Unit and Integration Tests: `sbt test it/test`
- Run Unit and Integration Tests with coverage report: `./check.sh`<br/>
  which runs `sbt clean coverage test it/test coverageReport dependencyUpdates`

### Expenses endpoints:

- **GET    /income-tax/nino/:nino/sources** (Gets either hmrc expenses or customer expenses data)

- **PUT    /income-tax/nino/:nino/sources** (Creates or updates employment expenses, if an update to hmrc expenses is being made it will also ignore the hrmc expenses and create customer expenses data)  

- **DELETE /income-tax/nino/:nino/sources/:toRemove** (Deletes hmrc expenses, customer expenses or both. Delete for hmrc expenses is actually ignored (Adds an ignored date to hmrc expenses data))


### Downstream services
All expenses data is retrieved / updated via one of two downstream systems.
- DES (Data Exchange Service)
- IF (Integration Framework)

### Employment Sources (HMRC-Held and Customer Data)
Employment data can come from different sources: HMRC-Held and Customer. HMRC-Held data is employment data that HMRC have for the user within the tax year, prior to any updates made by the user. The employment data displayed in-year is HMRC-Held. This is the same for expenses data, you can have hmrc held expenses and/or customer expenses data.

Customer data is provided by the user. At the end of the tax year, users can view any existing employment data and make changes (create, update and delete).

<details>
<summary>Click here to see an example of a user with HMRC-Held and Customer data (JSON)</summary>

```json
{
  "employment": [
    {
      "taxYear": 2022,
      "hmrcEmployments": [
        {
          "employmentId": "00000000-0000-1000-8000-000000000000",
          "employerName": "Vera Lynn",
          "employerRef": "123/12345",
          "payrollId": "123345657",
          "startDate": "2020-06-17",
          "cessationDate": "2020-06-17",
          "dateIgnored": "2020-06-17T10:53:38Z",
          "employmentData": {
            "submittedOn": "2020-01-04T05:01:01Z",
            "source": "HMRC-HELD",
            "employment": {
              "employmentSequenceNumber": "1002",
              "payrollId": "123456789999",
              "companyDirector": false,
              "closeCompany": true,
              "directorshipCeasedDate": "2020-02-12",
              "startDate": "2019-04-21",
              "cessationDate": "2020-03-11",
              "occPen": false,
              "disguisedRemuneration": false,
              "employer": {
                "employerRef": "223/AB12399",
                "employerName": "maggie"
              },
              "pay": {
                "taxablePayToDate": 34234.15,
                "totalTaxToDate": 6782.92,
                "payFrequency": "CALENDAR MONTHLY",
                "paymentDate": "2020-04-23",
                "taxWeekNo": 32
              },
              "deductions": {
                "studentLoans": {
                  "uglDeductionAmount": 13343.45,
                  "pglDeductionAmount": 24242.56
                }
              },
              "benefitsInKind": {
                "accommodation": 100,
                "assets": 100,
                "assetTransfer": 100,
                "beneficialLoan": 100,
                "car": 100,
                "carFuel": 100,
                "educationalServices": 100,
                "entertaining": 100,
                "expenses": 100,
                "medicalInsurance": 100,
                "telephone": 100,
                "service": 100,
                "taxableExpenses": 100,
                "van": 100,
                "vanFuel": 100,
                "mileage": 100,
                "nonQualifyingRelocationExpenses": 100,
                "nurseryPlaces": 100,
                "otherItems": 100,
                "paymentsOnEmployeesBehalf": 100,
                "personalIncidentalExpenses": 100,
                "qualifyingRelocationExpenses": 100,
                "employerProvidedProfessionalSubscriptions": 100,
                "employerProvidedServices": 100,
                "incomeTaxPaidByDirector": 100,
                "travelAndSubsistence": 100,
                "vouchersAndCreditCards": 100,
                "nonCash": 100
              }
            }
          }
        }
      ],
      "customerEmployments": [
        {
          "employmentId": "00000000-0000-1000-8000-000000000002",
          "employerName": "Vera Lynn",
          "employerRef": "123/12345",
          "payrollId": "123345657",
          "startDate": "2020-06-17",
          "cessationDate": "2020-06-17",
          "submittedOn": "2020-06-17T10:53:38Z",
          "employmentData": {
            "submittedOn": "2020-02-04T05:01:01Z",
            "employment": {
              "employmentSequenceNumber": "1002",
              "payrollId": "123456789999",
              "companyDirector": false,
              "closeCompany": true,
              "directorshipCeasedDate": "2020-02-12",
              "startDate": "2019-04-21",
              "cessationDate": "2020-03-11",
              "occPen": false,
              "disguisedRemuneration": false,
              "employer": {
                "employerRef": "223/AB12399",
                "employerName": "maggie"
              },
              "pay": {
                "taxablePayToDate": 34234.15,
                "totalTaxToDate": 6782.92,
                "payFrequency": "CALENDAR MONTHLY",
                "paymentDate": "2020-04-23",
                "taxWeekNo": 32
              },
              "deductions": {
                "studentLoans": {
                  "uglDeductionAmount": 13343.45,
                  "pglDeductionAmount": 24242.56
                }
              },
              "benefitsInKind": {
                "accommodation": 100,
                "assets": 100,
                "assetTransfer": 100,
                "beneficialLoan": 100,
                "car": 100,
                "carFuel": 100,
                "educationalServices": 100,
                "entertaining": 100,
                "expenses": 100,
                "medicalInsurance": 100,
                "telephone": 100,
                "service": 100,
                "taxableExpenses": 100,
                "van": 100,
                "vanFuel": 100,
                "mileage": 100,
                "nonQualifyingRelocationExpenses": 100,
                "nurseryPlaces": 100,
                "otherItems": 100,
                "paymentsOnEmployeesBehalf": 100,
                "personalIncidentalExpenses": 100,
                "qualifyingRelocationExpenses": 100,
                "employerProvidedProfessionalSubscriptions": 100,
                "employerProvidedServices": 100,
                "incomeTaxPaidByDirector": 100,
                "travelAndSubsistence": 100,
                "vouchersAndCreditCards": 100,
                "nonCash": 100
              }
            }
          }
        }
      ],
      "employmentExpenses": {
        "submittedOn": "2022-12-12T12:12:12Z",
        "dateIgnored": "2022-12-11T12:12:12Z",
        "source": "HMRC-HELD",
        "totalExpenses": 100,
        "expenses": {
          "businessTravelCosts": 100,
          "jobExpenses": 100,
          "flatRateJobExpenses": 100,
          "professionalSubscriptions": 100,
          "hotelAndMealExpenses": 100,
          "otherAndCapitalAllowances": 100,
          "vehicleExpenses": 100,
          "mileageAllowanceRelief": 100
        }
      }
    }
  ]
}
```

</details>


### Local

#### Individual
* Login via: [http://localhost:9949/auth-login-stub/gg-sign-in](http://localhost:9949/auth-login-stub/gg-sign-in)
* Entry page: [http://localhost:9302/update-and-submit-income-tax-return/2025/start](http://localhost:9302/update-and-submit-income-tax-return/2025/start)

| Enrolment Key | Identifier Name | Identifier Value |
|---------------|-----------------|------------------|
| HMRC-MTD-IT   | MTDITID         | 1234567890       |


### Agent
* Login via: [http://localhost:9949/auth-login-stub/gg-sign-in](http://localhost:9949/auth-login-stub/gg-sign-in)
* Entry page : [http://localhost:9302/update-and-submit-income-tax-return/test-only/2024/additional-parameters?ClientNino=AA123459A&ClientMTDID=1234567890](http://localhost:9302/update-and-submit-income-tax-return/test-only/2024/additional-parameters?ClientNino=AA123459A&ClientMTDID=1234567890)

| Enrolment Key  | Identifier Name      | Identifier Value	 |
|----------------|----------------------|-------------------|
| HMRC-MTD-IT    | MTDITID              | 1234567890        |
| HMRC-AS-AGENT  | AgentReferenceNumber | XARN1234567       |

### Staging

*Requires HMRC VPN*

#### Individual
* Login via: [https://www.staging.tax.service.gov.uk/auth-login-stub/gg-sign-in](https://www.staging.tax.service.gov.uk/auth-login-stub/gg-sign-in)
* Entry page : [https://www.staging.tax.service.gov.uk/update-and-submit-income-tax-return/2026/start](https://www.staging.tax.service.gov.uk/update-and-submit-income-tax-return/2026/start)

| Enrolment Key | Identifier Name | Identifier Value |
|---------------|-----------------|------------------|
| HMRC-MTD-IT   | MTDITID         | 1234567890       |

#### Agent
* Login via: [https://www.staging.tax.service.gov.uk/auth-login-stub/gg-sign-in](https://www.staging.tax.service.gov.uk/auth-login-stub/gg-sign-in)
* Entry page : [https://www.staging.tax.service.gov.uk/update-and-submit-income-tax-return/test-only/2026/additional-parameters?ClientNino=AA123459A&ClientMTDID=1234567890](https://www.staging.tax.service.gov.uk/update-and-submit-income-tax-return/test-only/2026/additional-parameters?ClientNino=AA123459A&ClientMTDID=1234567890)

| Enrolment Key  | Identifier Name      | Identifier Value	 |
|----------------|----------------------|-------------------|
| HMRC-MTD-IT    | MTDITID              | 1234567890        |
| HMRC-AS-AGENT  | AgentReferenceNumber | XARN1234567       |

## Ninos with stub data for employment

### In-Year
| Nino      | Employment data                                                  | Source    |
|-----------|------------------------------------------------------------------|-----------|
| AA123459A | Single employment - Employment details and expenses              | HMRC-Held |
| AA133742A | Single employment - Employment details, benefits and expenses    | HMRC-Held |
| BB444444A | Multiple employments - Employment details, benefits and expenses | HMRC-Held |

### End of Year
| Nino      | Employment data                                                  | Source              |
|-----------|------------------------------------------------------------------|---------------------|
| AA123459A | Single employment - Employment details and expenses              | HMRC-Held, Customer |
| BB444444A | Multiple employments - Employment details, benefits and expenses | HMRC-Held, Customer |
| AA333444A | User with only expenses data                                     | HMRC-Held           |

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
