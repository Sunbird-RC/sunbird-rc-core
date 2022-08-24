import React, {useEffect, useState} from "react";
import {verifyCredentials} from "../../utils/verify-service";
import {Loader} from "../Loader";
import CertificateValidImg from "../../assets/img/certificate-valid.svg";
import CertificateInValidImg from "../../assets/img/certificate-invalid.svg";
import ReactJson from 'react-json-view'
import {CustomButton} from "../CustomButton";
import "./index.css";

const SHOW_FIELDS = ["id", "issuanceDate"];
export const CertificateStatus = ({certificateData, goBack}) => {
    const [isLoading, setLoading] = useState(true);
    const [isValid, setValid] = useState(false);
    const [verifiedData, setVerifiedData] = useState({});
    useEffect(() => {
        async function verify() {
            try {
                const verificationResponse = await verifyCredentials(certificateData);
                console.log(verificationResponse)
                if (verificationResponse.verified) {
                    setValid(true)
                }
                setVerifiedData(verificationResponse)
            } catch (e) {
                debugger
                setValid(false)
                setVerifiedData({"error": e.message})
            } finally {
                setLoading(false)
            }
        }

        verify()


    }, [certificateData]);

    function renderRow(index, key, value) {

        if (typeof(value) === "object") {
            return (
                <tr key={index} style={{textAlign: "left"}}>
                    <td className="pr-3">{key}</td>
                    <td className="font-weight-bolder value-col">{renderTable(value)}</td>
                </tr>
            )
        }
        return (
            <tr key={index} style={{ textAlign: "left"}}>
                <td className="pr-3">{key}</td>
                <td className="font-weight-bolder value-col">{"" + value}</td>
            </tr>
        );
    }

    function renderTable(data) {
        return <table className="mt-3">
            {
                Object.keys(data).map((key, index) => {
                    if (SHOW_FIELDS.includes(key)) {
                        const value = data[key];
                        return renderRow(index, key, value)
                    } else {
                        return null
                    }
                }).filter(i => i !== null)
            }
        </table>;
    }

    return (
        isLoading ? <Loader/> : <div className="certificate-status-wrapper">
            <img src={isValid ? CertificateValidImg : CertificateInValidImg} alt={""}
                 className="certificate-status-image"/>
            <h3 className="certificate-status">
                {
                    isValid ? "Certificate Successfully Verified" : "Certificate Invalid"
                }
            </h3>
            <br/>
            {
                isValid &&
                <>
                    <h5>Certificate</h5>
                    {renderTable(certificateData)}
                    <br/><br/>
                    <h5>Certificate Data</h5>
                    <ReactJson src={certificateData} collapsed={true}/>
                    <br/><br/>
                    <h5>Verified Data</h5>
                    <ReactJson src={verifiedData} collapsed={true}/>
                </>
            }
            {
                !isValid &&
                <>
                    <h5>Error Data</h5>
                    <ReactJson src={verifiedData} collapsed={true}/>
                </>
            }

            <br/>
            <CustomButton className="blue-btn m-3" onClick={goBack}>Verify Another Certificate</CustomButton>
        </div>
    )
}