package services

import (
	"io"
	"strings"
	"testing"

	"github.com/stretchr/testify/assert"
)

func Test_Scanner(t *testing.T) {
	file := io.NopCloser(strings.NewReader("col1,col2\nrow11,row12\nrow21,row22"))
	expectedRows := [][]string{{"row11", "row12"}, {"row21", "row22"}}
	expectedColumns := "col1,col2"
	scanner, _ := NewScanner(file)
	actualColumns := scanner.getHeaderAsString()
	actualRows := make([][]string, 0)
	for scanner.Scan() {
		row := scanner.Row
		actualRows = append(actualRows, row)
	}
	assert := assert.New(t)
	assert.Equal(expectedRows, actualRows)
	assert.Equal(expectedColumns, actualColumns)
}

func Test_AppendHeader(t *testing.T) {
	file := io.NopCloser(strings.NewReader("col1,col2\nrow11,row12\nrow21,row22"))
	scanner, _ := NewScanner(file)
	scanner.appendHeader("Errors")
	expectedColumns := "col1,col2,Errors"
	actualColumns := scanner.getHeaderAsString()
	assert := assert.New(t)
	assert.Equal(expectedColumns, actualColumns)
}
