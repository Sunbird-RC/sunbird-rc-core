// Code generated by go-swagger; DO NOT EDIT.

package uploaded_files

// This file was generated by the swagger tool.
// Editing this file might prove futile when you re-run the swagger generate command

import (
	"net/http"

	"github.com/go-openapi/runtime"

	"bulk_issuance/swagger_gen/models"
)

// GetV1UploadsOKCode is the HTTP code returned for type GetV1UploadsOK
const GetV1UploadsOKCode int = 200

/*
GetV1UploadsOK OK

swagger:response getV1UploadsOK
*/
type GetV1UploadsOK struct {

	/*
	  In: Body
	*/
	Payload *models.UploadedFilesResponse `json:"body,omitempty"`
}

// NewGetV1UploadsOK creates GetV1UploadsOK with default headers values
func NewGetV1UploadsOK() *GetV1UploadsOK {

	return &GetV1UploadsOK{}
}

// WithPayload adds the payload to the get v1 uploads o k response
func (o *GetV1UploadsOK) WithPayload(payload *models.UploadedFilesResponse) *GetV1UploadsOK {
	o.Payload = payload
	return o
}

// SetPayload sets the payload to the get v1 uploads o k response
func (o *GetV1UploadsOK) SetPayload(payload *models.UploadedFilesResponse) {
	o.Payload = payload
}

// WriteResponse to the client
func (o *GetV1UploadsOK) WriteResponse(rw http.ResponseWriter, producer runtime.Producer) {

	rw.WriteHeader(200)
	if o.Payload != nil {
		payload := o.Payload
		if err := producer.Produce(rw, payload); err != nil {
			panic(err) // let the recovery middleware deal with this
		}
	}
}

// GetV1UploadsNotFoundCode is the HTTP code returned for type GetV1UploadsNotFound
const GetV1UploadsNotFoundCode int = 404

/*
GetV1UploadsNotFound Not found

swagger:response getV1UploadsNotFound
*/
type GetV1UploadsNotFound struct {

	/*
	  In: Body
	*/
	Payload string `json:"body,omitempty"`
}

// NewGetV1UploadsNotFound creates GetV1UploadsNotFound with default headers values
func NewGetV1UploadsNotFound() *GetV1UploadsNotFound {

	return &GetV1UploadsNotFound{}
}

// WithPayload adds the payload to the get v1 uploads not found response
func (o *GetV1UploadsNotFound) WithPayload(payload string) *GetV1UploadsNotFound {
	o.Payload = payload
	return o
}

// SetPayload sets the payload to the get v1 uploads not found response
func (o *GetV1UploadsNotFound) SetPayload(payload string) {
	o.Payload = payload
}

// WriteResponse to the client
func (o *GetV1UploadsNotFound) WriteResponse(rw http.ResponseWriter, producer runtime.Producer) {

	rw.WriteHeader(404)
	payload := o.Payload
	if err := producer.Produce(rw, payload); err != nil {
		panic(err) // let the recovery middleware deal with this
	}
}