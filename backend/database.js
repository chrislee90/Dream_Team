//require Firebase and Express
var firebase = require('firebase');
var express = require('express');
var app = express();
var bodyParser = require('body-parser');

app.use(bodyParser.json());  //parse JSON
app.use(bodyParser.urlencoded({     // to support URL-encoded bodies
  extended: true
}));

var firebaseConfig = {
  apiKey: "AIzaSyANV9dOPbD9ViJMUzzWSC1OUAUFbZZRYBo",
  authDomain: "dreamteam-5a7d0.firebaseapp.com",
  databaseURL: "https://dreamteam-5a7d0.firebaseio.com",
  projectId: "dreamteam-5a7d0",
  storageBucket: "dreamteam-5a7d0.appspot.com",
  messagingSenderId: "641791408552",
  appId: "1:641791408552:android:b8e579fd96f30568d8859d",
  measurementId: "G-measurement-id",
};

firebase.initializeApp(firebaseConfig);

// get reference to the database service
var database = firebase.database();

// check friendship status between two users

app.post('/friendshipStatus', function (req, res) {
  console.log('HTTP Post Req.')
  var user = req.body.uid;
  var friend = req.body.friendId
  console.log('friend:' + friend)

  var userFriendsListRef = database.ref('/users/'+user).child('friends').orderByChild('uid').equalTo(friend);
  var userPendingListRef = database.ref('/users/'+user).child('pendingList').orderByChild('uid').equalTo(friend);
  var userRequestingListRef = database.ref('/users/'+user).child('pendingRequests').orderByChild('uid').equalTo(friend);

  userFriendsListRef.once('value', function(snapshot) {
    if(snapshot.val() != null){
      console.log('users are friends.');
      res.json(0);
      console.log(0);
      return;
    } else {
      console.log('users are not friends.');
      userPendingListRef.once('value', function(snapshot) {
        if(snapshot.val() != null){
          console.log('user is requesting friendship to friend.');
          res.json(1);
          console.log(1);
          return;
        } else {
          console.log('user is not requesting friendship to friend.');
          userRequestingListRef.once('value', function(snapshot) {
            if(snapshot.val() != null){
              console.log('friend is requesting friendship to user.');
              res.json(2);
              console.log(2);
            } else {
              console.log('friend is not requesting friendsip to the user.');
              res.json(3);
              console.log(3);
              }
          });
        }
      });
    }
  });
});

//get user by its uid
app.get('/getUserByUid', function (req, res) {

  console.log('HTTP GET Req.');
  var userReference = database.ref('/users/'+req.query.uId);

  //async. callback to read user
  userReference.on('value', function(snapshot) {
    console.log(snapshot.val());
    res.json(snapshot.val());
    userReference.off('value');
  },
  function (errorObject) {
    console.log('Read FAIL:' + errorObject.code);
    res.send('Read FAIL:' + errorObject.code);
  });
});

//get list of users matching given name parameter
app.get('/getUsersByName', function(req, res) {

  console.log('HTTP GET Req.');
  var usersReference = database.ref('/users/').orderByChild('name').equalTo(req.query.name);

  //async. callback to read list of users with given name in the req
  usersReference.on('value', function(snapshot) {
    var resArray = [];

    snapshot.forEach(function(childSnapshot) {
      var u = childSnapshot.val();

      resArray.push(u);
    });
    console.log(resArray);
    res.json(resArray);
    usersReference.off('value');
  },
  function (errorObject) {
    console.log('Read FAIL:' + errorObject.code);
    res.send('Read FAIL:' + errorObject.code);
  });
});

//get list of friends for given uId
app.get('/getUserFriends', function(req, res) {

  console.log('HTTP GET Req.');
  var userReference = database.ref('/users/'+req.query.uId).child('friends');

  //async. call back to get list of uIds of friends
  userReference.on('value', function(snapshot) {
    var resArray = [];
    snapshot.forEach(function(childSnapshot) {
      var friend = childSnapshot.val()
      resArray.push(friend)
    });
    console.log(resArray);
    res.json(resArray);
    userReference.off('value');
  },
  function (errorObject) {
    console.log('Read FAIL:' + errorObject.code);
    res.send('Read FAIL:' + errorObject.code);
  });
});

//get list of friend requests for given uId
app.get('/getUserFriendRequests', function(req, res) {
  var userReference = database.ref('/users/'+req.query.uId).child('pendingRequests');
  userReference.on('value', function(snapshot) {
    var resArray = [];

    snapshot.forEach(function(childSnapshot) {
      var request = childSnapshot.val()
      resArray.push(request)
    });
    console.log(resArray);
    res.json(resArray);
    userReference.off('value');
  },
  function (errorObject) {
    console.log('Read FAIL:' + errorObject.code);
    res.send('Read FAIL:' + errorObject.code);
  });
  });

