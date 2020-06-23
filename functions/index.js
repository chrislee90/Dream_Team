const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

//Firebase initialized

//function triggered when a new user is created on Firebase:
//we want to record a new instance for the user in the Firebase Database!

exports.createUser = functions.auth.user().onCreate((userRecord, context) => {
  return admin.database().ref(`/users/${userRecord.uid}`).set({
    email: userRecord.email, name: userRecord.displayName, phone_number: userRecord.phoneNumber,
    picture: userRecord.photoURL, rating: 0.1, role: 'None', info: '', uid: userRecord.uid, emailPublic: false, phonePublic: false
  });
});

//function triggered when an user receives a new friendship request -> sends a notification to that user!
exports.friendshipRequest = functions.database.ref('/users/{receiverUid}/pendingRequests/{senderUid}')
    .onCreate((change, context) => {
      const receiverUid = context.params.receiverUid;
      const senderUid = context.params.senderUid;
      // get the sender's profile
      const senderProfile = admin.auth().getUser(senderUid)
        .then(function(userRecord) {
          console.log('Successfully fetched user data: ' + userRecord);
          var destinationTopic = receiverUid+'_friends';
          console.log('Notification/Friendshiprequest to: ' + destinationTopic);

          //notification's payload and options
          var payload = {
            data: {
              title: 'New Friendship Request!',
              body: userRecord.displayName+' is requesting your friendship!',
              uid: userRecord.uid,
	      icon: userRecord.photoURL,
              path: '/users/'
            }
          };

          const options = {
            priority: "high",
            timeToLive: 60*60*2
          };

          return admin.messaging().sendToTopic(destinationTopic, payload, options);
        })
        .catch(function(error) {
          console.log('Error fetching user data:', error);
        });
        return 'OK';
    });

//function triggered when an user receives a new friendship request -> sends a notification to that user!
exports.acceptRequest = functions.database.ref('/users/{receiverUid}/friendsList/{senderUid}')
    .onCreate((change, context) => {
      const receiverUid = context.params.receiverUid;
      const senderUid = context.params.senderUid;
      // get the sender's profile
      const senderProfile = admin.auth().getUser(senderUid)
        .then(function(userRecord) {
          console.log('Successfully fetched user data: ' + userRecord);
          var destinationTopic = receiverUid+'_friends';
          console.log('Notification/acceptRequest to: ' + destinationTopic);

          //notification's payload and options
          var payload = {
            data: {
              title: 'Friendship Accepted!',
              body: 'You just became friend with '+userRecord.displayName+'!',
              uid: userRecord.uid,
              icon: userRecord.photoURL,
              path: '/users/'
            }
          };

          const options = {
            priority: "high",
            timeToLive: 60*60*2
          };

          return admin.messaging().sendToTopic(destinationTopic, payload, options);
        })
        .catch(function(error) {
          console.log('Error fetching user data:', error);
        });
        return 'OK';
    });

//function triggered when a friend is invited to the publicMatch
exports.publicMatchInvited = functions.database.ref('/publicMatches/{matchId}/pendingList/{invitedUid}')
    .onCreate((change, context) => {
      const matchId = context.params.matchId;
      const invitedUid = context.params.invitedUid;
      var ownerRef = admin.database().ref('/publicMatches/').child(matchId).child('ownerId');
      var matchRef = admin.database().ref('/publicMatches/').child(matchId).child('name');

      //get match title
      matchRef.once('value', function(snapshot) {
        var matchTitle = snapshot.val();
        //get the owner's uid
        ownerRef.once('value', function(snapshot) {
          var ownerUid = snapshot.val();
          // get the owner's profile
          var ownerProfile = admin.auth().getUser(ownerUid)
            .then(function(userRecord) {
              console.log('Successfully fetched user data: ' + userRecord);
              var destinationTopic = invitedUid+'_matches';
              console.log('Notification to: ' + destinationTopic);
              //notification's payload and options

              var payload = {
                data: {
                  title: 'New match invitation!',
                  body: userRecord.displayName+' has invited you to join the public match '+matchTitle+'!',
                  uid: matchId,
                  icon: userRecord.photoURL,
                  path: 'publicMatches'
                }
              };

              const options = {
                priority: "high",
                timeToLive: 60*60*2
              };

              return admin.messaging().sendToTopic(destinationTopic, payload, options);
            })
            .catch(function(error) {
              console.log('Error fetching user data:', error);
            });
            return 'OK';
          });
        });
    });

