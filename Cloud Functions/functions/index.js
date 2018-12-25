const functions = require('firebase-functions');
const admin = require('firebase-admin');

admin.initializeApp();

const firestore = admin.firestore();
const settings = {timestampsInSnapshots: true};
firestore.settings(settings);
const storage = admin.storage();

exports.exportData = functions.https.onCall((data) => {
    const uid = data.uid;
    const exportData = {};

    const firestorePromise = exportFirestoreData(uid).then((firestoreData) => {
        exportData.firestore = firestoreData;
        return exportData;
    });

    return Promise.all([firestorePromise])
            .then(() => {
                console.log(`Success! Completed export for user ${uid}.`);
                temp = JSON.stringify(exportData);
                // console.log(`Data: ${temp}`);   
                return uploadToStorage(uid, exportData);
            })
            .then(() => {
                console.log("File uploaded to Firebase Storage."); 
                return {
                    status: "Data loaded from firestore",
                    exportData: true
                }
            }).catch((error) => {
                console.error(`Error encountered while getting all the promises: ${error}`);
            });

});

const exportFirestoreData = (uid) => {
    const path = {
        collection: "AppData",
        doc: uid,
        subCollections: ["AccelerometerReadings", "GyroscopeReadings"]
    };
    const promises = [];
    const exportData = {};
    const myCollection = path.collection;
    const myDoc = path.doc;
    const subCollections = path.subCollections[0];
    const accCollection = subCollections;
    const accData = {};
    // console.log(`Sub Collection: ${accCollection}`);
    const query = firestore.collection(myCollection).doc(myDoc).collection(accCollection);
    promises.push(query.orderBy("createdAtMillis").get().then(function (querySnapshot){
        var len  = 0;
        querySnapshot.forEach((doc) => {
            accData[`${doc.id}`] = doc.data();
            len++;
        });
        console.log(`Collected ${len} records.`);
        exportData[`${accCollection}`] = accData;
        return exportData;
    }).catch((error) => {
        console.error('Error encountered while exporting from firestore: ', error);
    }));
    return Promise.all(promises).then(() => exportData);
};

const uploadToStorage = (uid, exportData) => {
    const json = JSON.stringify(exportData, null, 4);
    // console.log(`JSON Data: ${json}`);
    const bucket = storage.bucket("walkingpattern-71969.appspot.com")
    const file = bucket.file(`exportData/${uid}/export.json`);

    return file.save(json);
}

// // Create and Deploy Your First Cloud Functions
// // https://firebase.google.com/docs/functions/write-firebase-functions
//
// exports.helloWorld = functions.https.onRequest((request, response) => {
//  response.send("Hello from Firebase!");
// });
