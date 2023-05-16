package pkg

import (
	"bulk_issuance/config"
	"bulk_issuance/swagger_gen/models"
	"bulk_issuance/utils"
	"crypto/rsa"
	"errors"
	"fmt"
	"github.com/golang-jwt/jwt/v5"
	log "github.com/sirupsen/logrus"
)

const (
	roles = "roles"
)

var (
	verifyKey *rsa.PublicKey
)

type CustomJWTClaimBody struct {
	*jwt.RegisteredClaims
	*models.JWTClaimBody
}

func (customJwt CustomJWTClaimBody) toJWTClaim() *models.JWTClaimBody {
	return &models.JWTClaimBody{
		PreferredUsername: customJwt.PreferredUsername,
		RealmAccess:       customJwt.RealmAccess,
		ResourceAccess:    customJwt.ResourceAccess,
		Scope:             customJwt.Scope,
		TokenType:         customJwt.TokenType,
		UserID:            customJwt.UserID,
	}
}

func ParseAndLoadPublicKey() {
	verifyBytes := ([]byte)("-----BEGIN PUBLIC KEY-----\n" + config.Config.Keycloak.PublicKey + "\n-----END PUBLIC KEY-----\n")
	log.Infof("Using the public key %s", string(verifyBytes))
	var err error
	verifyKey, err = jwt.ParseRSAPublicKeyFromPEM(verifyBytes)
	utils.LogErrorIfAny("Error parsing public key from keycloak : %v", err)
}

func RoleAuthorizer(bearerToken string, swaggerRoles []string) (*models.JWTClaimBody, error) {
	claimBody, err := getClaimBody(bearerToken)
	if err != nil {
		return nil, err
	}
	expectedRoles := getExpectedRoles(swaggerRoles)
	for _, role := range expectedRoles {
		if utils.Contains(claimBody.RealmAccess.Roles, role) {
			return claimBody.toJWTClaim(), err
		}
	}
	log.Debugf("Expected roles: %v, Actual roles: %v", expectedRoles, claimBody.RealmAccess)
	return nil, errors.New("user unauthorized to perform operation")
}

func getExpectedRoles(swaggerRoles []string) []string {
	if configRoles := config.Config.GetRoles(); len(configRoles) > 0 {
		return configRoles
	}
	return swaggerRoles
}

func getClaimBody(bearerToken string) (*CustomJWTClaimBody, error) {

	if verifyKey == nil {
		Init(controllers.services)
	}

	token, err := jwt.ParseWithClaims(bearerToken, &CustomJWTClaimBody{}, func(token *jwt.Token) (interface{}, error) {
		if _, ok := token.Method.(*jwt.SigningMethodRSA); !ok {
			return nil, fmt.Errorf("error decoding token")
		}
		return verifyKey, nil
	})
	if err != nil {
		return nil, err
	}
	if token.Valid {
		claims := token.Claims.(*CustomJWTClaimBody)
		return claims, nil
	}

	return nil, errors.New("invalid token")
}