//function triggered when a friend is invited to the privateMatch
exports.privateMatchInvited = functions.database.ref('/privateMatches/{matchId}/pendingList/{invitedUid}')
    .onCreate((change, context) => {
      const matchId = context.params.matchId;
      const invitedUid = context.params.invitedUid;
      var ownerRef = admin.database().ref('/privateMatches/').child(matchId).child('ownerId');
      var matchRef = admin.database().ref('/privateMatches/').child(matchId).child('name');

      //get match title
      matchRef.once('value', function(snapshot) {
        const matchTitle = snapshot.val();
        //get the owner's uid
        ownerRef.once('value', function(snapshot) {
          const ownerUid = snapshot.val();
          // get the owner's profile
          var ownerProfile = admin.auth().getUser(ownerUid)
            .then(function(userRecord) {
              console.log('Successfully fetched user data: ' + userRecord);
              var destinationTopic = invitedUid+'_matches';
              console.log('Notification to: ' + destinationTopic);
              //notification's payload and options

              var payload = {
                data: {
                  title: 'New match invitation!',
                  body: userRecord.displayName+' has invited you to join the private match '+matchTitle+'!',
                  uid: matchId,
                  icon: userRecord.photoURL,
                  path: 'privateMatches'
                }
              };

              const options = {
                priority: "high",
                timeToLive: 60*60*2
              };

              return admin.messaging().sendToTopic(destinationTopic, payload, options);
            })
            .catch(function(error) {
              console.log('Error fetching user data:', error);
            });
            return 'OK';
            });
          });
      });

//function triggered when a friend is added as participant the publicMatch
exports.publicMatchAdd = functions.database.ref('/publicMatches/{matchId}/participants/{newPlayerUid}')
    .onCreate((change, context) => {
      const matchId = context.params.matchId;
      const newPlayerUid = context.params.newPlayerUid;
      var ownerRef = admin.database().ref('/publicMatches/').child(matchId).child('ownerId');
      var matchRef = admin.database().ref('/publicMatches/').child(matchId).child('name');
      var newPlayerRef = admin.database().ref('/users/').child(newPlayerUid);

      //get match title
      matchRef.once('value', function(snapshot) {
        const matchTitle = snapshot.val();
        //get the owner's uid
        ownerRef.once('value', function(snapshot) {
          const ownerUid = snapshot.val();
          //get the new player's name
          newPlayerRef.child('name').once('value', function(snapshot) {
            const newPlayerName = snapshot.val();
            //get the new player's photoURL
            newPlayerRef.child('picture').once('value', function(snapshot) {
              const newPlayerPhoto = snapshot.val();
              // get the owner's profile
              var ownerProfile = admin.auth().getUser(ownerUid)
                .then(function(userRecord) {
                  console.log('Successfully fetched user data: ' + userRecord);
                  var ownerTopic = ownerUid+'_matches';
                  var newPlayerTopic = newPlayerUid+'_matches';
                  console.log('Notification to: ' + ownerTopic + ' ' + newPlayerTopic);
                  //notification's payload and options

                  var payloadOwner = {
                    data: {
                      title: 'Invitation Accepted!',
                      body: newPlayerName+' has joined your public match '+matchTitle+'!',
                      uid: matchId,
                      icon: newPlayerPhoto,
                      path: 'publicMatches'
                    }
                  };

                  var payloadNewPlayer = {
                    data: {
                      title: 'New match joined!',
                      body: 'You just joined a new public match: '+matchTitle+' created by '+userRecord.displayName+'!',
                      uid: matchId,
                      icon: userRecord.photoURL,
                      path: 'publicMatches'
                    }
                  };

                  const options = {
                    priority: "high",
                    timeToLive: 60*60*2
                  };

                  if(newPlayerUid !== ownerUid) {
                    admin.messaging().sendToTopic(ownerTopic, payloadOwner, options);
                    admin.messaging().sendToTopic(newPlayerTopic, payloadNewPlayer, options);
                  }
                  return 'OK';
                })
                .catch(function(error) {
                  console.log('Error fetching user data:', error);
                });
                return 'OK';
              });
            });
          });
        });
    });

