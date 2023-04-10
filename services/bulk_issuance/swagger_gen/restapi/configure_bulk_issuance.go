// This file is safe to edit. Once it exists it will not be overwritten

package restapi

import (
	"crypto/tls"
	"net/http"

	"github.com/go-openapi/errors"
	"github.com/go-openapi/runtime"
	"github.com/go-openapi/runtime/middleware"

	"bulk_issuance/pkg"
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
	pkg.SetupHandlers(api)
	api.HasRoleAuth = pkg.RoleAuthorizer
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
	if api.SampleTemplateGetV1SampleSchemaNameHandler == nil {
		api.SampleTemplateGetV1SampleSchemaNameHandler = sample_template.GetV1SampleSchemaNameHandlerFunc(func(params sample_template.GetV1SampleSchemaNameParams, principal *models.JWTClaimBody) middleware.Responder {
			return middleware.NotImplemented("operation sample_template.GetV1BulkSampleSchemaName has not yet been implemented")
		})
	}
	if api.UploadedFilesGetV1UploadedFilesHandler == nil {
		api.UploadedFilesGetV1UploadedFilesHandler = uploaded_files.GetV1UploadedFilesHandlerFunc(func(params uploaded_files.GetV1UploadedFilesParams, principal *models.JWTClaimBody) middleware.Responder {
			return middleware.NotImplemented("operation uploaded_files.GetV1BulkUploadedFiles has not yet been implemented")
		})
	}
	if api.DownloadFileReportGetV1DownloadIDHandler == nil {
		api.DownloadFileReportGetV1DownloadIDHandler = download_file_report.GetV1DownloadIDHandlerFunc(func(params download_file_report.GetV1DownloadIDParams, principal *models.JWTClaimBody) middleware.Responder {
			return middleware.NotImplemented("operation download_file_report.GetV1DownloadIDHandlerFunc has not yet been implemented")
		})
	}
	if api.UploadAndCreateRecordsPostV1UploadFilesVCNameHandler == nil {
		api.UploadAndCreateRecordsPostV1UploadFilesVCNameHandler = upload_and_create_records.PostV1UploadFilesVCNameHandlerFunc(func(params upload_and_create_records.PostV1UploadFilesVCNameParams, principal *models.JWTClaimBody) middleware.Responder {
			return middleware.NotImplemented("operation upload_and_create_records.PostV1UploadFilesVCName has not yet been implemented")
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
// scheme value will be set accordingly: "http", "https" or "unix"
func configureServer(s *http.Server, scheme, addr string) {
}

// The middleware configuration is for the handler executors. These do not apply to the swagger.json document.
// The middleware executes after routing but before authentication, binding and validation
func setupMiddlewares(handler http.Handler) http.Handler {
	return handler
}

// The middleware configuration happens before anything, this middleware also applies to serving the swagger.json document.
// So this is a good place to plug in a panic handling middleware, logging and metrics
func setupGlobalMiddleware(handler http.Handler) http.Handler {
	return handler
}
