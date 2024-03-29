// Code generated by go-swagger; DO NOT EDIT.

package restapi

// This file was generated by the swagger tool.
// Editing this file might prove futile when you re-run the swagger generate command

import (
	"encoding/json"
)

var (
	// SwaggerJSON embedded version of the swagger document used at generation time
	SwaggerJSON json.RawMessage
	// FlatSwaggerJSON embedded flattened version of the swagger document used at generation time
	FlatSwaggerJSON json.RawMessage
)

func init() {
	SwaggerJSON = json.RawMessage([]byte(`{
  "consumes": [
    "application/json"
  ],
  "produces": [
    "application/json"
  ],
  "swagger": "2.0",
  "info": {
    "description": "Metrics API",
    "title": "Metrics",
    "version": "1.0.0"
  },
  "paths": {
    "/health": {
      "get": {
        "description": "API to get the notification health status",
        "tags": [
          "health"
        ],
        "summary": "Get the health status",
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "type": "object"
            }
          }
        }
      }
    },
    "/v1/aggregates": {
      "get": {
        "security": [],
        "consumes": [
          "application/json"
        ],
        "produces": [
          "application/json"
        ],
        "tags": [
          "aggregates"
        ],
        "summary": "get aggregates for weekly added records",
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/AggregatesResponse"
            }
          }
        }
      }
    },
    "/v1/metrics": {
      "get": {
        "security": [],
        "consumes": [
          "application/json"
        ],
        "produces": [
          "application/json"
        ],
        "tags": [
          "metrics"
        ],
        "summary": "get all metrics",
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/LoginResponse"
            }
          }
        }
      }
    }
  },
  "definitions": {
    "AggregatesResponse": {
      "type": "object"
    },
    "LoginResponse": {
      "type": "object"
    }
  },
  "security": [
    {
      "hasRole": []
    }
  ]
}`))
	FlatSwaggerJSON = json.RawMessage([]byte(`{
  "consumes": [
    "application/json"
  ],
  "produces": [
    "application/json"
  ],
  "swagger": "2.0",
  "info": {
    "description": "Metrics API",
    "title": "Metrics",
    "version": "1.0.0"
  },
  "paths": {
    "/health": {
      "get": {
        "description": "API to get the notification health status",
        "tags": [
          "health"
        ],
        "summary": "Get the health status",
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "type": "object"
            }
          }
        }
      }
    },
    "/v1/aggregates": {
      "get": {
        "security": [],
        "consumes": [
          "application/json"
        ],
        "produces": [
          "application/json"
        ],
        "tags": [
          "aggregates"
        ],
        "summary": "get aggregates for weekly added records",
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/AggregatesResponse"
            }
          }
        }
      }
    },
    "/v1/metrics": {
      "get": {
        "security": [],
        "consumes": [
          "application/json"
        ],
        "produces": [
          "application/json"
        ],
        "tags": [
          "metrics"
        ],
        "summary": "get all metrics",
        "responses": {
          "200": {
            "description": "OK",
            "schema": {
              "$ref": "#/definitions/LoginResponse"
            }
          }
        }
      }
    }
  },
  "definitions": {
    "AggregatesResponse": {
      "type": "object"
    },
    "LoginResponse": {
      "type": "object"
    }
  },
  "security": [
    {
      "hasRole": []
    }
  ]
}`))
}
