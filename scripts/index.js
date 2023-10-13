

/**
 * Programmatically invoke APIs to populate database
 * 
 */

var request = require("request")
var async = require("async")
var fs = require("fs");
var path = require("path");
var csvjson = require('csvjson');
var _ = require('lodash');

var invoke_add = function (nIter, payload, callback) {
    var addSuffix = "register/users"
    var url = baseUrl + "/" + addSuffix
    var headerVars = {
        "Content-Type": "application/json",
        // "Authorization": "Bearer ",
        "x-authenticated-user-token": ""
    }

    if (dryRun) {
        console.log("#" + nIter + " DryRun: Will invoke " + url + " with payload " + payload)
        callback(null, nIter)
    } else {
        //console.log("#" + nIter + " Invoking " + url + " with payload " + payload)
    
        request(url, {
            method: "POST",
            body: payload,
            headers: headerVars
        }, function (err, response, body) {
            //console.log("This is the api response " + JSON.stringify(body))
            var apiResponse = JSON.parse(body)
            if (err) {
                console.error(err?.message)
                console.error(" error for " + payload)
                callback(err)
            } else {
                var responseErr = apiResponse
                if (responseErr != "") {
                    callback(responseErr, null)
                } else {
                    console.log(" success for " + payload, " " + apiResponse.result)
                    callback(null, apiResponse.result)
                }
            }
        })
    }
}

var addToArr = function (arr, val, cb) {
    arr.push(val)
    cb()
}

/**
 * 
 */
var populate_add_tasks = function (tasks, entityType, static_payload, arrDynamicData, someEntity) {
    var allPayloads = []
    //to match keys of schema and csv
    // const arrayWithValidKeys = [];
    // arrDynamicData.map(item => {
    //     arrayWithValidKeys.push(
    //         _.mapKeys(item, (value, key) => {
    //             let newKey = keys[key];
    //             if (newKey)
    //                 return newKey;
    //             else return key
    //         })
    //     )
    // });
    for (var itr = 0; itr < arrDynamicData.length; itr++) {
        //async.eachSeries(arrDynamicData, function (oneCSVRow, callback) {
        var completePayload = JSON.parse(JSON.stringify(static_payload))
        var oneCSVRow = JSON.parse(JSON.stringify(arrDynamicData[itr]))
        //console.log("PAYLOAD Complete", JSON.stringify(static_payload))
        //console.log("one row = " + JSON.stringify(oneCSVRow))
        completePayload["request"][entityType] ={}

        var attrsMerged = Object.assign(completePayload["request"][entityType], oneCSVRow)
        completePayload["request"][entityType] = attrsMerged

        //console.log(itr + " - payload = " + JSON.stringify(completePayload))

        var dataPortion = completePayload["request"][entityType]
        for (var field in dataPortion) {
            var fieldVal = dataPortion[field]

            if (fieldVal.indexOf("[") != -1) {
                var myArr = new Array()
                var individualItems = fieldVal.replace(/\[|\]/g, "")
                //console.log("Expect [] to be removed " + JSON.stringify(individualItems) + " flag = " + individualItems.indexOf(","));
                if (individualItems.indexOf(",") != -1) {
                    // console.log("Array contains multiple values")
                    // More than one item
                    var arrItems = individualItems.split(",")
                    arrItems.forEach(element => {
                        myArr.push(element);
                    });
                    console.log("Array", myArr);
                } else {
                    //console.log("Just one item in the array for " + field + " = " + individualItems)

                    if (parseInt(individualItems)) {
                        //console.log("is integer")
                        myArr.push(parseInt(individualItems))
                    } else {
                        myArr.push(individualItems)
                    }
                }
                dataPortion[field] = myArr
            }
            if (field === 'isActive') {
                if (dataPortion[field].toUpperCase() === 'Yes'.toUpperCase() || !dataPortion['endDate']) {

                    dataPortion['isOnboarded'] = true;
                    dataPortion['isActive'] = true;
                } else {
                    dataPortion['isActive'] = false;
                    dataPortion['isOnboarded'] = false;

                }
            }
            // If WorkLocation and working Style is empty , adding value "Unknown"
            if (field === 'projectName' || field === 'subProjectName' ) {
                if(dataPortion[field] === ''){
                    dataPortion[field]="Unknown"
                }else if(dataPortion[field].toUpperCase() === 'DIKSHA'){
                    dataPortion[field]="DIKSHA"
                }else if(dataPortion['projectName'] === 'Plugin'){
                    dataPortion[field]="Plugins"
                }
            }

            if ( field === 'proposedBilling') {
                if(dataPortion[field] === 'Non DIKSHA'){
                    dataPortion[field]="Non Diksha"
                }
            }

            if (field === 'role' || field === 'proposedBilling') {
                if(dataPortion[field] === ''){
                    dataPortion[field]="Unknown"
                }
            }

            if (field === 'workLocation' || field === 'workingStyle' ) {
                if( dataPortion[field] === ''){
                     dataPortion[field]="Unknown"
                }else if(dataPortion[field] === "Ekstep"){
                    dataPortion[field]="EkStep"
                }
            }

            
            // If there are field specific code, set here.
            if (field === 'isInKronos') {
                // Yes-No fields.
                if (dataPortion[field] !== undefined &&
                     (dataPortion[field].toUpperCase() === 'No'.toUpperCase() || dataPortion[field].toUpperCase() === 'Inactive'.toUpperCase() || dataPortion[field]==='')) {
                    dataPortion[field] = false
                } else {
                    dataPortion[field] = true
                }
            }
            if (field === 'repoAccess' || field === 'slackAccess') {
                // Yes-No fields.
                if(dataPortion[field] !== ''){
                    if (dataPortion[field].toUpperCase() === 'Added to ES'.toUpperCase()) {
                        dataPortion[field] = "ES"
                    } else if (dataPortion[field].toUpperCase() === 'Added to Both'.toUpperCase()) {
                        dataPortion[field] = "Both"
                    } else if (dataPortion[field].toUpperCase() === 'Added to SB'.toUpperCase()) {
                        dataPortion[field] = 'SB'
                    }
                }else{
                    dataPortion[field]="Unknown"
                }
            }
           
            if (field === 'startDate' || field === 'endDate') {
                if (dataPortion[field] !== "") {
                    var newdate = dataPortion[field].split("-").reverse().join("-");
                    dataPortion[field] = newdate;
                }
                if (dataPortion[field] === "") {
                    delete dataPortion[field];
                }
            }

        }

        // Any extra column to delete from the csv goes here
        //delete dataPortion.ParentCode
        delete dataPortion["Edu Stack - Layer"]
        delete dataPortion["Edu Stack - Component"]
        delete dataPortion[""]
        delete dataPortion["Experience"]
        delete dataPortion["Role"]
        delete dataPortion["Sub Project"]


        console.log(completePayload)
        if(dataPortion["orgName"]!== "" && dataPortion["name"]!== ""){
             allPayloads.push(completePayload)
        }
    }

    //console.log("Lengths of tasks = " + arrDynamicData.length + " and " + allPayloads.length)
    //console.log(JSON.stringify(allPayloads))
    async.forEachOf(allPayloads, function (onePayload, nIter, callback) {
        tasks.push(
            (cb) => invoke_add(nIter, JSON.stringify(onePayload), function (err, data) {
                var returnData = JSON.stringify(err)
                if (err != null) {
                    console.log("Return data = " + returnData + " for payload " + JSON.stringify(onePayload) );
                }
                // Do not cascade the error - fail for certain rows, but don't stop processing.
                cb(null, data)
            })
        )
        callback()
    })
}

