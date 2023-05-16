package services

import (
	"encoding/csv"
	log "github.com/sirupsen/logrus"
	"io"
	"sort"
	"strings"
)

type Scanner struct {
	Reader *csv.Reader
	Head   map[string]int
	Row    []string
}

func (s *Scanner) Scan() bool {
	a, e := s.Reader.Read()
	if e != nil {
		log.Errorf("Parsing error : %v", e)
	}
	s.Row = a
	return e == nil
}

func (s Scanner) getHeaderAsString() string {
	headers := make([]string, 0)
	for k := range s.Head {
		headers = append(headers, k)
	}
	// todo: why sorting
	sort.SliceStable(headers, func(i, j int) bool {
		return s.Head[headers[i]] < s.Head[headers[j]]
	})
	return strings.Join(headers, ",")
}

func (s *Scanner) appendHeader(field string) {
	s.Head[field] = len(s.Head)
}

func NewScanner(file io.Reader) (Scanner, error) {
	csvReader := csv.NewReader(file)
	header, e := csvReader.Read()
	if e != nil {
		log.Errorf("Parsing error : %v", e)
		return Scanner{}, e
	}
	headerOffset := map[string]int{}
	for i, name := range header {
		headerOffset[strings.TrimSpace(name)] = i
	}
	return Scanner{Reader: csvReader, Head: headerOffset}, nil
}
