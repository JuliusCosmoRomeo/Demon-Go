var express = require('express');
var app = express();
var http = require('http').Server(app);
var io = require('socket.io')(http);
var bodyParser = require('body-parser');
var uuidv4 = require('uuid/v4');

app.use(bodyParser.json())
app.use(bodyParser.urlencoded())

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
      return console.err('failed to write image', err);
    io.sockets.emit('new image', {path});
  });
});

app.use('/images', express.static(__dirname + '/images'))

io.on('connection', function(socket){
  socket.on('disconnect', function(){
    console.log('user disconnected');
  });

  // TODO maybe init new sockets with existing pictures: socket.emit('init');
});

var port = process.env.PORT || 8088;
http.listen(port, function(){
  console.log('listening on *:' + port);
});
