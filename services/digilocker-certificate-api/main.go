package main

import (
	"digilocker_certificate_api/swagger_gen/restapi"
	"digilocker_certificate_api/swagger_gen/restapi/operations"
	"log"
	"os"

	"github.com/go-openapi/loads"
	"github.com/jessevdk/go-flags"
)

func main() {
	swaggerSpec, err := loads.Embedded(restapi.SwaggerJSON, restapi.FlatSwaggerJSON)
	if err != nil {
		log.Fatalln(err)
	}
	api := operations.NewDigilockerCertificateAPIAPI(swaggerSpec)
	server := restapi.NewServer(api)
	defer server.Shutdown()
	server.ConfigureFlags()
	parser := flags.NewParser(server, flags.Default)
	for _, optsGroup := range api.CommandLineOptionsGroups {
		_, err := parser.AddGroup(optsGroup.ShortDescription, optsGroup.LongDescription, optsGroup.Options)
		if err != nil {
			log.Fatalln(err)
		}
	}
	if _, err := parser.Parse(); err != nil {
		code := 1
		if fe, ok := err.(*flags.Error); ok {
			if fe.Type == flags.ErrHelp {
				code = 0
			}
		}
		os.Exit(code)
	}
	server.ConfigureAPI()
	if err := server.Serve(); err != nil {
		log.Fatalln(err)
	}
}
