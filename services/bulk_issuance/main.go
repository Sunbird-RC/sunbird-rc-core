package main

import (
	"bulk_issuance/config"
	"bulk_issuance/db"
	"bulk_issuance/pkg"
	"bulk_issuance/services"
	"bulk_issuance/swagger_gen/restapi"
	"bulk_issuance/swagger_gen/restapi/operations"
	"os"
	"strings"

	log "github.com/sirupsen/logrus"

	"github.com/go-openapi/loads"
	"github.com/jessevdk/go-flags"
)

func main() {
	config.Initialize("./application-default.yml")
	pkg.ParseAndLoadPublicKey()
	if level, err := log.ParseLevel(strings.ToLower(config.Config.LogLevel)); err == nil {
		log.SetLevel(level)
	} else {
		log.Error("Error parsing log level", err)
	}
	repo := db.Init()
	servicesObj := services.Init(repo)
	pkg.Init(servicesObj)
	swaggerSpec, err := loads.Embedded(restapi.SwaggerJSON, restapi.FlatSwaggerJSON)
	if err != nil {
		log.Fatalln(err)
	}
	api := operations.NewBulkIssuanceAPI(swaggerSpec)
	server := restapi.NewServer(api)
	defer func(server *restapi.Server) {
		err := server.Shutdown()
		if err != nil {
			log.Error("Error while shutdown", err)
		}
	}(server)
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
	pkg.SetupHandlers(api)
	api.HasRoleAuth = pkg.RoleAuthorizer

	server.ConfigureAPI()
	if err := server.Serve(); err != nil {
		log.Fatalln(err)
	}

}