//function triggered when a friend is added as participant to the privateMatch
exports.privateMatchAdd = functions.database.ref('/privateMatches/{matchId}/participants/{newPlayerUid}')
    .onCreate((change, context) => {
      const matchId = context.params.matchId;
      const newPlayerUid = context.params.newPlayerUid;
      var ownerRef = admin.database().ref('/privateMatches/').child(matchId).child('ownerId');
      var matchRef = admin.database().ref('/privateMatches/').child(matchId).child('name');
      var newPlayerRef = admin.database().ref('/users/').child(newPlayerUid);

      //get match title
      matchRef.once('value', function(snapshot) {
        const matchTitle = snapshot.val();
        //get the owner's uid
        ownerRef.once('value', function(snapshot) {
          const ownerUid = snapshot.val();
          //get the new player's name
          newPlayerRef.child('name').once('value', function(snapshot) {
            const newPlayerName = snapshot.val();
            //get the new player's photoURL
            newPlayerRef.child('picture').once('value', function(snapshot) {
              const newPlayerPhoto = snapshot.val();
              // get the owner's profile
              var ownerProfile = admin.auth().getUser(ownerUid)
                .then(function(userRecord) {
                  console.log('Successfully fetched user data: ' + userRecord);
                  var ownerTopic = ownerUid+'_matches';
                  var newPlayerTopic = newPlayerUid+'_matches';
                  console.log('Notification to: ' + ownerTopic + ' ' + newPlayerTopic);
                  //notification's payload and options

                  var payloadOwner = {
                    data: {
                      title: 'Invitation Accepted!',
                      body: newPlayerName+' has joined your private match '+matchTitle+'!',
                      uid: matchId,
                      icon: newPlayerPhoto,
                      path: 'privateMatches'
                    }
                  };

                  var payloadNewPlayer = {
                    data: {
                      title: 'New match joined!',
                      body: 'You just joined a new private match: '+matchTitle+' created by '+userRecord.displayName+'!',
                      uid: matchId,
                      icon: userRecord.photoURL,
                      path: 'privateMatches'
                    }
                  };

                  const options = {
                    priority: "high",
                    timeToLive: 60*60*2
                  };

                  if(newPlayerUid !== ownerUid) {
                    admin.messaging().sendToTopic(ownerTopic, payloadOwner, options);
                    admin.messaging().sendToTopic(newPlayerTopic, payloadNewPlayer, options);
                  }
                  return 'OK';
                })
                .catch(function(error) {
                  console.log('Error fetching user data:', error);
                });
                return 'OK';
              });
            });
          });
        });
    });

//function triggered when a user requests participation to the publicMatch
exports.publicMatchRequest = functions.database.ref('/publicMatches/{matchId}/pendingRequests/{requestingUid}')
    .onCreate((change, context) => {
      const matchId = context.params.matchId;
      const requestingUid = context.params.requestingUid;
      var ownerRef = admin.database().ref('/publicMatches/').child(matchId).child('ownerId');
      var matchRef = admin.database().ref('/publicMatches/').child(matchId).child('name');
      var requestingRef = admin.database().ref('/users/').child(requestingUid);

      //get match title
      matchRef.once('value', function(snapshot) {
        var matchTitle = snapshot.val();
        //get the owner's uid
        ownerRef.once('value', function(snapshot) {
          var ownerUid = snapshot.val();
          requestingRef.child('name').once('value', function(snapshot) {
            var requestingName = snapshot.val();
            requestingRef.child('picture').once('value', function(snapshot) {
              var requestingPhoto = snapshot.val();
              // get the owner's profile
              var ownerProfile = admin.auth().getUser(ownerUid)
                .then(function(userRecord) {
                  console.log('Successfully fetched user data: ' + userRecord);
                  var destinationTopic = ownerUid+'_matches';
                  console.log('Notification to: ' + destinationTopic);
                  //notification's payload and options

                  var payload = {
                    data: {
                      title: 'New participation request!',
                      body: requestingName+' has requested to join your public match '+matchTitle+'!',
                      uid: matchId,
                      icon: requestingPhoto,
                      path: 'publicMatches'
                    }
                  };

                  const options = {
                    priority: "high",
                    timeToLive: 60*60*2
                  };

                  return admin.messaging().sendToTopic(destinationTopic, payload, options);
                })
                .catch(function(error) {
                  console.log('Error fetching user data:', error);
                });
                return 'OK';
              });
            });
        });
      });
    });

