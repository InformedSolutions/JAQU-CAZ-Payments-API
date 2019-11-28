const argv = require('minimist')(process.argv.slice(2));

const recordsCnt = argv['recordsCnt'];
if (!recordsCnt)
  throw new Error('Please specify the number of records that will be generated: --recordsCnt');

const cazId = '938cac88-1103-11ea-a1a6-33ad4299653d';
for(let i = 0; i < recordsCnt; i++) {
  const extPaymentId = externalPaymentId();
  const stringifiedExtPaymentId = extPaymentId ? `'${extPaymentId}'` : null;
  const externalStatus = randomStatus();
  const internalStatus = externalStatus === 'SUCCESS' ? 'PAID' : 'NOT_PAID';
  const submittedTimestamp = paymentSubmittedTimestamp();
  const paymentId = uuidv4();
  const total = totalPaid();
  console.log(`insert into payment(payment_id, payment_method, payment_provider_id, payment_provider_status, total_paid, payment_submitted_timestamp) values ('${paymentId}', 'CREDIT_DEBIT_CARD', ${stringifiedExtPaymentId}, '${externalStatus}', ${total}, '${submittedTimestamp.toISOString()}');`);

  const numberOfVehicleEntrantPayments = randomIntInclusive(2, 10);
  const startTravelDate = randomDateLessThan(submittedTimestamp);
  const vrn = randomVrn();
  for (let j = 0; j < numberOfVehicleEntrantPayments; j++) {
    const travelDate = new Date(startTravelDate.getTime() + j * 24*60*60*1000).toLocaleDateString('pl-PL');
    console.log(`insert into vehicle_entrant_payment (vehicle_entrant_payment_id, payment_id, vrn, caz_id, travel_date, charge_paid, payment_status) values ('${uuidv4()}', '${paymentId}', '${vrn}', '${cazId}', '${travelDate}', ${Math.floor(total / numberOfVehicleEntrantPayments)}, '${internalStatus}');`)
  }
}

function randomStatus() {
  return randomDigit() & 1 ? 'CREATED' : 'SUCCESS';
}

function randomVrn() {
  return `${randomUppercaseString(2)}${randomDigit()}${randomDigit()}${randomUppercaseString(3)}`
}

function uuidv4() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
    var r = Math.random() * 16 | 0, v = c == 'x' ? r : (r & 0x3 | 0x8);
    return v.toString(16);
  });
}

function externalPaymentId() {
  return randomString(10);
}

function totalPaid() {
  return randomIntInclusive(100, 500);
}

function paymentSubmittedTimestamp() {
  return randomPastDate();
}

function randomPastDate() {
  const numberOfDays = 14; // last 14 days
  const offset = Math.abs(Math.random())*24*60*60*1000 * numberOfDays;
  const date = new Date(new Date().getTime() - offset);
  return date;
}

function randomDateLessThan(input) {
  const numberOfDays = 14; // last 14 days
  const offset = Math.abs(Math.random())*24*60*60*1000 * numberOfDays;
  const date = new Date(input.getTime() - offset);
  return date;
}

function randomUppercaseString(length) {
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';
  var result = '';
  for (var i = length; i > 0; --i) result += chars[Math.floor(Math.random() * chars.length)];
  return result;
}

function randomString(length) {
  const chars = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ';
  var result = '';
  for (var i = length; i > 0; --i) result += chars[Math.floor(Math.random() * chars.length)];
  return result;
}

function randomDigit() {
  return Math.floor(Math.random() * 10) ;
}

function randomIntInclusive(min, max) {
  min = Math.ceil(min);
  max = Math.floor(max);
  return Math.floor(Math.random() * (max - min + 1)) + min; //The maximum is inclusive and the minimum is inclusive
}
