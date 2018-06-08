var express = require('express');
var fs = require('fs');
var app = express();
var http = require('http').Server(app);
var io = require('socket.io')(http);
var bodyParser = require('body-parser');
var uuidv4 = require('uuid/v4');
var notifications = require('./notifications');

app.use(bodyParser.json({limit: '10mb'}))
app.use(bodyParser.urlencoded({limit: '10mb'}))

app.get('/', function(req, res){
  res.sendFile(__dirname + '/index.html');
});

app.post('/post', (req, res) => {
  // answer immediately, client doesnt care if we save
  res.send();

  var buffer = Buffer.from(req.body.image, 'base64');
  const path = '/images/' + uuidv4() + '.jpg';
  fs.writeFile(__dirname + path, buffer, 'binary', err => {
    if (err)
      return console.log('failed to write image', err);
    io.sockets.emit('new image', {path});
  });

  notifications.sendTo('Got one!', req.body.token);
});

app.use('/images', express.static(__dirname + '/images'))

io.on('connection', function(socket){
  // TODO maybe init new sockets with existing pictures: socket.emit('init');
});

var port = process.env.PORT || 8088;
http.listen(port, function(){
  console.log('listening on *:' + port);
});
