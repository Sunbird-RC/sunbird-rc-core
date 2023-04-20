package pkg

import (
	"bulk_issuance/swagger_gen/restapi/operations"
	"bulk_issuance/swagger_gen/restapi/operations/download_file_report"
	"bulk_issuance/swagger_gen/restapi/operations/sample_template"
	"bulk_issuance/swagger_gen/restapi/operations/upload_and_create_records"
	"bulk_issuance/swagger_gen/restapi/operations/uploaded_files"
)

func SetupHandlers(api *operations.BulkIssuanceAPI) {
	api.SampleTemplateGetV1SchemaNameSampleCsvHandler = sample_template.GetV1SchemaNameSampleCsvHandlerFunc(downloadSampleFile)
	api.UploadedFilesGetV1UploadHandler = uploaded_files.GetV1UploadHandlerFunc(listFiles)
	api.DownloadFileReportGetV1IDReportHandler = download_file_report.GetV1IDReportHandlerFunc(downloadReportFile)
	api.UploadAndCreateRecordsPostV1EntityNameUploadHandler = upload_and_create_records.PostV1EntityNameUploadHandlerFunc(createRecords)
}
