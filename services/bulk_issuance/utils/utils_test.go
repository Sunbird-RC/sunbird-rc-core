package utils

import (
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
	type args struct {
		value map[string]interface{}
	}
	tests := []struct {
		name string
		args args
		want string
	}{
		{
			name: "Should return default value for string",
			args: args{value: map[string]interface{}{
				"type": "string",
			}},
			want: "string",
		},
		{
			name: "Should return random string for string with pattern",
			args: args{value: map[string]interface{}{
				"type":    "string",
				"pattern": "^a",
			}},
			want: "a",
		},
		{
			name: "Should return default value for number",
			args: args{value: map[string]interface{}{
				"type": "number",
			}},
			want: "0",
		},
		{
			name: "Should return default value for integer",
			args: args{value: map[string]interface{}{
				"type": "integer",
			}},
			want: "0",
		},
		{
			name: "Should return default value for object",
			args: args{value: map[string]interface{}{
				"type": "object",
			}},
			want: "{}",
		},
		{
			name: "Should return default value for array",
			args: args{value: map[string]interface{}{
				"type": "array",
			}},
			want: "[]",
		},
		{
			name: "Should return default value for boolean",
			args: args{value: map[string]interface{}{
				"type": "boolean",
			}},
			want: "true",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := getSampleValueByType(tt.args.value); got != tt.want {
				t.Errorf("getSampleValueByType() = %v, want %v", got, tt.want)
			}
		})
	}
}
