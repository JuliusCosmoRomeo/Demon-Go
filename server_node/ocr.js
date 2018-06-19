// import Tesseract from 'tesseract.js'
const Tesseract = require('tesseract.js');

Tesseract.recognize("test.jpg", {lang: 'deu'})
  .then(function(result){
    console.log(result)
  })
  .catch(function(error) {
    console.log(error)
  });