/**
 * Executes all the populated tasks in parallel.
 */
var execute_tasks = function (tasks, fileName, cb) {
    //async.parallelLimit(tasks, PARALLEL_LIMIT, function (err, callback) {
    async.series(tasks, function (err, callback) {
        if (!err) {
            console.log("Executed tasks")
            cb(null)
        } else {
            console.error(err)
            console.log("One or more errors occurred.")
            cb(err)
        }
    })
}

var options = {
    delimiter: ',', // optional
    quote: '"' // optional
};

var csvToJson = function (csvFileName) {
    var data = fs.readFileSync(path.join(__dirname, csvFileName), { encoding: 'utf8' });
    const jsonObject = csvjson.toObject(data, options);
    //console.log("JSON Object", jsonObject);
    return jsonObject;
}

// This is the default payload
var addApiPayload = {
    "id": "sunbird-rc.registry.create",
    "request": {
    }
}

// The subject that we have schematized
var entityType = "Employee"
addApiPayload.request[entityType] = {}


// The URL where the registry is running
var baseUrl = "http://localhost:9081"

// Whether you want to run in dryRun mode
// true - API will not be invoked.
// false - API will be invoked.
var dryRun = true

var PARALLEL_LIMIT = 1;
var dataEntities = {}


function populateData(cb) {
    var data_tasks = [];
    var dataCSV = csvToJson('data_ek.csv')
    populate_add_tasks(data_tasks, entityType, addApiPayload, dataCSV)
    console.log("Total number of data records = " + data_tasks.length)
    execute_tasks(data_tasks, "data.json", cb)
}

//start pupulating data
populateData(function (err, result) {
    if (err) {
        return (err);
        console.error("Errorrrrr==>", err);
    }
    console.log("Finished successfully");
    return result;
})
