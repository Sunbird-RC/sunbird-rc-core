// This file is safe to edit. Once it exists it will not be overwritten

package restapi

import (
	"crypto/tls"
	"net/http"

	"github.com/go-openapi/errors"
	"github.com/go-openapi/runtime"
	"github.com/go-openapi/runtime/middleware"

	"bulk_issuance/swagger_gen/models"
	"bulk_issuance/swagger_gen/restapi/operations"
	"bulk_issuance/swagger_gen/restapi/operations/download_file_report"
	"bulk_issuance/swagger_gen/restapi/operations/sample_template"
	"bulk_issuance/swagger_gen/restapi/operations/upload_and_create_records"
	"bulk_issuance/swagger_gen/restapi/operations/uploaded_files"
)

//go:generate swagger generate server --target ../../swagger_gen --name BulkIssuance --spec ../../interfaces/bulk_issuance_api.yaml --principal models.JWTClaimBody --exclude-main

func configureFlags(api *operations.BulkIssuanceAPI) {
	// api.CommandLineOptionsGroups = []swag.CommandLineOptionsGroup{ ... }
}

func configureAPI(api *operations.BulkIssuanceAPI) http.Handler {
	// configure the api here
	api.ServeError = errors.ServeError

	// Set your custom logger if needed. Default one is log.Printf
	// Expected interface func(string, ...interface{})
	//
	// Example:
	// api.Logger = log.Printf

	api.UseSwaggerUI()
	// To continue using redoc as your UI, uncomment the following line
	// api.UseRedoc()

	api.JSONConsumer = runtime.JSONConsumer()
	api.MultipartformConsumer = runtime.DiscardConsumer

	api.BinProducer = runtime.ByteStreamProducer()
	api.JSONProducer = runtime.JSONProducer()

	if api.HasRoleAuth == nil {
		api.HasRoleAuth = func(token string, scopes []string) (*models.JWTClaimBody, error) {
			return nil, errors.NotImplemented("oauth2 bearer auth (hasRole) has not yet been implemented")
		}
	}

	// Set your custom authorizer if needed. Default one is security.Authorized()
	// Expected interface runtime.Authorizer
	//
	// Example:
	// api.APIAuthorizer = security.Authorized()
	// You may change here the memory limit for this multipart form parser. Below is the default (32 MB).
	// upload_and_create_records.PostV1SchemaNameUploadMaxParseMemory = 32 << 20

	if api.DownloadFileReportGetV1IDReportHandler == nil {
		api.DownloadFileReportGetV1IDReportHandler = download_file_report.GetV1IDReportHandlerFunc(func(params download_file_report.GetV1IDReportParams, principal *models.JWTClaimBody) middleware.Responder {
			return middleware.NotImplemented("operation download_file_report.GetV1IDReport has not yet been implemented")
		})
	}
	if api.SampleTemplateGetV1SchemaNameSampleCsvHandler == nil {
		api.SampleTemplateGetV1SchemaNameSampleCsvHandler = sample_template.GetV1SchemaNameSampleCsvHandlerFunc(func(params sample_template.GetV1SchemaNameSampleCsvParams, principal *models.JWTClaimBody) middleware.Responder {
			return middleware.NotImplemented("operation sample_template.GetV1SchemaNameSampleCsv has not yet been implemented")
		})
	}
	if api.UploadedFilesGetV1UploadsHandler == nil {
		api.UploadedFilesGetV1UploadsHandler = uploaded_files.GetV1UploadsHandlerFunc(func(params uploaded_files.GetV1UploadsParams, principal *models.JWTClaimBody) middleware.Responder {
			return middleware.NotImplemented("operation uploaded_files.GetV1Uploads has not yet been implemented")
		})
	}
	if api.UploadAndCreateRecordsPostV1SchemaNameUploadHandler == nil {
		api.UploadAndCreateRecordsPostV1SchemaNameUploadHandler = upload_and_create_records.PostV1SchemaNameUploadHandlerFunc(func(params upload_and_create_records.PostV1SchemaNameUploadParams, principal *models.JWTClaimBody) middleware.Responder {
			return middleware.NotImplemented("operation upload_and_create_records.PostV1SchemaNameUpload has not yet been implemented")
		})
	}

	api.PreServerShutdown = func() {}

	api.ServerShutdown = func() {}

	return setupGlobalMiddleware(api.Serve(setupMiddlewares))
}

// The TLS configuration before HTTPS server starts.
func configureTLS(tlsConfig *tls.Config) {
	// Make all necessary changes to the TLS configuration here.
}

// As soon as server is initialized but not run yet, this function will be called.
// If you need to modify a config, store server instance to stop it individually later, this is the place.
// This function can be called multiple times, depending on the number of serving schemes.
// scheme value will be set accordingly: "http", "https" or "unix".
func configureServer(s *http.Server, scheme, addr string) {
}

// The middleware configuration is for the handler executors. These do not apply to the swagger.json document.
// The middleware executes after routing but before authentication, binding and validation.
func setupMiddlewares(handler http.Handler) http.Handler {
	return handler
}

// The middleware configuration happens before anything, this middleware also applies to serving the swagger.json document.
// So this is a good place to plug in a panic handling middleware, logging and metrics.
func setupGlobalMiddleware(handler http.Handler) http.Handler {
	return handler
}
