## Usage
```
$ yarn install
```

Generating SQL statements that insert data into `payment` and `vehicle_entrant_payment` tables:

```
$ node payments-data-generator.js --recordsCnt NUMBER_OF_RECORDS
```

The above command generates `NUMBER_OF_RECORDS` rows in `payment` table 
and a random number of rows in `vehicle_entrant_payment` table for the added payment. 

