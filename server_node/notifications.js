const admin = require('firebase-admin');
const path = require('path');

let _serviceAccount
function getServiceAccount() {
  if (!_serviceAccount) {
    _serviceAccount = require(path.join(__dirname, 'firebase-adminsdk.json'));
    admin.initializeApp({
      credential: admin.credential.cert(_serviceAccount),
      databaseURL: 'https://demon-go.firebaseio.com'
    });
  }
  return _serviceAccount
}

const TITLES = ['Look!'];

module.exports.sendTo = function(text, token) {
  const serviceAccount = getServiceAccount()

  return admin.messaging().sendToDevice(token, {
    notification: {
      title: TITLES[parseInt(Math.random() * TITLES.length)],
      body: text
    }
  }, {timeToLive: 10}).then(function (response) {
    console.log('Successfully sent message:', response)
  })
  .catch(function (error) {
    console.log('Error sending message:', error)
    throw error
  });
};

