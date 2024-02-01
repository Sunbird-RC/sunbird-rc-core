// @/toolbox/registry/helpers/registry
// Wrapper around Registry API

import { http } from 'gluegun';
import { ApisauceInstance } from 'apisauce';
import { ApiResponse } from '../../../types';
import { config } from '../../../config/config';


class RegistryWrapper {
    httpClient: ApisauceInstance


    constructor() {
		this.httpClient = http.create({
			baseURL: 'http://localhost:8081',
		})
	}

    async createAUserInRegistry (userName : string , email: string) : Promise<string> {
        // Create a user(admin/Issuer) via registry
        let data = this.getAdminUserReq(userName , email);
        console.log(data);
        let maxRetries = config.maximumRetries
		let retryCount = 0;
        while (retryCount < maxRetries) {
        try {
            const response = (await this.httpClient.post(
                `/api/v1/Issuer`,
                data,
                {
                headers: { 
                    'Content-Type': 'application/json', 
                    'Accept': 'application/json'
                    },
                }
            )) as ApiResponse


            if (response.ok) {
                // get the keycloak osOwnerID and use it assign admin role
                return "success";
                // return response.data.access_token
            } else {
                // if (retryCount === maxRetries - 1)
                //     console.debug(response.originalError)
                throw new Error(
                    `There was an error while creating an User in registry: ${
                        response.originalError ?? response.problem
                    }`
                )
            }
        } catch (error) {
            retryCount++;
            // You can adjust the delay time as needed
            await new Promise((resolve) => setTimeout(resolve, 1000)) // 1 second delay
        }
        }

        throw new Error(
			`API call failed to create new user after ${maxRetries} retries.`
		);


    }

    async createDocumentTypeInRegistry () : Promise<string> {
        // Create a user(admin/Issuer) via registry
        let data = this.getDocumentTypeReq();
        console.log(data);
        let maxRetries = config.maximumRetries
		let retryCount = 0;
        while (retryCount < maxRetries) {
        try {
            const response = (await this.httpClient.post(
                `/api/v1/DocumentType`,
                data,
                {
                headers: { 
                    'Content-Type': 'application/json', 
                    'Accept': 'application/json'
                    },
                }
            )) as ApiResponse


            if (response.ok) {
                // get the keycloak osOwnerID and use it assign admin role
                return "success";
                // return response.data.access_token
            } else {
                // if (retryCount === maxRetries - 1)
                //     console.debug(response.originalError)
                console.log(retryCount);
                throw new Error(
                    `There was an error while creating an User in registry: ${
                        response.originalError ?? response.problem
                    }`
                )
            }
        } catch (error) {
            retryCount++;
            // You can adjust the delay time as needed
            await new Promise((resolve) => setTimeout(resolve, 1000)) // 1 second delay
        }
        }

        throw new Error(
			`API call failed to create new DocumentType after ${maxRetries} retries.`
		);


    }

    getAdminUserReq (userName : string , email : string) {
        return {
            "name": userName,
            "sectorType": "Education",
            "logoUrl": "https://avatars.githubusercontent.com/u/81144329?s=200&v=4",
            "websiteUrl": "https://docs.sunbirdrc.dev/learn/readme",
            "accountDetails": {
              "userId": email
            },
            "contactDetails": {
              "name": userName,
              "mobile": email,
              "email": email
            },
            "schemas": []
        };
    }
    
    getDocumentTypeReq () {
        return {
            "name": "Training Certificate",
            "samples": [
              {
                "schemaUrl": "https://raw.githubusercontent.com/Sunbird-RC/demo-certificate-issuance/main/schemas/SkillCertificate.json",
                "certificateUrl": "https://raw.githubusercontent.com/Sunbird-RC/demo-certificate-issuance/main/schemas/templates/SkillCertificate.html",
                "thumbnailUrl": "https://avatars.githubusercontent.com/u/81144329?s=200&v=4"
              }
            ]
        }
    }


}

export default RegistryWrapper;