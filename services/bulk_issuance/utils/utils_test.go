package utils

import (
	"github.com/go-openapi/spec"
	"reflect"
	"testing"
	"time"
)

func Test_getSampleStringValueBasedOnFormat(t *testing.T) {
	type args struct {
		value string
	}
	tests := []struct {
		name string
		args args
		want string
	}{
		{
			name: "should return sample date",
			args: args{
				value: "date",
			},
			want: time.Now().Format(time.DateOnly),
		},
		{
			name: "should return date time",
			args: args{
				value: "date-time",
			},
			want: time.Now().Format(time.RFC3339),
		},
		{
			name: "should return email",
			args: args{
				value: "email",
			},
			want: "yyy@xx.com",
		},
		{
			name: "should return default",
			args: args{
				value: "",
			},
			want: "",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := getSampleStringValueBasedOnFormat(tt.args.value); got != tt.want {
				t.Errorf("getSampleStringValueBasedOnFormat() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_getSampleValueByType(t *testing.T) {
	tests := []struct {
		name string
		args spec.Schema
		want string
	}{
		{
			name: "Should return default value for string",
			args: spec.Schema{
				SchemaProps: spec.SchemaProps{
					Type: []string{"string"},
				},
			},
			want: "string",
		},
		{
			name: "Should return random string for string with pattern",
			args: spec.Schema{
				SchemaProps: spec.SchemaProps{
					Type:    []string{"string"},
					Pattern: "^a",
				},
			},
			want: "a",
		},
		{
			name: "Should return default value for number",
			args: spec.Schema{
				SchemaProps: spec.SchemaProps{
					Type: []string{"number"},
				},
			},
			want: "0",
		},
		{
			name: "Should return default value for integer",
			args: spec.Schema{
				SchemaProps: spec.SchemaProps{
					Type: []string{"integer"},
				},
			},
			want: "0",
		},
		{
			name: "Should return default value for object",
			args: spec.Schema{
				SchemaProps: spec.SchemaProps{
					Type: []string{"object"},
				},
			},
			want: "{}",
		},
		{
			name: "Should return default value for array",
			args: spec.Schema{
				SchemaProps: spec.SchemaProps{
					Type: []string{"array"},
				},
			},
			want: "[]",
		},
		{
			name: "Should return default value for boolean",
			args: spec.Schema{
				SchemaProps: spec.SchemaProps{
					Type: []string{"boolean"},
				},
			},
			want: "true",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := GetSampleValueByType(tt.args); got != tt.want {
				t.Errorf("getSampleValueByType() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_createCSVBuffer(t *testing.T) {
	type args struct {
		data [][]string
	}
	tests := []struct {
		name    string
		args    args
		want    string
		wantErr bool
	}{
		{
			name: "Should return csv buffer",
			args: args{
				data: [][]string{{"a", "b", "c"}},
			},
			want:    "a,b,c\n",
			wantErr: false,
		},
		{
			name: "Should return multiple row csv buffer",
			args: args{
				data: [][]string{{"a", "b", "c"}, {"1", "2", "3"}},
			},
			want:    "a,b,c\n1,2,3\n",
			wantErr: false,
		},
		{
			name: "Should return multiple row csv buffer",
			args: args{
				data: nil,
			},
			want:    "",
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := CreateCSVBuffer(tt.args.data)
			if (err != nil) != tt.wantErr {
				t.Errorf("CreateCSVBuffer() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got.String(), tt.want) {
				t.Errorf("CreateCSVBuffer() got = %v, want %v", got.String(), tt.want)
			}
		})
	}
}
