package services

import (
	"testing"
)

func TestDigiLockerService_generateHMAC(t *testing.T) {
	type args struct {
		rawData []byte
	}
	tests := []struct {
		name    string
		args    args
		want    int
		wantErr bool
	}{
		{
			name: "should return hmac",
			args: args{
				rawData: []byte("TEXT"),
			},
			want:    64,
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			service := DigiLockerService{}
			got, err := service.generateHMAC(tt.args.rawData)
			if (err != nil) != tt.wantErr {
				t.Errorf("generateHMAC() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if len(got) != tt.want {
				t.Errorf("generateHMAC() got = %v, want %v", len(got), tt.want)
			}
		})
	}
}
