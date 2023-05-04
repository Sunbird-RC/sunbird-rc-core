package pkg

import (
	"bulk_issuance/config"
	"bulk_issuance/services"
	"bulk_issuance/swagger_gen/models"
	"bulk_issuance/utils"
	"crypto/rsa"
	"errors"
	"fmt"
	"strings"

	"github.com/dgrijalva/jwt-go"
	log "github.com/sirupsen/logrus"
)

const (
	roles = "roles"
)

var (
	verifyKey *rsa.PublicKey
)

type Controllers struct {
	services services.IService
}

var controllers Controllers

func Init(services services.IService) {

	controllers = Controllers{
		services: services,
	}

	verifyBytes := ([]byte)("-----BEGIN PUBLIC KEY-----\n" + config.Config.Keycloak.PublicKey + "\n-----END PUBLIC KEY-----\n")
	log.Infof("Using the public key %s", string(verifyBytes))
	var err error
	verifyKey, err = jwt.ParseRSAPublicKeyFromPEM(verifyBytes)
	utils.LogErrorIfAny("Error parsing public key from keycloak : %v", err)
}

func HasResourceRole(clientId string, role string, principal *models.JWTClaimBody) bool {
	return utils.Contains(principal.ResourceAccess[clientId].Roles, role)
}

func RoleAuthorizer(bearerToken string, expectedRole []string) (*models.JWTClaimBody, error) {
	claimBody, err := getClaimBody(bearerToken)
	if err != nil {
		return nil, err
	}
	for _, role := range expectedRole {
		if utils.Contains(claimBody.RealmAccess[roles], role) {
			return claimBody, err
		}
	}
	return nil, errors.New("unauthorized")
}

func getClaimBody(bearerToken string) (*models.JWTClaimBody, error) {

	if verifyKey == nil {
		Init(controllers.services)
	}

	token, err := jwt.ParseWithClaims(bearerToken, &models.JWTClaimBody{}, func(token *jwt.Token) (interface{}, error) {
		if _, ok := token.Method.(*jwt.SigningMethodRSA); !ok {
			return nil, fmt.Errorf("error decoding token")
		}
		return verifyKey, nil
	})
	if err != nil {
		return nil, err
	}
	if token.Valid {
		claims := token.Claims.(*models.JWTClaimBody)
		return claims, nil
	}

	return nil, errors.New("invalid token")
}

func getToken(bearerHeader string) (string, error) {
	bearerTokenArr := strings.Split(bearerHeader, " ")
	if len(bearerTokenArr) <= 1 {
		return "", errors.New("invalid token")
	}
	bearerToken := bearerTokenArr[1]
	return bearerToken, nil
}