//function triggered when a user requests participation to the privateMatch (on the cange -> public private)
exports.privateMatchRequest = functions.database.ref('/privateMatches/{matchId}/pendingRequests/{requestingUid}')
    .onCreate((change, context) => {
      const matchId = context.params.matchId;
      const requestingUid = context.params.requestingUid;
      var ownerRef = admin.database().ref('/privateMatches/').child(matchId).child('ownerId');
      var matchRef = admin.database().ref('/privateMatches/').child(matchId).child('name');
      var requestingRef = admin.database().ref('/users/').child(requestingUid);

      //get match title
      matchRef.once('value', function(snapshot) {
        var matchTitle = snapshot.val();
        //get the owner's uid
        ownerRef.once('value', function(snapshot) {
          var ownerUid = snapshot.val();
          requestingRef.child('name').once('value', function(snapshot) {
            var requestingName = snapshot.val();
            requestingRef.child('picture').once('value', function(snapshot) {
              var requestingPhoto = snapshot.val();
              // get the owner's profile
              var ownerProfile = admin.auth().getUser(ownerUid)
                .then(function(userRecord) {
                  console.log('Successfully fetched user data: ' + userRecord);
                  var destinationTopic = ownerUid+'_matches';
                  console.log('Notification to: ' + destinationTopic);
                  //notification's payload and options

                  var payload = {
                    data: {
                      title: 'Participation request!',
                      body: requestingName+' is still waiting for your decision on '+matchTitle+'!',
                      uid: matchId,
                      icon: requestingPhoto,
                      path: 'privateMatches'
                    }
                  };

                  const options = {
                    priority: "high",
                    timeToLive: 60*60*2
                  };

                  return admin.messaging().sendToTopic(destinationTopic, payload, options);
                })
                .catch(function(error) {
                  console.log('Error fetching user data:', error);
                });
                return 'OK';
              });
            });
        });
      });
    });

//function triggered when a user's request to publicMatch is rejected by owner
exports.publicMatchRequestRejected = functions.database.ref('/publicMatches/{matchId}/rejected/{requestingUid}')
    .onCreate((change, context) => {
      const matchId = context.params.matchId;
      const requestingUid = context.params.requestingUid;
      var ownerRef = admin.database().ref('/publicMatches/').child(matchId).child('ownerId');
      var matchRef = admin.database().ref('/publicMatches/').child(matchId).child('name');

      //get match title
      matchRef.once('value', function(snapshot) {
        var matchTitle = snapshot.val();
        //get the owner's uid
        ownerRef.once('value', function(snapshot) {
          var ownerUid = snapshot.val();
          // get the owner's profile
          var ownerProfile = admin.auth().getUser(ownerUid)
            .then(function(userRecord) {
              console.log('Successfully fetched user data: ' + userRecord);
              var destinationTopic = requestingUid+'_matches';
              console.log('Notification to: ' + destinationTopic);
              //notification's payload and options

              var payload = {
                data: {
                  title: 'Join match request rejected',
                  body: userRecord.displayName+' has rejected your request to join '+matchTitle+'.',
                  uid: matchId,
                  icon: userRecord.photoURL,
                  path: 'publicMatches'
                }
              };

              const options = {
                priority: "high",
                timeToLive: 60*60*2
              };

              return admin.messaging().sendToTopic(destinationTopic, payload, options);
            })
            .catch(function(error) {
              console.log('Error fetching user data:', error);
            });
            return 'OK';
          });
        });
    });

