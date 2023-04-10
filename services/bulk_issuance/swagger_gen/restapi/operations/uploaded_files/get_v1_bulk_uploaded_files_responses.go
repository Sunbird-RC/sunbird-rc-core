// Code generated by go-swagger; DO NOT EDIT.

package uploaded_files

// This file was generated by the swagger tool.
// Editing this file might prove futile when you re-run the swagger generate command

import (
	"net/http"

	"github.com/go-openapi/runtime"
)

// GetV1BulkUploadedFilesOKCode is the HTTP code returned for type GetV1BulkUploadedFilesOK
const GetV1BulkUploadedFilesOKCode int = 200

/*GetV1BulkUploadedFilesOK OK

swagger:response getV1BulkUploadedFilesOK
*/
type GetV1BulkUploadedFilesOK struct {

	/*
	  In: Body
	*/
	Payload interface{} `json:"body,omitempty"`
}

// NewGetV1BulkUploadedFilesOK creates GetV1BulkUploadedFilesOK with default headers values
func NewGetV1BulkUploadedFilesOK() *GetV1BulkUploadedFilesOK {

	return &GetV1BulkUploadedFilesOK{}
}

// WithPayload adds the payload to the get v1 bulk uploaded files o k response
func (o *GetV1BulkUploadedFilesOK) WithPayload(payload interface{}) *GetV1BulkUploadedFilesOK {
	o.Payload = payload
	return o
}

// SetPayload sets the payload to the get v1 bulk uploaded files o k response
func (o *GetV1BulkUploadedFilesOK) SetPayload(payload interface{}) {
	o.Payload = payload
}

// WriteResponse to the client
func (o *GetV1BulkUploadedFilesOK) WriteResponse(rw http.ResponseWriter, producer runtime.Producer) {

	rw.WriteHeader(200)
	payload := o.Payload
	if err := producer.Produce(rw, payload); err != nil {
		panic(err) // let the recovery middleware deal with this
	}
}

// GetV1BulkUploadedFilesNotFoundCode is the HTTP code returned for type GetV1BulkUploadedFilesNotFound
const GetV1BulkUploadedFilesNotFoundCode int = 404

/*GetV1BulkUploadedFilesNotFound Not found

swagger:response getV1BulkUploadedFilesNotFound
*/
type GetV1BulkUploadedFilesNotFound struct {

	/*
	  In: Body
	*/
	Payload string `json:"body,omitempty"`
}

// NewGetV1BulkUploadedFilesNotFound creates GetV1BulkUploadedFilesNotFound with default headers values
func NewGetV1BulkUploadedFilesNotFound() *GetV1BulkUploadedFilesNotFound {

	return &GetV1BulkUploadedFilesNotFound{}
}

// WithPayload adds the payload to the get v1 bulk uploaded files not found response
func (o *GetV1BulkUploadedFilesNotFound) WithPayload(payload string) *GetV1BulkUploadedFilesNotFound {
	o.Payload = payload
	return o
}

// SetPayload sets the payload to the get v1 bulk uploaded files not found response
func (o *GetV1BulkUploadedFilesNotFound) SetPayload(payload string) {
	o.Payload = payload
}

// WriteResponse to the client
func (o *GetV1BulkUploadedFilesNotFound) WriteResponse(rw http.ResponseWriter, producer runtime.Producer) {

	rw.WriteHeader(404)
	payload := o.Payload
	if err := producer.Produce(rw, payload); err != nil {
		panic(err) // let the recovery middleware deal with this
	}
}
