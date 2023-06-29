package pkg

import (
	"bulk_issuance/swagger_gen/models"
	"fmt"
	"github.com/stretchr/testify/assert"
	"testing"
)

func TestRoleAuthorizer(t *testing.T) {
	type args struct {
		bearerToken  string
		swaggerRoles []string
	}
	tests := []struct {
		name    string
		args    args
		want    *models.JWTClaimBody
		wantErr assert.ErrorAssertionFunc
	}{
		// TODO: Add test cases.
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := RoleAuthorizer(tt.args.bearerToken, tt.args.swaggerRoles)
			if !tt.wantErr(t, err, fmt.Sprintf("RoleAuthorizer(%v, %v)", tt.args.bearerToken, tt.args.swaggerRoles)) {
				return
			}
			assert.Equalf(t, tt.want, got, "RoleAuthorizer(%v, %v)", tt.args.bearerToken, tt.args.swaggerRoles)
		})
	}
}