//function triggered when owner's invitation to publicMatch is refused by user
exports.publicMatchInvitedRefused = functions.database.ref('/publicMatches/{matchId}/refused/{refusingUid}')
    .onCreate((change, context) => {
      const matchId = context.params.matchId;
      const refusingUid = context.params.requestingUid;
      var ownerRef = admin.database().ref('/publicMatches/').child(matchId).child('ownerId');
      var matchRef = admin.database().ref('/publicMatches/').child(matchId).child('name');
      var refusingRef = admin.database().ref('/users/').child(refusingUid);

      //get match title
      matchRef.once('value', function(snapshot) {
        var matchTitle = snapshot.val();
        //get the owner's uid
        ownerRef.once('value', function(snapshot) {
          var ownerUid = snapshot.val();
          refusingRef.child('name').once('value', function(snapshot) {
            var refusingName = snapshot.val();
            refusingRef.child('picture').once('value', function(snapshot) {
              var refusingPhoto = snapshot.val();
              // get the owner's profile
              var ownerProfile = admin.auth().getUser(ownerUid)
                .then(function(userRecord) {
                  console.log('Successfully fetched user data: ' + userRecord);
                  var destinationTopic = ownerUid+'_matches';
                  console.log('Notification to: ' + destinationTopic);
                  //notification's payload and options

                  var payload = {
                    data: {
                      title: 'Match invitation refused!',
                      body: refusingName+' has refused to join your public match '+matchTitle+'.',
                      uid: matchId,
                      icon: refusingPhoto,
                      path: 'publicMatches'
                    }
                  };

                  const options = {
                    priority: "high",
                    timeToLive: 60*60*2
                  };

                  return admin.messaging().sendToTopic(destinationTopic, payload, options);
                })
                .catch(function(error) {
                  console.log('Error fetching user data:', error);
                });
                return 'OK';
              });
            });
        });
      });
    });

//function triggered when owner's invitation to privateMatch is refused by user
exports.privateMatchInvitedRefused = functions.database.ref('/privateMatches/{matchId}/refused/{refusingUid}')
    .onCreate((change, context) => {
      const matchId = context.params.matchId;
      const refusingUid = context.params.requestingUid;
      var ownerRef = admin.database().ref('/privateMatches/').child(matchId).child('ownerId');
      var matchRef = admin.database().ref('/privateMatches/').child(matchId).child('name');
      var refusingRef = admin.database().ref('/users/').child(refusingUid);

      //get match title
      matchRef.once('value', function(snapshot) {
        var matchTitle = snapshot.val();
        //get the owner's uid
        ownerRef.once('value', function(snapshot) {
          var ownerUid = snapshot.val();
          refusingRef.child('name').once('value', function(snapshot) {
            var refusingName = snapshot.val();
            refusingRef.child('picture').once('value', function(snapshot) {
              var refusingPhoto = snapshot.val();
              // get the owner's profile
              var ownerProfile = admin.auth().getUser(ownerUid)
                .then(function(userRecord) {
                  console.log('Successfully fetched user data: ' + userRecord);
                  var destinationTopic = ownerUid+'_matches';
                  console.log('Notification to: ' + destinationTopic);
                  //notification's payload and options

                  var payload = {
                    data: {
                      title: 'Match invitation refused!',
                      body: refusingName+' has refused to join your private match '+matchTitle+'.',
                      uid: matchId,
                      icon: refusingPhoto,
                      path: 'privateMatches'
                    }
                  };

                  const options = {
                    priority: "high",
                    timeToLive: 60*60*2
                  };

                  return admin.messaging().sendToTopic(destinationTopic, payload, options);
                })
                .catch(function(error) {
                  console.log('Error fetching user data:', error);
                });
                return 'OK';
              });
            });
        });
      });
    });

