import React, {useEffect, useState} from 'react';
import {TabPanels} from "../TabPanel/TabPanel";
import Entities from "../EntityRegistry/EntityRegistry";
import {Button, Col} from "react-bootstrap";
import {API_URL, SampleCSV} from "../../utils/constants";
import DownloadImg from "../../assets/img/download.svg";
import "./Admin.module.css"
import {useAxios} from "../../utils/useAxios";
import AppBar from "@material-ui/core/AppBar";
import Tabs from "@material-ui/core/Tabs";
import Tab from "@material-ui/core/Tab";


export default function Admin(props) {

    const axiosInstance = useAxios("");
    const [entities, setEntities] = useState([]);
    const entityType = props.match.params.entity;

    useEffect(() => {
        axiosInstance.current.get(API_URL.REGISTRY_LIST)
        .then((res) => {
            const entities = res.data.result.filter(x=> x[0] != x[0].toLowerCase())
            setEntities(entities);
        })
        .catch((e) => {
            console.log(e);
        })
    }, []);

    function renderDownloadTemplateButton(templateLink) {
        return <Button bsPrefix={"btn-template"} href={templateLink}>
            <Col className="d-flex flex-row">
                <h6>DOWNLOAD TEMPLATE</h6>
                <img src={DownloadImg} alt={"Download CSV"}/>
            </Col>
        </Button>;
    }

    function fetchTabs() {
        return entities.map(ent => {
            return {
                title: ent,
                component: <Entities type={ent}/>
                // , rightTabContent: renderDownloadTemplateButton(SampleCSV.FACILITY_REGISTRY)
            }
        });
    }
    
    return entities.length > 0 && (
        <TabPanels tabs={fetchTabs()} />
    );
}
