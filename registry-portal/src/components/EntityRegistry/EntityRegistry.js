import React, {useEffect, useState} from 'react';
import {API_URL} from "../../utils/constants";
import UploadHistory from "../UploadHistory/UploadHistory";
import {useAxios} from "../../utils/useAxios";
import {formatDate} from "../../utils/dateutil";

function Entities({type}) {
    const axiosInstance = useAxios("");
    const [data, setData] = useState([]);

    useEffect(() => {
        fetchTableDetails();
    }, [type]);

    function fetchTableDetails() {
        axiosInstance.current.post(API_URL.REGISTRY_SEARCH, {
            id: "open-saber.registry.search",
            ver: 1.0,
            request: {
                entityType: [type],
                filters:{}
            }
        })
        .then((res) => {
            setData(res.data?.result[type].map((o) => {
                return {
                        ...o, 
                        "createdOn": o["osCreatedAt"] ? formatDate(o["osCreatedAt"]) : "-",
                        "updatedOn": o["osUpdatedAt"] ? formatDate(o["osUpdatedAt"]) : "-"
                    }
            }))
        }).catch((e) => {
            console.log(e);
        })
    }

    const HeaderData = [
        {
            title: "ENTITY ID",
            key: "osid"
        },
        {
            title: "CREATED ON",
            key: "createdOn"
        },
        {
            title: "UPDATED ON",
            key: "updatedOn"
        }
    ]

    return <UploadHistory
        fileUploadAPI={API_URL.FACILITY_API}
        fileUploadHistoryAPI={API_URL.FACILITY_FILE_UPLOAD_HISTORY_API}
        fileUploadErrorsAPI={API_URL.FACILITY_FILE_UPLOAD_ERRORS_API}
        infoTitle={"Records in the DIVOC Registry"}
        tableTitle="All Entities"
        emptyListMessage={"No Entity Found"}
        tableData={data}
        tableHeader={HeaderData}
        entityType={type}
        onRefresh={() => fetchTableDetails()}
    />
}

export default Entities;
