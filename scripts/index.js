/**
 * Programmatically invoke Open-Saber APIs to populate database
 * and spit out "code -> osid" map
 */

// @ts-check
var request = require("request")
var async = require("async")
var fs = require("fs");
var path = require("path");
var csvjson = require('csvjson');

var invoke_add = function (nIter, payload, callback) {
    var addSuffix = "add"
    var url = baseUrl + "/" + addSuffix
    var headerVars = {
        "Content-Type": "application/json"
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
                console.error(err)
                console.log(" error for " + payload)
                callback(err)
            } else {
                var responseErr = apiResponse["params"]["errmsg"]
                if (responseErr != "") {
                    callback(responseErr, null)
                } else {
                    console.log(" success for " + payload)
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

    for (var itr = 0; itr < arrDynamicData.length; itr++) {
        //async.eachSeries(arrDynamicData, function (oneCSVRow, callback) {
        var completePayload = JSON.parse(JSON.stringify(static_payload))
        var oneCSVRow = JSON.parse(JSON.stringify(arrDynamicData[itr]))
        //console.log("PAYLOAD Complete", JSON.stringify(static_payload))
        //console.log("one row = " + JSON.stringify(oneCSVRow))

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

            // If there are field specific code, set here.
            // if (field === 'phone') {
            //     var phone = parseInt(dataPortion[field]) * 100
            //     dataPortion[field] = phone + (itr + 1)
            // }
        }

        // Any extra column to delete from the csv goes here
        //delete dataPortion.ParentCode

        allPayloads.push(completePayload)
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
    "id": "open-saber.registry.create",
    "request": {
    }
}

// The subject that we have schematized
var entityType = "Student"
addApiPayload.request[entityType] = {}

// The URL where the registry is running
var baseUrl = "http://127.0.0.1:8080"

// Whether you want to run in dryRun mode
// true - API will not be invoked.
// false - API will be invoked.
var dryRun = false

var PARALLEL_LIMIT = 1;
var dataEntities = {}


function populateStudent(cb) {
    var student_tasks = [];
    var studentCSV = csvToJson('data.csv')
    populate_add_tasks(student_tasks, entityType, addApiPayload, studentCSV)
    console.log("Total number of students = " + student_tasks.length)
    execute_tasks(student_tasks, "data.json", cb)
}

var instance1_url = "http://localhost:8080"
var func_array = [populateStudent]

// Execution phase
console.log("Func_Array", func_array);

async.series(func_array, (result, err) => {
    if (err) {
        return (err);
        console.log("Errorrrrr==>", err);
    }
    return result;
    console.log("result = " + result);
})
