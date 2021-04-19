import React, {useEffect, useState} from 'react';
import {TabPanels} from "../TabPanel/TabPanel";
import Entities from "../EntityRegistry/EntityRegistry";
import {Button, Col} from "react-bootstrap";
import {API_URL, SampleCSV} from "../../utils/constants";
import DownloadImg from "../../assets/img/download.svg";
import "./Add.module.css"
import {useAxios} from "../../utils/useAxios";
import AppBar from "@material-ui/core/AppBar";
import Tabs from "@material-ui/core/Tabs";
import Tab from "@material-ui/core/Tab";
import Form from "@rjsf/core";
import {useHistory} from "react-router-dom";

const schema = {
    title: "Todo",
    type: "object",
    required: ["title"],
    properties: {
        title: {type: "string", title: "Title", default: "A new task"},
        done: {type: "boolean", title: "Done?", default: false}
    }
};

const log = (type) => console.log.bind(console, type);

export default function Add(props) {
    const axiosInstance = useAxios("");
    const [schema, setSchema] = useState({});
    const entityType = props.match.params.entity;
    const history = useHistory();

    useEffect(x=>{
        axiosInstance.current.get(API_URL.SCHEMA_BASE + entityType + ".json")
            .then((res) => {
                setSchema(res.data);
            })
            .catch((e) => {
                console.log(e);
            })
    }, []);

    const onSubmit = function (form) {
        const data = form.formData[entityType];
        axiosInstance.current.post(API_URL.REST_BASE + entityType, data)
            .then(res => {
                if (res.status === 200) {
                    alert('Saved!');
                    history.push('/portal/admin/')
                }
                else
                    alert("Something went wrong while saving!");
            });
        alert('submit' + form.formData[entityType]);
    };

    return <div className={"container"}>
        <Form schema={schema}
              onChange={log("changed")}
              onSubmit={onSubmit}
              onError={log("errors")} />
    </div>
}
