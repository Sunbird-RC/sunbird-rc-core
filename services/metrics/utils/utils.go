package utils

import "time"

func GetTimeFromMilliseconds(m int64) (time.Time, error) {
	return time.Parse("2006-01-02", time.Unix(0, m*int64(time.Millisecond)).Format("2006-01-02"))
}
