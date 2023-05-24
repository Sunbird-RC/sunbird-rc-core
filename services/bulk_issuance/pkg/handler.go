package pkg

import (
	"bulk_issuance/services"
	"bulk_issuance/swagger_gen/restapi/operations"
	"bulk_issuance/swagger_gen/restapi/operations/download_file_report"
	"bulk_issuance/swagger_gen/restapi/operations/sample_template"
	"bulk_issuance/swagger_gen/restapi/operations/upload_and_create_records"
	"bulk_issuance/swagger_gen/restapi/operations/uploaded_files"
)

type Controllers struct {
	services services.IService
}

var controllers Controllers

func Init(services services.IService) {
	controllers = Controllers{
		services: services,
	}
}

func SetupHandlers(api *operations.BulkIssuanceAPI) {
	api.SampleTemplateGetV1SchemaNameSampleCsvHandler = sample_template.GetV1SchemaNameSampleCsvHandlerFunc(controllers.getSampleCSVForSchema)
	api.UploadedFilesGetV1UploadsHandler = uploaded_files.GetV1UploadsHandlerFunc(controllers.getUserUploadedFiles)
	api.DownloadFileReportGetV1IDReportHandler = download_file_report.GetV1IDReportHandlerFunc(controllers.getCSVReportById)
	api.UploadAndCreateRecordsPostV1SchemaNameUploadHandler = upload_and_create_records.PostV1SchemaNameUploadHandlerFunc(controllers.createRecordsForSchema)
}
