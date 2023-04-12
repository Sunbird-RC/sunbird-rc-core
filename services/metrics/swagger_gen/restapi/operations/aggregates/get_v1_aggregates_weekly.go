// Code generated by go-swagger; DO NOT EDIT.

package aggregates

// This file was generated by the swagger tool.
// Editing this file might prove futile when you re-run the generate command

import (
	"net/http"

	"github.com/go-openapi/runtime/middleware"
)

// GetV1AggregatesWeeklyHandlerFunc turns a function with the right signature into a get v1 aggregates weekly handler
type GetV1AggregatesWeeklyHandlerFunc func(GetV1AggregatesWeeklyParams) middleware.Responder

// Handle executing the request and returning a response
func (fn GetV1AggregatesWeeklyHandlerFunc) Handle(params GetV1AggregatesWeeklyParams) middleware.Responder {
	return fn(params)
}

// GetV1AggregatesWeeklyHandler interface for that can handle valid get v1 aggregates weekly params
type GetV1AggregatesWeeklyHandler interface {
	Handle(GetV1AggregatesWeeklyParams) middleware.Responder
}

// NewGetV1AggregatesWeekly creates a new http.Handler for the get v1 aggregates weekly operation
func NewGetV1AggregatesWeekly(ctx *middleware.Context, handler GetV1AggregatesWeeklyHandler) *GetV1AggregatesWeekly {
	return &GetV1AggregatesWeekly{Context: ctx, Handler: handler}
}

/*
	GetV1AggregatesWeekly swagger:route GET /v1/aggregates/weekly aggregates getV1AggregatesWeekly

get aggregates for weekly added records
*/
type GetV1AggregatesWeekly struct {
	Context *middleware.Context
	Handler GetV1AggregatesWeeklyHandler
}

func (o *GetV1AggregatesWeekly) ServeHTTP(rw http.ResponseWriter, r *http.Request) {
	route, rCtx, _ := o.Context.RouteInfo(r)
	if rCtx != nil {
		*r = *rCtx
	}
	var Params = NewGetV1AggregatesWeeklyParams()
	if err := o.Context.BindValidRequest(r, route, &Params); err != nil { // bind params
		o.Context.Respond(rw, r, route.Produces, route, err)
		return
	}

	res := o.Handler.Handle(Params) // actually handle the request
	o.Context.Respond(rw, r, route.Produces, route, res)

}