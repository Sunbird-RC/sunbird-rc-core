const axios = require('axios');
let token = '';
let osids = [""];
osids.forEach(osid => {


    let config = {
        method: 'get',
        maxBodyLength: Infinity,
        url: 'http://localhost:8081/api/v1/ProofOfAchievement/'+osid,
        headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + token,
        }
    };

    axios.request(config)
        .then((response) => {
            const getResp = response.data;
            delete getResp["osUpdatedAt"]
            delete getResp["osUpdatedBy"]
            delete getResp["_osSignedData"]
            delete getResp["osOwner"]
            delete getResp["osCreatedAt"]
            delete getResp["osCreatedBy"]
            getResp["achievementTitle"] = "Participation"
            console.log(getResp)
            let config = {
                method: 'put',
                maxBodyLength: Infinity,
                url: 'http://localhost:8081/api/v1/ProofOfAchievement/' + getResp["osid"],
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': 'Bearer ' + token,
                },
                data : getResp
            };

            axios.request(config)
                .then((response) => {
                    console.log(JSON.stringify(response.data));
                })
                .catch((error) => {
                    console.log(error);
                });


        })
        .catch((error) => {
            console.log(error);
        });
})