//friend request
app.post('/friendRequest', function(req, res) {

  console.log('HTTP POST Req.');
  var user = req.body.uid;
  var friendId = req.body.friendId;
  var picture = req.body.picture;
  var name = req.body.name;
  console.log('user requesting: ' + user + 'friendship on:' + friendId);

  var userPendingList = database.ref('/users/'+user).child('pendingList');
  var friendPendingRequests = database.ref('/users/'+friendId).child('pendingRequests');

  userPendingList.push().set({
    'uid': friendId
  });

  friendPendingRequests.push().set({
    'uid': user,
    'name': name,
    'picture': picture,
  })
  res.send('SUCCESS.');
  console.log('SUCCESS.');
});

//deny friendship
app.post('/denyFriendship', function(req, res) {
  console.log('HTTP POST Req.');
  var user = req.body.uid;
  var friendId = req.body.friendId;
  console.log('user: ' + user + 'denies friendship to: ' + friendId);

  var userPendingRequests = database.ref('/users/'+user).child('pendingRequests').orderByChild('uid').equalTo(friendId);
  var friendPendingList = database.ref('/users/'+friendId).child('pendingList').orderByChild('uid').equalTo(user);

  userPendingRequests.once('value', function(snapshot) {
    snapshot.forEach(function(friendSnapshot) {
      friendSnapshot.ref.remove();
    });
    friendPendingList.once('value', function(snapshot) {
      snapshot.forEach(function(userSnapshot) {
        userSnapshot.ref.remove();
      });
    });
  });
  res.send('SUCCESS.');
  console.log('SUCCESS.');
});

// add a friend
app.post('/addFriend', function(req, res) {

  console.log('HTTP POST Req.');
  var user = req.body.uid;
  var userName = req.body.userName;
  var userPic = req.body.userPic;
  var friendId = req.body.friendId;
  var friendName = req.body.friendName;
  var friendPic = req.body.friendPic;
  console.log('user: ' + user + 'accepted friendship with:' + friendId + '' + friendName);

  var userReference = database.ref('/users/'+user).child('friends');

  //add friend to list of user's friends
  userReference.push().set({
    'uid': friendId,
    'picture': friendPic,
    'name': friendName
  });

  var friendReference = database.ref('/users/'+friendId).child('friends');

  friendReference.push().set({
    'uid': user,
    'picture': userPic,
    'name': userName
  });

  var userPendingRequests = database.ref('/users/'+user).child('pendingRequests').orderByChild('uid').equalTo(friendId);
  var friendPendingList = database.ref('/users/'+friendId).child('pendingList').orderByChild('uid').equalTo(user);
  userPendingRequests.once('value', function(snapshot) {
    snapshot.forEach(function(friendSnapshot) {
      friendSnapshot.ref.remove();
    });
    friendPendingList.once('value', function(snapshot) {
      snapshot.forEach(function(userSnapshot) {
        userSnapshot.ref.remove();
      });
    });
  });
  res.send('SUCCESS.');
  console.log('SUCCESS.');
});

//delete a friend
app.post('/deleteFriend', function(req, res) {

  console.log('HTTP post Req.');
  var user = req.body.uid;
  var friendId = req.body.friendId;
  console.log('user:' +user + 'removes friendship with:' + friendId);

  var userReference = database.ref('/users/'+user).child('friends').orderByChild('uid').equalTo(friendId);
  var friendReference = database.ref('/users/'+friendId).child('friends').orderByChild('uid').equalTo(user);
  //delete friend from list of user's friends
  userReference.once('value', function(snapshot) {
   snapshot.forEach(function(userSnapshot) {
       userSnapshot.ref.remove();
     });
     friendReference.once('value', function(snapshot) {
      snapshot.forEach(function(friendSnapshot) {
          friendSnapshot.ref.remove();
        });
     });
   });
   res.send('SUCCESS.');
   console.log('SUCCESS.');
});

// update user preference
app.post('/updateUserPreference', function(req, res) {
  console.log('HTTP POST Req.');
  var user = req.body.uid;
  var preference = req.body.preference;
  var flag = req.body.flag;

  console.log('setting: ' + flag + ' on preference: ' + preference + ' for user: ' + user)
  var userReference = database.ref('/users/'+user).child(preference);
  userReference.set(flag);
  console.log('SUCCESS.');
  res.json('Updated successfully.');
});

// update user profile
app.post('/updateUser', function(req, res) {
  console.log('HTTP POST Req.');
  var user = req.body.uid;
  var userName = req.body.name;
  var userPhone = req.body.phone;
  var userRole = req.body.role;
  var userInfo = req.body.info;
  console.log('Received: ' + user + '' + userName + '' + userPhone + userRole + userInfo);

  var userReference = database.ref('/users/'+user)
  userReference.child('name').set(userName);
  userReference.child('phone_number').set(userPhone);
  userReference.child('role').set(userRole);
  userReference.child('info').set(userInfo);

  userReference.on('value', function(snapshot) {
    console.log(snapshot.val());
    res.json(snapshot.val());
    userReference.off('value');
  },
  function (errorObject) {
    console.log('Read FAIL:' + errorObject.code);
    res.send('Read FAIL:' + errorObject.code);
  });
});

//set server up

var server = app.listen(8080, function() {
  var host = server.address().address;
  var port = server.address().port;

  console.log('Server listening at http://localhost:%s', port);
});
