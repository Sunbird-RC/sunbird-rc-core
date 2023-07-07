const express = require('express');
const router = express.Router();
const {FUSIONAUTH_BASE_URL, FUSIONAUTH_APPLICATION_ID, FUSIONAUTH_API_KEY} = require("../config");
const axios = require("axios");

/* GET users listing. */
router.post('/api/v1/user', async function (req, res, next) {
    try {
        const {userName, email, mobile, entity, password} = req.body;
        await createRole(entity)
        try {
            const newUser = await createUser(userName, email, mobile, entity, password);
            res.json({
                userId: newUser.user.id,
                status: "SUCCESSFUL"
            })
            return;
        } catch (e) {
            if (e.response.data.fieldErrors["user.username"][0].code === "[duplicate]user.username") {
                console.log("User already exists")
                const existingUser = await getUser(userName)
                await updateUserEntity(existingUser.user.id, existingUser, entity);
                await updateUserRole(existingUser.user.id, entity)
                res.json({
                    userId: existingUser.user.id,
                    status: "SUCCESSFUL"
                })
                return;
            }
            res.status(500)
            res.json({
                userId: "",
                status: "UNSUCCESSFUL",
                message: e.message
            })
        }
    } catch (e) {
        console.log("Error while creating user", e)
    }
    res.status(500)
    res.json({
        userId: "",
        status: "UNSUCCESSFUL",
        message: "Internal server error"
    })
});

const updateUserRole = async (userId, role) => {
    const options = {
        'method': 'PATCH',
        'url': `${FUSIONAUTH_BASE_URL}/api/user/${userId}/${FUSIONAUTH_APPLICATION_ID}`,
        'headers': {
            'Authorization': FUSIONAUTH_API_KEY,
            'Content-Type': 'application/json'
        },
        data: JSON.stringify({
            "registration": {
                "applicationId": FUSIONAUTH_APPLICATION_ID,
                "roles": [
                    role
                ]
            }
        })

    };
    await axios.request(options).then((response) => {
        return response.data;
    });

}

const updateUserEntity = async (userId, existingUserDetails, entity) => {
    existingUserDetails.user.data.entity.push(entity)
    const options = {
        'method': 'PUT',
        'url': `${FUSIONAUTH_BASE_URL}/api/user/${userId}`,
        'headers': {
            'Authorization': FUSIONAUTH_API_KEY,
            'Content-Type': 'application/json'
        },
        data: JSON.stringify(existingUserDetails)

    };
    await axios.request(options).then((response) => {
        return response.data;
    });

}

const getUser = async (userName) => {
    const options = {
        'method': 'GET',
        'url': `${FUSIONAUTH_BASE_URL}/api/user?username=${userName}`,
        'headers': {
            'Authorization': FUSIONAUTH_API_KEY
        }
    };
    return await axios.request(options).then((response) => {
        return response.data;
    });
}

const createUser = async (userName, email, mobile, role, password="") => {
    const options = {
        'method': 'POST',
        'url': `${FUSIONAUTH_BASE_URL}/api/user/registration`,
        'headers': {
            'Authorization': FUSIONAUTH_API_KEY,
            'Content-Type': 'application/json'
        },
        data: JSON.stringify({
            "user": {
                "username": userName,
                "email": email,
                "password": password,
                "data": {
                    "entity": [
                        role
                    ]
                },
                "mobilePhone": mobile
            },
            "registration": {
                "applicationId": FUSIONAUTH_APPLICATION_ID,
                "roles": [
                    role
                ]
            }
        })

    };
    return await axios.request(options).then((response) => {
        return response.data;
    }).catch((e) => {
        console.log(e.response.data.fieldErrors);
        throw e;
    });
}

const createRole = async (role) => {
    try {
        let data = JSON.stringify({
            "role": {
                "description": "",
                "name": role,
                "isDefault": false
            }
        });

        let config = {
            method: 'post',
            maxBodyLength: Infinity,
            url: `${FUSIONAUTH_BASE_URL}/api/application/${FUSIONAUTH_APPLICATION_ID}/role`,
            headers: {
                'Authorization': FUSIONAUTH_API_KEY,
                'Content-Type': 'application/json'
            },
            data: data
        };

        await axios.request(config)
            .then((response) => {
                return response.data;
            })
            .catch((error) => {
                console.log(error.response.data.fieldErrors);
            });
    } catch (e) {
        console.log(e)
    }
}
module.exports = router;