//function triggered when an user is removed from participants in publicMatch
exports.publicMatchDeleteParticipant = functions.database.ref('/publicMatches/{matchId}/removed/{deletedUid}')
    .onCreate((change, context) => {
      const matchId = context.params.matchId;
      const deletedUid = context.params.deletedUid;
      var ownerRef = admin.database().ref('/publicMatches/').child(matchId).child('ownerId');
      var matchRef = admin.database().ref('/publicMatches/').child(matchId).child('name');
      var deletedRef = admin.database().ref('/users/').child(deletedUid);

      //get match title
      matchRef.once('value', function(snapshot) {
        const matchTitle = snapshot.val();
        //get the owner's uid
        ownerRef.once('value', function(snapshot) {
          const ownerUid = snapshot.val();
          //get the new player's name
          deletedRef.child('name').once('value', function(snapshot) {
            const deletedName = snapshot.val();
            //get the new player's photoURL
            deletedRef.child('picture').once('value', function(snapshot) {
              const deletedPhoto = snapshot.val();
              // get the owner's profile
              var ownerProfile = admin.auth().getUser(ownerUid)
                .then(function(userRecord) {
                  console.log('Successfully fetched user data: ' + userRecord);
                  var ownerTopic = ownerUid+'_matches';
                  var deletedTopic = deletedUid+'_matches';
                  console.log('Notification to: ' + ownerTopic + ' ' + deletedTopic);
                  //notification's payload and options

                  var payloadOwner = {
                    data: {
                      title: 'Participant Left!',
                      body: deletedName+' has been removed from your public match '+matchTitle+'!',
                      uid: matchId,
                      icon: deletedPhoto,
                      path: 'publicMatches'
                    }
                  };

                  var payloadDeleted = {
                    data: {
                      title: 'Left Match!',
                      body: 'You have been removed from match: '+matchTitle+' created by '+userRecord.displayName+'!',
                      uid: matchId,
                      icon: userRecord.photoURL,
                      path: 'publicMatches'
                    }
                  };

                  const options = {
                    priority: "high",
                    timeToLive: 60*60*2
                  };

                  if(deletedUid !== ownerUid) {
                    admin.messaging().sendToTopic(ownerTopic, payloadOwner, options);
                    admin.messaging().sendToTopic(deletedTopic, payloadDeleted, options);
                  }
                  return 'OK';
                })
                .catch(function(error) {
                  console.log('Error fetching user data:', error);
                });
                return 'OK';
              });
            });
          });
        });
    });

//function triggered when an user is removed from participants in privateMatch
exports.privateMatchDeleteParticipant = functions.database.ref('/privateMatches/{matchId}/removed/{deletedUid}')
    .onCreate((change, context) => {
      const matchId = context.params.matchId;
      const deletedUid = context.params.deletedUid;
      var ownerRef = admin.database().ref('/privateMatches/').child(matchId).child('ownerId');
      var matchRef = admin.database().ref('/privateMatches/').child(matchId).child('name');
      var deletedRef = admin.database().ref('/users/').child(deletedUid);

      //get match title
      matchRef.once('value', function(snapshot) {
        const matchTitle = snapshot.val();
        //get the owner's uid
        ownerRef.once('value', function(snapshot) {
          const ownerUid = snapshot.val();
          //get the new player's name
          deletedRef.child('name').once('value', function(snapshot) {
            const deletedName = snapshot.val();
            //get the new player's photoURL
            deletedRef.child('picture').once('value', function(snapshot) {
              const deletedPhoto = snapshot.val();
              // get the owner's profile
              var ownerProfile = admin.auth().getUser(ownerUid)
                .then(function(userRecord) {
                  console.log('Successfully fetched user data: ' + userRecord);
                  var ownerTopic = ownerUid+'_matches';
                  var deletedTopic = deletedUid+'_matches';
                  var puppet = "cantsee"
                  console.log('Notification to: ' + ownerTopic + ' ' + deletedTopic);
                  //notification's payload and options

                  var payloadOwner = {
                    data: {
                      title: 'Participant Left!',
                      body: deletedName+' has been removed from your private match '+matchTitle+'!',
                      uid: matchId,
                      icon: deletedPhoto,
                      path: 'privateMatches'
                    }
                  };

                  var payloadDeleted = {
                    data: {
                      title: 'Left Match!',
                      body: 'You have been removed from match: '+matchTitle+' created by '+userRecord.displayName+'!',
                      uid: puppet,
                      icon: userRecord.photoURL,
                      path: 'privateMatches'
                    }
                  };

                  const options = {
                    priority: "high",
                    timeToLive: 60*60*2
                  };

                  if(deletedUid !== ownerUid) {
                    admin.messaging().sendToTopic(ownerTopic, payloadOwner, options);
                    admin.messaging().sendToTopic(deletedTopic, payloadDeleted, options);
                  }
                  return 'OK';
                })
                .catch(function(error) {
                  console.log('Error fetching user data:', error);
                });
                return 'OK';
              });
            });
          });
        });
    });

