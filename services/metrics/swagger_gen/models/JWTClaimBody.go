package models

import (
	jwt "github.com/golang-jwt/jwt/v4"
)

type JWTClaimBody struct {
	*jwt.RegisteredClaims
	TokenType         string
	ResourceAccess    map[string]Group `json:"resource_access"`
	Scope             string           `json:"scope"`
	PreferredUsername string           `json:"preferred_username"`
	FacilityCode      string           `json:"facility_code"`
}

type Group struct {
	Roles []string `json:"roles"`
}
