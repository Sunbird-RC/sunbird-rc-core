var Mustache = require('mustache');
test('Should render json template', async () => {
    const entity = {
      identityDetails: {
          name: "Tejash"
      }
    };
    const template = {
        "@context": ["https://www.w3.org/2018/credentials/v1"],
        "type": ["VerifiableCredential"],
        "credentialSubject": "Person",
        "issuanceDate": "2021-08-27T10:57:57.237Z",
        "data": {
            "name": "{{identityDetails.name}}"
        },
        "issuer": "https://sunbird.org/",
        // "date": "28-09-2021",
    }
    const output = JSON.parse(Mustache.render(JSON.stringify(template), entity));
    expect(output.data.name).toBe("Tejash");
});

test('Should render json template with custom delimiters', async () => {
    const entity = {
      identityDetails: {
          name: "Tejash"
      }
    };
    const template = {
        "@context": ["https://www.w3.org/2018/credentials/v1"],
        "type": ["VerifiableCredential"],
        "credentialSubject": "Person",
        "issuanceDate": "2021-08-27T10:57:57.237Z",
        "data": {
            "name": "${identityDetails.name}"
        },
        "issuer": "did:issuer:sunbird",
        // "date": "28-09-2021",
    };
    var customTags = [ '${', '}' ];
    Mustache.tags = customTags;
    const output = JSON.parse(Mustache.render(JSON.stringify(template), entity));
    expect(output.data.name).toBe("Tejash");
});