//function triggered when a public event is cancelled/privacy-modified
exports.publicMatchCancel= functions.database.ref('/publicMatches/{matchId}/isConfirmed/')
    .onWrite((change, context) => {
      const matchId = context.params.matchId;
      var ownerRef = admin.database().ref('/publicMatches/').child(matchId).child('ownerId');
      var valueRef = admin.database().ref('/publicMatches/').child(matchId).child('isConfirmed');
      var matchRef = admin.database().ref('/publicMatches/').child(matchId).child('name');
      var partRef = admin.database().ref('/publicMatches/').child(matchId).child('participants');
      var reqRef = admin.database().ref('/publicMatches/').child(matchId).child('pendingRequests');
      var penRef = admin.database().ref('/publicMatches/').child(matchId).child('pendingList');

      //get match title
      matchRef.once('value', function(snapshot) {
        const matchTitle = snapshot.val();
        //get the value
        valueRef.once('value', function(snapshot) {
          const value = snapshot.val()
        //get the owner's uid
        ownerRef.once('value', function(snapshot) {
          const ownerUid = snapshot.val();
          // get the owner's profile
          var ownerProfile = admin.auth().getUser(ownerUid)
            .then(function(userRecord) {
              console.log('Successfully fetched user data: ' + userRecord);
              console.log('CANCEL notification for: ' + matchTitle);

              //notification's payload and options
              var payloadCancelled = {
                data: {
                  title: 'Match Cancelled!',
                  body: userRecord.displayName+' has cancelled his public match '+matchTitle+'!',
                  uid: matchId,
                  icon: userRecord.photoURL,
                  path: 'publicMatches'
                }
              };

              var payloadChanged = {
                data: {
                  title: 'Match Privacy Updated!',
                  body: userRecord.displayName+' has made match '+matchTitle+' public!',
                  uid: matchId,
                  icon: userRecord.photoURL,
                  path: 'publicMatches'
                }
              };

              const options = {
                priority: "high",
                timeToLive: 60*60*2
              };

              partRef.once('value', function(snapshot) {
                if(snapshot.exists()){
                  snapshot.forEach(function(childSnapshot) {
                    var partId = childSnapshot.val().uid;
                    console.log("partID: "+partId);
                    if(partId !== ownerUid) {
                      if(value === false) {
                        admin.messaging().sendToTopic(partId+'_matches', payloadCancelled, options);
                      } else {
                        admin.messaging().sendToTopic(partId+'_matches', payloadChanged, options);
                      }
                    }
                  });
                }
              });

              reqRef.once('value', function(snapshot) {
                if(snapshot.exists()){
                  snapshot.forEach(function(childSnapshot) {
                    var reqId = childSnapshot.val().uid;
                    console.log("reqID: "+reqId);
                    if(value === false){
                      admin.messaging().sendToTopic(reqId+'_matches', payloadCancelled, options);
                    } else {
                      admin.messaging().sendToTopic(reqId+'_matches', payloadChanged, options);
                    }
                  });
                }
              });

              penRef.once('value', function(snapshot) {
                if(snapshot.exists()){
                  snapshot.forEach(function(childSnapshot) {
                    var penId = childSnapshot.val().uid;
                    console.log("penID: "+penId);
                    if(value === false){
                      admin.messaging().sendToTopic(penId+'_matches', payloadCancelled, options);
                    } else {
                      admin.messaging().sendToTopic(penId+'_matches', payloadChanged, options);
                    }
                  });
                }
              });

              return 'OK';
            })
            .catch(function(error) {
              console.log('Error fetching user data:', error);
            });
            return 'OK';
          });
        });
      });
    });

