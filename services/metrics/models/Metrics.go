package models

type Metrics struct {
	Eid   string `json:"eid"`
	Ets   int64  `json:"ets"`
	Ver   string `json:"ver"`
	Mid   string `json:"mid"`
	Actor struct {
		Id   string `json:"id"`
		Type string `json:"type"`
	} `json:"actor"`
	Context struct {
		Channel string `json:"channel"`
		Env     string `json:"env"`
	} `json:"context"`
	Object struct {
		Id   string `json:"id"`
		Type string `json:"type"`
	} `json:"object"`
	Edata map[string]interface{} `json:"edata"`
}