//function triggered when a private event is cancelled/privacy-modified
exports.privateMatchCancel= functions.database.ref('/privateMatches/{matchId}/isConfirmed/')
    .onWrite((change, context) => {
      const matchId = context.params.matchId;
      var ownerRef = admin.database().ref('/privateMatches/').child(matchId).child('ownerId');
      var valueRef = admin.database().ref('/privateMatches/').child(matchId).child('isConfirmed');
      var matchRef = admin.database().ref('/privateMatches/').child(matchId).child('name');
      var partRef = admin.database().ref('/privateMatches/').child(matchId).child('participants');
      var reqRef = admin.database().ref('/privateMatches/').child(matchId).child('pendingRequests');
      var penRef = admin.database().ref('/privateMatches/').child(matchId).child('pendingList');

      //get match title
      matchRef.once('value', function(snapshot) {
        const matchTitle = snapshot.val();
        //get the value
        valueRef.once('value', function(snapshot) {
          const value = snapshot.val()
        //get the owner's uid
        ownerRef.once('value', function(snapshot) {
          const ownerUid = snapshot.val();
          // get the owner's profile
          var ownerProfile = admin.auth().getUser(ownerUid)
            .then(function(userRecord) {
              console.log('Successfully fetched user data: ' + userRecord);
              console.log('CANCEL notification for: ' + matchTitle);

              //notification's payload and options
              var payloadCancelled = {
                data: {
                  title: 'Match Cancelled!',
                  body: userRecord.displayName+' has cancelled his private match '+matchTitle+'!',
                  uid: matchId,
                  icon: userRecord.photoURL,
                  path: 'privateMatches'
                }
              };

              var payloadChanged = {
                data: {
                  title: 'Match Privacy Updated!',
                  body: userRecord.displayName+' has made match '+matchTitle+' private!',
                  uid: matchId,
                  icon: userRecord.photoURL,
                  path: 'publicMatches'
                }
              };

              const options = {
                priority: "high",
                timeToLive: 60*60*2
              };

              partRef.once('value', function(snapshot) {
                if(snapshot.exists()){
                  snapshot.forEach(function(childSnapshot) {
                    var partId = childSnapshot.val().uid;
                    console.log("partID: "+partId);
                    if(partId !== ownerUid) {
                      if(value === false) {
                        admin.messaging().sendToTopic(partId+'_matches', payloadCancelled, options);
                      } else {
                        admin.messaging().sendToTopic(partId+'_matches', payloadChanged, options);
                      }
                    }
                  });
                }
              });

              reqRef.once('value', function(snapshot) {
                if(snapshot.exists()){
                  snapshot.forEach(function(childSnapshot) {
                    var reqId = childSnapshot.val().uid;
                    console.log("reqID: "+reqId);
                    if(value === false){
                      admin.messaging().sendToTopic(reqId+'_matches', payloadCancelled, options);
                    } else {
                      admin.messaging().sendToTopic(reqId+'_matches', payloadChanged, options);
                    }
                  });
                }
              });

              penRef.once('value', function(snapshot) {
                if(snapshot.exists()){
                  snapshot.forEach(function(childSnapshot) {
                    var penId = childSnapshot.val().uid;
                    console.log("penID: "+penId);
                    if(value === false){
                      admin.messaging().sendToTopic(penId+'_matches', payloadCancelled, options);
                    } else {
                      admin.messaging().sendToTopic(penId+'_matches', payloadChanged, options);
                    }
                  });
                }
              });

              return 'OK';
            })
            .catch(function(error) {
              console.log('Error fetching user data:', error);
            });
            return 'OK';
          });
        });
      });
    });
